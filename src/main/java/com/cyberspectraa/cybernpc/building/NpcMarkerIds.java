package com.cyberspectraa.cybernpc.building;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class NpcMarkerIds {
    public static UUID id(
            ResourceKey<Level> dimension,
            BlockPos pos,
            NpcMarkerKind kind
    ) {
        String value = dimension.location()
                + "|" + pos.asLong()
                + "|" + kind.name();

        return UUID.nameUUIDFromBytes(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private NpcMarkerIds() {
    }
}
