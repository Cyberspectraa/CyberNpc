package com.cyberspectraa.cybernpc.intro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import javax.annotation.Nullable;

public final class IntroSettings extends SavedData {
    private static final String KEY = "cybernpc_intro";
    @Nullable private Point arrival;
    @Nullable private Point popeWait;

    public record Point(ResourceKey<Level> dimension, BlockPos pos, float yaw) {
        public Point { pos = pos.immutable(); }
        public boolean matches(ServerLevel level) { return level.dimension().equals(dimension); }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimension.location().toString());
            tag.putLong("pos", pos.asLong());
            tag.putFloat("yaw", yaw);
            return tag;
        }
        @Nullable public static Point read(CompoundTag tag) {
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("dimension"));
            if (location == null || !tag.contains("pos")) return null;
            return new Point(ResourceKey.create(Registries.DIMENSION, location),
                    BlockPos.of(tag.getLong("pos")), tag.getFloat("yaw"));
        }
    }

    public static IntroSettings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                IntroSettings::load, IntroSettings::new, KEY);
    }
    private static IntroSettings load(CompoundTag tag) {
        IntroSettings data = new IntroSettings();
        if (tag.contains("arrival")) data.arrival = Point.read(tag.getCompound("arrival"));
        if (tag.contains("pope_wait")) data.popeWait = Point.read(tag.getCompound("pope_wait"));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        if (arrival != null) tag.put("arrival", arrival.save());
        if (popeWait != null) tag.put("pope_wait", popeWait.save());
        return tag;
    }
    @Nullable public Point arrival() { return arrival; }
    @Nullable public Point popeWait() { return popeWait; }
    public boolean ready() { return arrival != null && popeWait != null; }
    public void setArrival(ServerLevel level, BlockPos pos, float yaw) {
        arrival = new Point(level.dimension(), pos, yaw); setDirty();
    }
    public void setPopeWait(ServerLevel level, BlockPos pos, float yaw) {
        popeWait = new Point(level.dimension(), pos, yaw); setDirty();
    }
}
