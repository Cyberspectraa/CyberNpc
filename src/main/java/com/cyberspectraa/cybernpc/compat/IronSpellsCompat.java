package com.cyberspectraa.cybernpc.compat;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.WildNpcGearTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class IronSpellsCompat {
    public static final String MOD_ID = "irons_spellbooks";

    private static final String SPELL_CONTAINER = "ISB_Spells";
    private static final String SPELL_DATA = "data";
    private static final Map<UUID, ActiveCast> ACTIVE_CASTS = new HashMap<>();

    // Some Iron's spells are valid player spells but are not safe for a foreign
    // LivingEntity caster. Spectral Hammer later fires Forge's BreakEvent with
    // a null Player when cast by CyberNpc, which crashes on the hammer entity tick.
    private static final Set<String> MOB_UNSAFE_SPELLS = Set.of(
            MOD_ID + ":spectral_hammer"
    );

    private static boolean attemptedInit;
    private static boolean ready;
    private static Method getSpell;
    private static Method isEnabled;
    private static Method getCastType;
    private static Method getSpellCooldown;
    private static Method getEffectiveCastTime;
    private static Method checkPreCastConditions;
    private static Method onServerPreCast;
    private static Method onServerCastTick;
    private static Method onCast;
    private static Method onServerCastComplete;
    private static Method magicInitiateCast;
    private static Method magicSetSyncedData;
    private static Constructor<?> magicDataConstructor;
    private static Constructor<?> syncedSpellDataConstructor;
    private static Object mobCastSource;

    private IronSpellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack createItem(String itemPath) {
        if (!isLoaded() || itemPath == null || itemPath.isBlank()) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(MOD_ID, itemPath));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static ItemStack createMageSpellBook(
            String preferredBookPath,
            List<String> schoolSpellPaths,
            WildNpcGearTier gearTier,
            RandomSource random
    ) {
        if (!isLoaded() || schoolSpellPaths == null || schoolSpellPaths.isEmpty()) {
            return ItemStack.EMPTY;
        }

        String fallbackBook = switch (gearTier) {
            case ELITE -> "netherite_spell_book";
            case RARE -> "diamond_spell_book";
            case FINE -> "gold_spell_book";
            default -> "iron_spell_book";
        };

        ItemStack book = createItem(preferredBookPath);
        if (book.isEmpty()) {
            book = createItem(fallbackBook);
        }
        if (book.isEmpty()) {
            book = createItem("copper_spell_book");
        }
        if (book.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<String> available = new ArrayList<>();
        for (String path : schoolSpellPaths) {
            String id = path.contains(":") ? path : MOD_ID + ":" + path;
            if (isSupportedCombatSpell(id)) {
                available.add(id);
            }
        }

        if (available.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = available.size() - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            String temp = available.get(i);
            available.set(i, available.get(swap));
            available.set(swap, temp);
        }

        int spellCount = Math.min(
                available.size(),
                switch (gearTier) {
                    case STANDARD -> 2;
                    case FINE -> 3;
                    case RARE -> 3;
                    case ELITE -> 4;
                }
        );

        int maxSlots = switch (gearTier) {
            case STANDARD -> 6;
            case FINE -> 8;
            case RARE -> 10;
            case ELITE -> 12;
        };

        int baseLevel = switch (gearTier) {
            case STANDARD -> 1;
            case FINE -> 2;
            case RARE -> 3;
            case ELITE -> 4;
        };

        CompoundTag root = new CompoundTag();
        root.putInt("maxSpells", maxSlots);
        root.putBoolean("mustEquip", true);
        root.putBoolean("spellWheel", true);

        ListTag data = new ListTag();
        for (int i = 0; i < spellCount; i++) {
            CompoundTag slot = new CompoundTag();
            slot.putString("id", available.get(i));
            slot.putInt(
                    "level",
                    Math.max(1, baseLevel + (random.nextFloat() < 0.20F ? 1 : 0))
            );
            slot.putBoolean("locked", false);
            slot.putInt("index", i);
            data.add(slot);
        }

        root.put(SPELL_DATA, data);
        book.addTagElement(SPELL_CONTAINER, root);
        book.getOrCreateTag().putBoolean("CyberNpcMageSpellbook", true);
        return book;
    }

    /**
     * Converts the NPC's populated combat spellbook into the normal empty
     * Iron's spellbook that a player should receive as loot.
     *
     * The NPC-only marker and school marker are removed as well, so the
     * dropped item behaves exactly like an ordinary empty Iron's book.
     */
    public static ItemStack createEmptyLootSpellBook(ItemStack npcSpellBook) {
        if (npcSpellBook == null || npcSpellBook.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack droppedBook = npcSpellBook.copy();
        CompoundTag tag = droppedBook.getTag();

        if (tag != null) {
            tag.remove(SPELL_CONTAINER);
            tag.remove("CyberNpcMageSpellbook");
            tag.remove("CyberNpcMageSchool");
        }

        return droppedBook;
    }

    public static List<SpellEntry> getBookSpells(ItemStack spellBook) {
        List<SpellEntry> result = new ArrayList<>();
        if (spellBook == null || spellBook.isEmpty()) {
            return result;
        }

        CompoundTag root = spellBook.getTagElement(SPELL_CONTAINER);
        if (root == null) {
            return result;
        }

        ListTag data = root.getList(SPELL_DATA, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < data.size(); i++) {
            CompoundTag slot = data.getCompound(i);
            String id = slot.getString("id");
            int level = Math.max(1, slot.getInt("level"));

            if (!id.isBlank()) {
                result.add(new SpellEntry(id, level));
            }
        }

        return result;
    }

    public static boolean hasActiveCast(CyberNpcEntity caster) {
        return ACTIVE_CASTS.containsKey(caster.getUUID());
    }

    public static CastResult tickAttackSpell(
            CyberNpcEntity caster,
            LivingEntity target,
            ItemStack spellBook
    ) {
        if (!isLoaded()
                || caster.level().isClientSide
                || !initialize()
                || spellBook == null
                || spellBook.isEmpty()) {
            return CastResult.failed();
        }

        ActiveCast active = ACTIVE_CASTS.get(caster.getUUID());
        if (active != null) {
            if (!caster.isAlive() || target == null || !target.isAlive()) {
                cancelCast(caster);
                return CastResult.failed();
            }

            faceTarget(caster, target);

            try {
                onServerCastTick.invoke(
                        active.spell,
                        caster.level(),
                        active.level,
                        caster,
                        active.magicData
                );

                active.remainingTicks--;

                if (active.remainingTicks <= 0) {
                    onCast.invoke(
                            active.spell,
                            caster.level(),
                            active.level,
                            caster,
                            mobCastSource,
                            active.magicData
                    );

                    onServerCastComplete.invoke(
                            active.spell,
                            caster.level(),
                            active.level,
                            caster,
                            active.magicData,
                            false
                    );

                    ACTIVE_CASTS.remove(caster.getUUID());
                    return new CastResult(
                            true,
                            false,
                            active.cooldownTicks,
                            12,
                            active.spellId,
                            "LONG"
                    );
                }

                return new CastResult(
                        true,
                        true,
                        0,
                        active.remainingTicks + 2,
                        active.spellId,
                        "LONG"
                );
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                cancelCast(caster);
                return CastResult.failed();
            }
        }

        List<SpellEntry> spells = getBookSpells(spellBook);
        if (spells.isEmpty()) {
            return CastResult.failed();
        }

        faceTarget(caster, target);

        int start = caster.getRandom().nextInt(spells.size());
        for (int offset = 0; offset < spells.size(); offset++) {
            SpellEntry entry = spells.get((start + offset) % spells.size());

            // Saved 0.14.0 Mage books may already contain a spell that was later
            // classified as unsafe, so guard again at cast time as well as book creation.
            if (isExplicitlyUnsafeMobSpell(entry.spellId())) {
                continue;
            }

            try {
                Object spell = getSpell.invoke(null, entry.spellId());
                if (!isSupportedSpellObject(spell)) {
                    continue;
                }

                Object castType = getCastType.invoke(spell);
                String castTypeName = String.valueOf(castType);

                Object magicData = createMobMagicData(caster);

                boolean canCast = (Boolean) checkPreCastConditions.invoke(
                        spell,
                        caster.level(),
                        entry.level(),
                        caster,
                        magicData
                );
                if (!canCast) {
                    continue;
                }

                int effectiveCastTime = ((Number) getEffectiveCastTime.invoke(
                        spell,
                        entry.level(),
                        caster
                )).intValue();

                magicInitiateCast.invoke(
                        magicData,
                        spell,
                        entry.level(),
                        Math.max(0, effectiveCastTime),
                        mobCastSource,
                        "mainhand"
                );

                onServerPreCast.invoke(
                        spell,
                        caster.level(),
                        entry.level(),
                        caster,
                        magicData
                );

                int cooldown = Mth.clamp(
                        ((Number) getSpellCooldown.invoke(spell)).intValue(),
                        16,
                        120
                );

                if ("INSTANT".equals(castTypeName) || effectiveCastTime <= 0) {
                    onCast.invoke(
                            spell,
                            caster.level(),
                            entry.level(),
                            caster,
                            mobCastSource,
                            magicData
                    );
                    onServerCastComplete.invoke(
                            spell,
                            caster.level(),
                            entry.level(),
                            caster,
                            magicData,
                            false
                    );

                    return new CastResult(
                            true,
                            false,
                            cooldown,
                            14,
                            entry.spellId(),
                            "INSTANT"
                    );
                }

                ACTIVE_CASTS.put(
                        caster.getUUID(),
                        new ActiveCast(
                                spell,
                                magicData,
                                entry.level(),
                                Math.max(1, effectiveCastTime),
                                cooldown,
                                entry.spellId()
                        )
                );

                return new CastResult(
                        true,
                        true,
                        0,
                        Math.max(2, effectiveCastTime + 2),
                        entry.spellId(),
                        "LONG"
                );
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try another spell from the NPC's actual spellbook.
            }
        }

        return CastResult.failed();
    }

    public static void cancelCast(CyberNpcEntity caster) {
        ActiveCast active = ACTIVE_CASTS.remove(caster.getUUID());
        if (active == null || !ready) {
            return;
        }

        try {
            onServerCastComplete.invoke(
                    active.spell,
                    caster.level(),
                    active.level,
                    caster,
                    active.magicData,
                    true
            );
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static boolean isSupportedCombatSpell(String spellId) {
        if (!isLoaded()
                || spellId == null
                || spellId.isBlank()
                || isExplicitlyUnsafeMobSpell(spellId)
                || !initialize()) {
            return false;
        }

        try {
            Object spell = getSpell.invoke(null, spellId);
            return isSupportedSpellObject(spell);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isExplicitlyUnsafeMobSpell(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return true;
        }

        String normalized = spellId.contains(":")
                ? spellId.trim()
                : MOD_ID + ":" + spellId.trim();
        return MOB_UNSAFE_SPELLS.contains(normalized);
    }

    private static boolean isSupportedSpellObject(Object spell)
            throws ReflectiveOperationException {
        if (spell == null) {
            return false;
        }

        if (isEnabled != null && !((Boolean) isEnabled.invoke(spell))) {
            return false;
        }

        Object castType = getCastType.invoke(spell);
        if (castType == null) {
            return false;
        }

        String name = String.valueOf(castType);
        return "INSTANT".equals(name) || "LONG".equals(name);
    }

    private static Object createMobMagicData(CyberNpcEntity caster)
            throws ReflectiveOperationException {
        Object magicData = magicDataConstructor.newInstance(true);
        Object syncedData = syncedSpellDataConstructor.newInstance(caster);
        magicSetSyncedData.invoke(magicData, syncedData);
        return magicData;
    }

    private static synchronized boolean initialize() {
        if (attemptedInit) {
            return ready;
        }
        attemptedInit = true;

        try {
            Class<?> spellRegistryClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.registry.SpellRegistry"
            );
            Class<?> abstractSpellClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.AbstractSpell"
            );
            Class<?> magicDataClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.magic.MagicData"
            );
            Class<?> castSourceClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.CastSource"
            );
            Class<?> syncedSpellDataClass = Class.forName(
                    "io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData"
            );

            getSpell = spellRegistryClass.getMethod("getSpell", String.class);
            isEnabled = abstractSpellClass.getMethod("isEnabled");
            getCastType = abstractSpellClass.getMethod("getCastType");
            getSpellCooldown = abstractSpellClass.getMethod("getSpellCooldown");
            getEffectiveCastTime = abstractSpellClass.getMethod(
                    "getEffectiveCastTime",
                    int.class,
                    LivingEntity.class
            );
            checkPreCastConditions = abstractSpellClass.getMethod(
                    "checkPreCastConditions",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass
            );
            onServerPreCast = abstractSpellClass.getMethod(
                    "onServerPreCast",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass
            );
            onServerCastTick = abstractSpellClass.getMethod(
                    "onServerCastTick",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass
            );
            onCast = abstractSpellClass.getMethod(
                    "onCast",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    castSourceClass,
                    magicDataClass
            );
            onServerCastComplete = abstractSpellClass.getMethod(
                    "onServerCastComplete",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass,
                    boolean.class
            );
            magicInitiateCast = magicDataClass.getMethod(
                    "initiateCast",
                    abstractSpellClass,
                    int.class,
                    int.class,
                    castSourceClass,
                    String.class
            );
            magicSetSyncedData = magicDataClass.getMethod(
                    "setSyncedData",
                    syncedSpellDataClass
            );

            magicDataConstructor = magicDataClass.getConstructor(boolean.class);
            syncedSpellDataConstructor =
                    syncedSpellDataClass.getConstructor(LivingEntity.class);

            @SuppressWarnings({"rawtypes", "unchecked"})
            Object source = Enum.valueOf(
                    (Class<? extends Enum>) castSourceClass,
                    "MOB"
            );
            mobCastSource = source;
            ready = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            ready = false;
        }

        return ready;
    }

    private static void faceTarget(CyberNpcEntity caster, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(caster.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float yaw = (float) (
                Math.atan2(delta.z, delta.x) * (180.0D / Math.PI)
        ) - 90.0F;
        float pitch = (float) -(
                Math.atan2(delta.y, horizontal) * (180.0D / Math.PI)
        );

        caster.setYRot(yaw);
        caster.setYHeadRot(yaw);
        caster.setXRot(pitch);
        caster.getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    private static final class ActiveCast {
        private final Object spell;
        private final Object magicData;
        private final int level;
        private int remainingTicks;
        private final int cooldownTicks;
        private final String spellId;

        private ActiveCast(
                Object spell,
                Object magicData,
                int level,
                int remainingTicks,
                int cooldownTicks,
                String spellId
        ) {
            this.spell = spell;
            this.magicData = magicData;
            this.level = level;
            this.remainingTicks = remainingTicks;
            this.cooldownTicks = cooldownTicks;
            this.spellId = spellId;
        }
    }

    public record SpellEntry(String spellId, int level) {
    }

    public record CastResult(
            boolean success,
            boolean casting,
            int cooldownTicks,
            int visualTicks,
            String spellId,
            String castType
    ) {
        static CastResult failed() {
            return new CastResult(false, false, 20, 0, "", "");
        }
    }
}
