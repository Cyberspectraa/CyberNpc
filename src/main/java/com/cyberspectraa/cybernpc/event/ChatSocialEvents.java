package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcChatIntent;
import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ChatSocialEvents {
    private static final double HEARING_RADIUS = 24.0D;
    private static final double DIRECT_ADDRESS_RADIUS = 8.0D;
    private static final double DIRECT_ADDRESS_DOT = 0.62D;

    private static final String[] POSITIVE_WORDS = {
            "love", "like", "cool", "nice", "great", "awesome",
            "good", "friend", "thanks", "thank"
    };

    private static final String[] NEGATIVE_WORDS = {
            "hate", "dislike", "idiot", "stupid", "annoying",
            "ugly", "loser", "sucks", "shut up"
    };

    private static final String[] THREAT_WORDS = {
            "kill", "hurt", "attack", "fight", "beat", "destroy",
            "murder"
    };

    private static final String[] NEGATIONS = {
            "dont", "do not", "wont", "will not", "wouldnt",
            "would not", "never"
    };

    private ChatSocialEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null
                || player.isSpectator()
                || player.level().isClientSide()) {
            return;
        }

        String raw = event.getMessage().getString();
        String message = normalize(raw);

        if (message.isBlank()) {
            return;
        }

        NpcChatIntent intent = classify(message);
        if (intent == null) {
            return;
        }

        AABB search = player.getBoundingBox().inflate(
                HEARING_RADIUS,
                10.0D,
                HEARING_RADIUS
        );

        List<CyberNpcEntity> nearby = player.level().getEntitiesOfClass(
                CyberNpcEntity.class,
                search,
                npc -> npc.isAlive()
                        && npc.getNpcType() == NpcType.WILD
                        && !npc.isZombifying()
        );

        if (nearby.isEmpty()) {
            return;
        }

        List<CyberNpcEntity> namedTargets = new ArrayList<>();
        for (CyberNpcEntity npc : nearby) {
            String name = npc.getCustomName() == null
                    ? ""
                    : normalize(npc.getCustomName().getString());

            if (!name.isBlank() && containsPhrase(message, name)) {
                namedTargets.add(npc);
            }
        }

        if (namedTargets.isEmpty()) {
            if (!containsPhrase(message, "you")) {
                return;
            }

            CyberNpcEntity addressed = findAddressedNpc(
                    player,
                    nearby
            );

            if (addressed == null) {
                return;
            }

            namedTargets.add(addressed);
        }

        for (CyberNpcEntity target : namedTargets) {
            target.reactToPlayerChat(player, intent, target);

            for (CyberNpcEntity witness : nearby) {
                if (witness != target
                        && witness.isFriendWith(target)) {
                    witness.reactToPlayerChat(
                            player,
                            intent,
                            target
                    );
                }
            }
        }
    }

    private static CyberNpcEntity findAddressedNpc(
            ServerPlayer player,
            List<CyberNpcEntity> nearby
    ) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        CyberNpcEntity best = null;
        double bestScore = -Double.MAX_VALUE;

        for (CyberNpcEntity npc : nearby) {
            double distanceSqr = player.distanceToSqr(npc);
            if (distanceSqr
                    > DIRECT_ADDRESS_RADIUS * DIRECT_ADDRESS_RADIUS
                    || !player.hasLineOfSight(npc)) {
                continue;
            }

            Vec3 direction = npc.getBoundingBox()
                    .getCenter()
                    .subtract(eye);

            if (direction.lengthSqr() < 0.0001D) {
                return npc;
            }

            double dot = look.dot(direction.normalize());
            if (dot < DIRECT_ADDRESS_DOT) {
                continue;
            }

            // Looking directly at an NPC matters more than tiny distance
            // differences, but nearby targets still win close ties.
            double score = dot * 4.0D
                    - Math.sqrt(distanceSqr) * 0.04D;

            if (score > bestScore) {
                bestScore = score;
                best = npc;
            }
        }

        return best;
    }

    private static NpcChatIntent classify(String message) {
        boolean threat = containsAny(message, THREAT_WORDS)
                && !isNegated(message, THREAT_WORDS);

        if (threat) {
            return NpcChatIntent.THREAT;
        }

        boolean negative = containsAny(message, NEGATIVE_WORDS)
                && !isNegated(message, NEGATIVE_WORDS);

        if (negative) {
            return NpcChatIntent.NEGATIVE;
        }

        if (containsAny(message, POSITIVE_WORDS)) {
            return NpcChatIntent.POSITIVE;
        }

        return null;
    }

    private static boolean isNegated(
            String message,
            String[] words
    ) {
        for (String negation : NEGATIONS) {
            for (String word : words) {
                if (containsPhrase(
                        message,
                        negation + " " + word
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean containsAny(
            String message,
            String[] words
    ) {
        for (String word : words) {
            if (containsPhrase(message, word)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsPhrase(
            String message,
            String phrase
    ) {
        String paddedMessage = " " + message + " ";
        String paddedPhrase = " " + phrase.trim() + " ";
        return paddedMessage.contains(paddedPhrase);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
