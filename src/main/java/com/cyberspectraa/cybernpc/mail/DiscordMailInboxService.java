package com.cyberspectraa.cybernpc.mail;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

/**
 * Optional, same-host Discord-to-Minecraft bridge. The external Discord bot
 * writes a small, authenticated-user form result as an atomic JSON spool file.
 * No Discord token, network listener or third-party library lives inside the
 * mod or Minecraft client.
 *
 * On the server thread, imported letters enter the exact same MailSavedData
 * PENDING queue and registered Drop Box used by the in-game letter item.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class DiscordMailInboxService {
    private static final int MAX_FILE_BYTES = 4_096;
    private static final int MAX_ITEMS_PER_POLL = 6;
    private static final long POLL_INTERVAL = 40L; // two seconds at 20 TPS
    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger("CyberNpc/DiscordMail");
    private static final Queue<Path> ARCHIVE_RECOVERY = new ArrayDeque<>();
    private static boolean scannedArchive;

    private DiscordMailInboxService() {}

    private record LetterRequest(UUID requestId, String discordUserId,
                                 String sender, String recipient, String message) {}
    private record Receipt(boolean accepted, long mailId, String description) {}

    private static Path base() {
        return FMLPaths.CONFIGDIR.get().resolve("cybernpc-discord-mail");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        // The bridge must never read local files in a modpack client's
        // integrated test session. It belongs to the dedicated server only.
        if (server == null || !server.isDedicatedServer()) return;
        ServerLevel overworld = server.overworld();
        if (overworld == null || overworld.getGameTime() % POLL_INTERVAL != 0) return;

        try {
            Path root = base();
            Path incoming = root.resolve("incoming");
            Path archive = root.resolve("archive");
            Path failed = root.resolve("rejected");
            Path results = root.resolve("results");
            Files.createDirectories(incoming);
            Files.createDirectories(archive);
            Files.createDirectories(failed);
            Files.createDirectories(results);

            if (!scannedArchive) {
                scannedArchive = true;
                // Recover letters accepted just before a server crash but not
                // yet world-saved. MailSavedData's UUID dedup makes this safe.
                ARCHIVE_RECOVERY.addAll(collect(archive, 20_000));
            }

            int remaining = MAX_ITEMS_PER_POLL;
            for (Path file : collect(incoming, remaining)) {
                importFile(server, file, archive, failed, results, false);
                remaining--;
            }
            while (remaining-- > 0 && !ARCHIVE_RECOVERY.isEmpty()) {
                Path file = ARCHIVE_RECOVERY.poll();
                if (file != null && Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                        && !importFile(server, file, archive, failed, results, true)) {
                    // If a crash lost a world save and no Drop Box is loaded
                    // yet, keep the archived letter for another recovery pass.
                    ARCHIVE_RECOVERY.add(file);
                }
            }
        } catch (IOException ex) {
            LOG.warning("Could not poll Discord mail inbox: " + ex.getMessage());
        }
    }

    private static List<Path> collect(Path folder, int max) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    files.add(file);
                }
                if (files.size() >= max) break;
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    /** @return true if imported/handled, false if this file needs another try. */
    private static boolean importFile(MinecraftServer server, Path file, Path archive,
                                   Path failed, Path results, boolean replay) {
        String fileName = file.getFileName().toString();
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.json")) {
            // An unrelated file must not stall the queue or be parsed.
            return true;
        }
        UUID fileId;
        try {
            fileId = UUID.fromString(fileName.substring(0, 36));
        } catch (IllegalArgumentException ex) {
            return true;
        }
        final LetterRequest request;
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                reject(file, failed, results, fileId, "Letter exceeded the file size limit.", replay);
                return true;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            request = parse(json);
            if (!request.requestId().equals(fileId)) {
                reject(file, failed, results, fileId, "Mail request ID mismatch.", replay);
                return true;
            }
        } catch (RuntimeException | IOException ex) {
            try {
                reject(file, failed, results, fileId, "Invalid mail request.", replay);
            } catch (IOException trouble) {
                LOG.warning("Discord mail could not be rejected: " + trouble.getMessage());
            }
            LOG.warning("Invalid Discord mail " + fileId + ": " + ex.getMessage());
            return true;
        }

        // A registered letter may already be saved in the world at this point.
        // If writing its receipt or archiving the file fails, keep the request
        // in place and retry. Never mark an accepted letter as rejected.
        try {
            Receipt receipt = register(server, request);
            if (receipt == null) return false; // No active Drop Box; retry later.
            writeReceipt(results, fileId, receipt);
            if (!replay) {
                move(file, (receipt.accepted() ? archive : failed).resolve(fileName));
            }
            return true;
        } catch (RuntimeException | IOException ex) {
            LOG.warning("Discord mail " + fileId
                    + " will retry after processing problem: " + ex.getMessage());
            return false;
        }
    }

    private static LetterRequest parse(String raw) {
        JsonObject data = JsonParser.parseString(raw).getAsJsonObject();
        UUID requestId = UUID.fromString(data.get("requestId").getAsString());
        String discordId = data.get("discordUserId").getAsString();
        String sender = clean(data.get("senderName").getAsString(), 32);
        String recipient = data.get("recipientName").getAsString().trim();
        String message = clean(data.get("message").getAsString(), 256);
        if (!discordId.matches("[0-9]{15,22}")
                || sender.isBlank() || message.isBlank()
                || sender.length() > 32 || message.length() > 256
                || !recipient.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Invalid letter fields");
        }
        return new LetterRequest(requestId, discordId, sender, recipient, message);
    }

    private static String clean(String value, int max) {
        if (value.length() > max) throw new IllegalArgumentException("Field too long");
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 167 || Character.isISOControl(c)) {
                throw new IllegalArgumentException("Formatting/control character in letter");
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    /**
     * Return null only for a temporary lack of a registered, loaded drop box.
     * Other failures produce an explicit private status for the Discord bot.
     */
    @Nullable
    private static Receipt register(MinecraftServer server, LetterRequest request) {
        MailSavedData mail = MailSavedData.get(server.overworld());
        Long already = mail.discordMailId(request.requestId());
        if (already != null) {
            return new Receipt(true, already, "Your letter is in the Minecraft postal system.");
        }
        Optional<GameProfile> recipient = server.getProfileCache().get(request.recipient());
        if (recipient.isEmpty() || recipient.get().getId() == null) {
            return new Receipt(false, -1L, "No known Minecraft player has that name.");
        }
        GameProfile to = recipient.get();
        // A valid Mojang username is not proof the person belongs to this
        // server. Only mail real participants of this Minecraft world.
        Path savedPlayer = server.getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .resolve(to.getId() + ".dat");
        if (server.getPlayerList().getPlayer(to.getId()) == null
                && !Files.isRegularFile(savedPlayer, LinkOption.NOFOLLOW_LINKS)) {
            return new Receipt(false, -1L,
                    "That Minecraft player has not joined this server yet.");
        }
        MailSavedData.PostalAddress dropBox = mail.availableDiscordDropBox(server);
        if (dropBox == null) return null;

        UUID senderId = UUID.nameUUIDFromBytes(
                ("cybernpc:discord:" + request.discordUserId()).getBytes(StandardCharsets.UTF_8));
        MailItemData.Draft draft = new MailItemData.Draft(
                senderId, request.sender(), to.getId(), to.getName(), request.message());
        long mailId = mail.postDiscordOnce(request.requestId(), draft, dropBox);
        return mailId >= 0L
                ? new Receipt(true, mailId, "Posted to a Minecraft Drop Box for courier collection.")
                : new Receipt(false, -1L, "The postal service could not accept the letter.");
    }

    private static void reject(Path file, Path failed, Path results,
                               UUID id, String reason, boolean replay) throws IOException {
        writeReceipt(results, id, new Receipt(false, -1L, reason));
        if (!replay) move(file, failed.resolve(file.getFileName()));
    }

    private static void writeReceipt(Path results, UUID id, Receipt receipt) throws IOException {
        JsonObject status = new JsonObject();
        status.addProperty("requestId", id.toString());
        status.addProperty("status", receipt.accepted() ? "accepted" : "rejected");
        status.addProperty("mailId", receipt.mailId());
        status.addProperty("message", receipt.description());
        Path temp = results.resolve(id + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(temp, status.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            move(temp, results.resolve(id + ".json"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        ARCHIVE_RECOVERY.clear();
        scannedArchive = false;
    }
}
