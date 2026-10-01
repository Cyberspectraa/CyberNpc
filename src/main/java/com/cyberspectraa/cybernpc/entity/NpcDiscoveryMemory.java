package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent lightweight knowledge of places an NPC has personally discovered
 * or learned about from an explorer.
 */
final class NpcDiscoveryMemory {
    private static final int MAX_DISCOVERIES = 48;
    private static final String TAG_DISCOVERIES = "CyberNpcDiscoveries";

    private final LinkedHashMap<String, Discovery> discoveries =
            new LinkedHashMap<>();

    boolean rememberBiome(String biomeId, BlockPos pos) {
        if (biomeId == null || biomeId.isBlank() || pos == null) {
            return false;
        }

        return remember(new Discovery(
                "biome",
                "biome:" + biomeId,
                humanizeBiome(biomeId),
                pos.immutable()
        ));
    }

    boolean rememberVillage(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        // Treat nearby village POI areas as the same landmark. This avoids
        // recording every street/house as a separate discovery.
        int regionX = Math.floorDiv(pos.getX(), 96);
        int regionZ = Math.floorDiv(pos.getZ(), 96);

        return remember(new Discovery(
                "village",
                "village:" + regionX + ":" + regionZ,
                "Village",
                pos.immutable()
        ));
    }

    boolean remember(Discovery discovery) {
        if (discovery == null
                || discovery.key == null
                || discovery.key.isBlank()
                || discoveries.containsKey(discovery.key)) {
            return false;
        }

        if (discoveries.size() >= MAX_DISCOVERIES) {
            String oldest = discoveries.keySet().iterator().next();
            discoveries.remove(oldest);
        }

        discoveries.put(discovery.key, discovery);
        return true;
    }

    int shareTo(NpcDiscoveryMemory other) {
        if (other == null || other == this) {
            return 0;
        }

        int learned = 0;
        for (Discovery discovery : discoveries.values()) {
            if (other.remember(discovery)) {
                learned++;
            }
        }
        return learned;
    }

    int size() {
        return discoveries.size();
    }

    int countType(String type) {
        int count = 0;
        for (Discovery discovery : discoveries.values()) {
            if (discovery.type.equals(type)) {
                count++;
            }
        }
        return count;
    }

    String debugSummary() {
        int biomes = countType("biome");
        int villages = countType("village");

        if (discoveries.isEmpty()) {
            return "none";
        }

        return biomes + " biome"
                + (biomes == 1 ? "" : "s")
                + " / "
                + villages + " village"
                + (villages == 1 ? "" : "s");
    }

    String latestLabel() {
        Discovery latest = null;
        for (Discovery discovery : discoveries.values()) {
            latest = discovery;
        }
        return latest == null ? "none" : latest.label;
    }

    void saveTo(CompoundTag root) {
        ListTag list = new ListTag();

        for (Discovery discovery : discoveries.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", discovery.type);
            tag.putString("Key", discovery.key);
            tag.putString("Label", discovery.label);
            tag.putLong("Pos", discovery.pos.asLong());
            list.add(tag);
        }

        root.put(TAG_DISCOVERIES, list);
    }

    void loadFrom(CompoundTag root) {
        discoveries.clear();

        if (!root.contains(TAG_DISCOVERIES, Tag.TAG_LIST)) {
            return;
        }

        ListTag list = root.getList(TAG_DISCOVERIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && discoveries.size() < MAX_DISCOVERIES; i++) {
            CompoundTag tag = list.getCompound(i);
            String type = tag.getString("Type");
            String key = tag.getString("Key");
            String label = tag.getString("Label");

            if (type.isBlank() || key.isBlank() || label.isBlank()) {
                continue;
            }

            discoveries.put(
                    key,
                    new Discovery(
                            type,
                            key,
                            label,
                            BlockPos.of(tag.getLong("Pos"))
                    )
            );
        }
    }

    List<Discovery> snapshot() {
        return new ArrayList<>(discoveries.values());
    }

    private static String humanizeBiome(String id) {
        String path = id;
        int colon = path.indexOf(':');
        if (colon >= 0 && colon + 1 < path.length()) {
            path = path.substring(colon + 1);
        }

        String[] parts = path.split("_");
        StringBuilder out = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (out.length() > 0) {
                out.append(' ');
            }

            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }

        return out.length() == 0 ? id : out.toString();
    }

    record Discovery(
            String type,
            String key,
            String label,
            BlockPos pos
    ) {
    }
}
