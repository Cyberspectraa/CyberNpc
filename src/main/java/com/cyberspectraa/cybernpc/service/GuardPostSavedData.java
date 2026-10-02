package com.cyberspectraa.cybernpc.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GuardPostSavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_guard_posts";

    private final Set<GuardPost> posts = new LinkedHashSet<>();
    private final Map<GuardPost, Claim> claims = new HashMap<>();

    public static GuardPostSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static GuardPostSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                GuardPostSavedData::load,
                GuardPostSavedData::new,
                DATA_NAME
        );
    }

    public static GuardPostSavedData load(CompoundTag tag) {
        GuardPostSavedData data = new GuardPostSavedData();
        ListTag list = tag.getList("Posts", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            GuardPost post = GuardPost.load(list.getCompound(i));
            if (post != null) {
                data.posts.add(post);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (GuardPost post : posts) {
            list.add(post.save());
        }

        tag.put("Posts", list);
        return tag;
    }

    public void register(
            ResourceKey<Level> dimension,
            BlockPos pos,
            Direction facing
    ) {
        unregister(dimension, pos);
        posts.add(new GuardPost(
                dimension,
                pos.immutable(),
                facing
        ));
        setDirty();
    }

    public void unregister(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        GuardPost existing = find(dimension, pos);
        if (existing == null) {
            return;
        }

        posts.remove(existing);
        claims.remove(existing);
        setDirty();
    }

    public boolean exists(GuardPost post) {
        return post != null && posts.contains(post);
    }

    @Nullable
    public GuardPost claimNearest(
            ResourceKey<Level> dimension,
            BlockPos center,
            UUID guardId,
            double radiusSqr,
            long gameTime,
            long claimUntil
    ) {
        pruneClaims(gameTime);

        GuardPost current = claimedBy(guardId);
        if (current != null && posts.contains(current)) {
            claims.put(current, new Claim(guardId, claimUntil));
            return current;
        }

        GuardPost best = null;
        double bestDistance = Double.MAX_VALUE;

        for (GuardPost post : posts) {
            if (!post.dimension.equals(dimension)) {
                continue;
            }

            double distance = center.distSqr(post.pos);
            if (distance > radiusSqr || distance >= bestDistance) {
                continue;
            }

            Claim claim = claims.get(post);
            if (claim != null && !claim.guardId.equals(guardId)) {
                continue;
            }

            best = post;
            bestDistance = distance;
        }

        if (best != null) {
            claims.put(best, new Claim(guardId, claimUntil));
        }

        return best;
    }

    public void releaseGuard(UUID guardId) {
        if (guardId == null) {
            return;
        }

        claims.entrySet().removeIf(
                entry -> entry.getValue().guardId.equals(guardId)
        );
    }

    @Nullable
    private GuardPost claimedBy(UUID guardId) {
        for (Map.Entry<GuardPost, Claim> entry : claims.entrySet()) {
            if (entry.getValue().guardId.equals(guardId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    private GuardPost find(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        for (GuardPost post : posts) {
            if (post.dimension.equals(dimension)
                    && post.pos.equals(pos)) {
                return post;
            }
        }
        return null;
    }

    private void pruneClaims(long now) {
        claims.entrySet().removeIf(
                entry -> entry.getValue().expiresAt <= now
                        || !posts.contains(entry.getKey())
        );
    }

    public record GuardPost(
            ResourceKey<Level> dimension,
            BlockPos pos,
            Direction facing
    ) {
        public GuardPost {
            pos = pos.immutable();
            facing = facing == null ? Direction.NORTH : facing;
        }

        public float yaw() {
            return facing.toYRot();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Pos", pos.asLong());
            tag.putString("Facing", facing.getName());
            return tag;
        }

        @Nullable
        private static GuardPost load(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.tryParse(
                    tag.getString("Dimension")
            );
            if (id == null || !tag.contains("Pos")) {
                return null;
            }

            Direction facing = Direction.byName(
                    tag.getString("Facing")
            );

            return new GuardPost(
                    ResourceKey.create(Registries.DIMENSION, id),
                    BlockPos.of(tag.getLong("Pos")),
                    facing == null ? Direction.NORTH : facing
            );
        }
    }

    private record Claim(UUID guardId, long expiresAt) {
    }
}
