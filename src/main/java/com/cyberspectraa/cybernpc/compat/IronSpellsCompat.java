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
import net.minecraft.world.entity.Mob;
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
    private static final Map<UUID, Map<String, Long>> SPELL_COOLDOWNS = new HashMap<>();

    // Some Iron's spells are valid player spells but are not safe for a foreign
    // LivingEntity caster. Spectral Hammer later fires Forge's BreakEvent with
    // a null Player when cast by CyberNpc, which crashes on the hammer entity tick.
    private static final Set<String> MOB_UNSAFE_SPELLS = Set.of(
            MOD_ID + ":spectral_hammer"
    );

    /*
     * Iron's spells do not expose one universal range/usage API. CyberNpc only
     * rolls a controlled pool of Mage spells, so profile those spells here.
     * Distances are tactical AI distances, not replacements for Iron's own
     * spell checks. Unknown future spells fall back to a conservative mid-range
     * profile instead of being treated as point-blank or infinite-range.
     */
    private static final Map<String, SpellTactics> SPELL_TACTICS = Map.ofEntries(
            // Fire
            Map.entry(MOD_ID + ":firebolt", SpellTactics.mid()),
            Map.entry(MOD_ID + ":fireball", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":flaming_strike", new SpellTactics(0.0D, 2.0D, 3.4D, SpellRole.CLOSE, true)),
            Map.entry(MOD_ID + ":magma_bomb", SpellTactics.longRange()),

            // Ice
            Map.entry(MOD_ID + ":icicle", new SpellTactics(2.0D, 11.0D, 28.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":frostwave", new SpellTactics(0.0D, 5.0D, 7.5D, SpellRole.CLOSE, false)),
            Map.entry(MOD_ID + ":snowball", SpellTactics.mid()),
            Map.entry(MOD_ID + ":ray_of_frost", new SpellTactics(4.0D, 15.0D, 30.0D, SpellRole.RANGED, true)),

            // Lightning
            Map.entry(MOD_ID + ":lightning_bolt", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":chain_lightning", new SpellTactics(4.0D, 14.0D, 30.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":electrocute", new SpellTactics(0.0D, 6.0D, 10.0D, SpellRole.CLOSE, true)),
            Map.entry(MOD_ID + ":shockwave", new SpellTactics(0.0D, 7.0D, 10.0D, SpellRole.CLOSE, false)),

            // Nature
            Map.entry(MOD_ID + ":acid_orb", new SpellTactics(4.0D, 12.0D, 28.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":poison_arrow", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":root", new SpellTactics(4.0D, 14.0D, 30.0D, SpellRole.CONTROL, true)),
            Map.entry(MOD_ID + ":poison_splash", new SpellTactics(5.0D, 18.0D, 32.0D, SpellRole.CONTROL, true)),

            // Holy
            Map.entry(MOD_ID + ":guiding_bolt", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":divine_smite", new SpellTactics(0.0D, 1.7D, 2.6D, SpellRole.CLOSE, true)),
            Map.entry(MOD_ID + ":heal", new SpellTactics(0.0D, 0.0D, 64.0D, SpellRole.HEAL, false)),
            Map.entry(MOD_ID + ":wisp", new SpellTactics(5.0D, 20.0D, 48.0D, SpellRole.RANGED, true)),

            // Ender
            Map.entry(MOD_ID + ":magic_missile", SpellTactics.mid()),
            Map.entry(MOD_ID + ":magic_arrow", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":dragon_breath", new SpellTactics(0.0D, 6.0D, 10.0D, SpellRole.CLOSE, true)),
            Map.entry(MOD_ID + ":evasion", new SpellTactics(0.0D, 0.0D, 64.0D, SpellRole.DEFENSE, false)),

            // Blood
            Map.entry(MOD_ID + ":blood_needles", new SpellTactics(3.0D, 16.0D, 32.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":blood_slash", new SpellTactics(2.0D, 8.0D, 18.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":wither_skull", SpellTactics.longRange()),
            Map.entry(MOD_ID + ":ray_of_siphoning", new SpellTactics(3.0D, 12.0D, 24.0D, SpellRole.RANGED, true)),

            // Evocation
            Map.entry(MOD_ID + ":fang_strike", new SpellTactics(2.0D, 7.0D, 14.0D, SpellRole.CLOSE, true)),
            Map.entry(MOD_ID + ":fang_swirl", new SpellTactics(4.0D, 18.0D, 32.0D, SpellRole.RANGED, true)),
            Map.entry(MOD_ID + ":slow", new SpellTactics(4.0D, 18.0D, 32.0D, SpellRole.CONTROL, true)),
            Map.entry(MOD_ID + ":firecracker", new SpellTactics(4.0D, 14.0D, 24.0D, SpellRole.RANGED, true))
    );

    private static boolean attemptedInit;
    private static boolean ready;
    private static Method getSpell;
    private static Method isEnabled;
    private static Method getCastType;
    private static Method getSpellCooldown;
    private static Method getEffectiveCastTime;
    private static Method checkPreCastConditions;
    private static Method shouldAIStopCasting;
    private static Method onServerPreCast;
    private static Method onServerCastTick;
    private static Method onCast;
    private static Method onServerCastComplete;
    private static Method magicInitiateCast;
    private static Method magicSetSyncedData;

    // Iron's real 1.20.1 spell-container API. Keeping this reflective preserves
    // CyberNpc's soft/optional dependency on Iron's.
    private static Method spellContainerCreate;
    private static Method spellContainerGet;
    private static Method spellContainerSet;
    private static Method spellContainerMutableCopy;
    private static Method spellContainerGetActiveSpells;
    private static Method mutableAddSpell;
    private static Method mutableToImmutable;
    private static Method spellSlotGetSpell;
    private static Method spellSlotGetLevel;
    private static Method abstractSpellGetSpellId;

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

        // Every Mage must have a useful book: at least two usable spells and
        // at least one direct damage spell.
        List<String> attackSpells = new ArrayList<>();
        for (String id : available) {
            if (isAttackSpellId(id)) {
                attackSpells.add(id);
            }
        }

        if (available.size() < 2 || attackSpells.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = available.size() - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            String temp = available.get(i);
            available.set(i, available.get(swap));
            available.set(swap, temp);
        }

        int spellCount = Math.max(
                2,
                Math.min(
                        available.size(),
                        switch (gearTier) {
                            case STANDARD -> 2;
                            case FINE -> 3;
                            case RARE -> 3;
                            case ELITE -> 4;
                        }
                )
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

        if (!initialize()) {
            return ItemStack.EMPTY;
        }

        List<String> selected = new ArrayList<>();
        String guaranteedAttack = attackSpells.get(random.nextInt(attackSpells.size()));
        selected.add(guaranteedAttack);

        for (String id : available) {
            if (selected.size() >= spellCount) {
                break;
            }
            if (!selected.contains(id)) {
                selected.add(id);
            }
        }

        try {
            Object immutableContainer = spellContainerCreate.invoke(
                    null,
                    maxSlots,
                    true,
                    true
            );
            Object mutableContainer = spellContainerMutableCopy.invoke(
                    immutableContainer
            );

            int added = 0;
            for (String spellId : selected) {
                Object spell = getSpell.invoke(null, spellId);
                if (!isSupportedSpellObject(spell)) {
                    continue;
                }

                int level = Math.max(
                        1,
                        baseLevel + (random.nextFloat() < 0.20F ? 1 : 0)
                );

                boolean success = (Boolean) mutableAddSpell.invoke(
                        mutableContainer,
                        spell,
                        level,
                        false
                );
                if (success) {
                    added++;
                }
            }

            if (added < 2) {
                return ItemStack.EMPTY;
            }

            Object finalContainer = mutableToImmutable.invoke(mutableContainer);
            spellContainerSet.invoke(null, book, finalContainer);
            book.getOrCreateTag().putBoolean("CyberNpcMageSpellbook", true);
            return book;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
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

        // Constructing a fresh stack of the same Iron's book gives us its normal
        // empty spell container without carrying over the NPC's rolled spells.
        ItemStack droppedBook = new ItemStack(
                npcSpellBook.getItem(),
                npcSpellBook.getCount()
        );

        CompoundTag sourceTag = npcSpellBook.getTag();
        if (sourceTag != null && sourceTag.contains("display")) {
            droppedBook.getOrCreateTag().put(
                    "display",
                    sourceTag.getCompound("display").copy()
            );
        }

        return droppedBook;
    }

    public static List<SpellEntry> getBookSpells(ItemStack spellBook) {
        List<SpellEntry> result = new ArrayList<>();
        if (spellBook == null || spellBook.isEmpty() || !initialize()) {
            return result;
        }

        // First use Iron's real container API. This is what its tooltip, spell
        // wheel and other systems use in current 1.20.1 builds.
        try {
            Object container = spellContainerGet.invoke(null, spellBook);
            if (container != null) {
                @SuppressWarnings("unchecked")
                List<Object> activeSpells =
                        (List<Object>) spellContainerGetActiveSpells.invoke(container);

                for (Object slot : activeSpells) {
                    Object spell = spellSlotGetSpell.invoke(slot);
                    int level = Math.max(
                            1,
                            ((Number) spellSlotGetLevel.invoke(slot)).intValue()
                    );
                    String id = String.valueOf(
                            abstractSpellGetSpellId.invoke(spell)
                    );

                    if (!id.isBlank()) {
                        result.add(new SpellEntry(id, level));
                    }
                }

                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        // Compatibility fallback for NPC books created by early 0.14.x builds.
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

    public static boolean hasUsableCombatSpells(ItemStack spellBook) {
        int usable = 0;
        boolean hasAttack = false;

        for (SpellEntry entry : getBookSpells(spellBook)) {
            if (isExplicitlyUnsafeMobSpell(entry.spellId())
                    || !isSupportedCombatSpell(entry.spellId())) {
                continue;
            }

            usable++;
            if (isAttackSpellId(entry.spellId())) {
                hasAttack = true;
            }
        }

        return usable >= 2 && hasAttack;
    }

    public static CombatPlan getCombatPlan(
            CyberNpcEntity caster,
            LivingEntity target,
            ItemStack spellBook
    ) {
        if (caster == null
                || target == null
                || !target.isAlive()
                || spellBook == null
                || spellBook.isEmpty()) {
            return CombatPlan.none();
        }

        List<SpellEntry> spells = getBookSpells(spellBook);
        SpellEntry selected = selectBestTacticalSpell(
                caster,
                target,
                spells,
                false
        );

        if (selected == null) {
            int nextReady = getNextRelevantCooldownTicks(caster, target, spells);
            return nextReady > 0
                    ? CombatPlan.coolingDown(nextReady)
                    : CombatPlan.none();
        }

        SpellTactics tactics = tacticsFor(selected.spellId());
        double distance = caster.distanceTo(target);
        return new CombatPlan(
                true,
                false,
                0,
                canUseAtCurrentDistance(caster, tactics, distance),
                tactics.minRange,
                tactics.preferredRange,
                tactics.maxRange,
                tactics.requiresLineOfSight,
                selected.spellId(),
                tactics.role.name()
        );
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

            SpellTactics activeTactics = tacticsFor(active.spellId);
            double activeDistance = caster.distanceTo(target);
            if (activeTactics.role != SpellRole.HEAL
                    && activeTactics.role != SpellRole.DEFENSE
                    && activeDistance > activeTactics.maxRange * 1.30D) {
                cancelCast(caster);
                return CastResult.failed();
            }

            faceTarget(caster, target);

            try {
                boolean ironSaysStop = (Boolean) shouldAIStopCasting.invoke(
                        active.spell,
                        active.level,
                        caster,
                        target
                );
                if (ironSaysStop) {
                    cancelCast(caster);
                    return CastResult.failed();
                }

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
                    startSpellCooldown(caster, active.spellId, active.cooldownTicks);
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

        List<SpellEntry> candidates = new ArrayList<>(spells);
        candidates.sort((left, right) -> Double.compare(
                tacticalScore(caster, target, right),
                tacticalScore(caster, target, left)
        ));

        for (SpellEntry entry : candidates) {
            // Saved 0.14.x Mage books may already contain a spell that was later
            // classified as unsafe, so guard again at cast time as well as book creation.
            if (isExplicitlyUnsafeMobSpell(entry.spellId())
                    || !isSpellReady(caster, entry.spellId())) {
                continue;
            }

            SpellTactics tactics = tacticsFor(entry.spellId());
            double distance = caster.distanceTo(target);
            if (!canUseAtCurrentDistance(caster, tactics, distance)) {
                continue;
            }

            if ((tactics.role == SpellRole.RANGED
                    || tactics.role == SpellRole.CONTROL)
                    && !caster.hasClearFriendlyFireLane(target)) {
                continue;
            }

            try {
                Object spell = getSpell.invoke(null, entry.spellId());
                if (!isSupportedSpellObject(spell)) {
                    continue;
                }

                Object castType = getCastType.invoke(spell);
                String castTypeName = String.valueOf(castType);

                boolean ironSaysStop = (Boolean) shouldAIStopCasting.invoke(
                        spell,
                        entry.level(),
                        caster,
                        target
                );
                if (ironSaysStop) {
                    continue;
                }

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
                        10,
                        3600
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

                    startSpellCooldown(caster, entry.spellId(), cooldown);
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

    private static SpellEntry selectBestTacticalSpell(
            CyberNpcEntity caster,
            LivingEntity target,
            List<SpellEntry> spells,
            boolean requireUsableNow
    ) {
        SpellEntry best = null;
        double bestScore = -Double.MAX_VALUE;
        double distance = caster.distanceTo(target);

        for (SpellEntry entry : spells) {
            if (isExplicitlyUnsafeMobSpell(entry.spellId())
                    || !isSupportedCombatSpell(entry.spellId())
                    || !isSpellReady(caster, entry.spellId())) {
                continue;
            }

            SpellTactics tactics = tacticsFor(entry.spellId());
            if (!isRoleUseful(caster, tactics, distance)) {
                continue;
            }

            if (requireUsableNow
                    && !canUseAtCurrentDistance(caster, tactics, distance)) {
                continue;
            }

            double score = tacticalScore(caster, target, entry);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        return best;
    }

    private static double tacticalScore(
            CyberNpcEntity caster,
            LivingEntity target,
            SpellEntry entry
    ) {
        SpellTactics tactics = tacticsFor(entry.spellId());
        double distance = caster.distanceTo(target);
        double healthFraction = caster.getMaxHealth() <= 0.0F
                ? 1.0D
                : caster.getHealth() / caster.getMaxHealth();

        if (tactics.role == SpellRole.HEAL) {
            if (healthFraction >= 0.78D) {
                return -250.0D;
            }
            return 180.0D + (1.0D - healthFraction) * 180.0D;
        }

        if (tactics.role == SpellRole.DEFENSE) {
            if (healthFraction < 0.62D || distance < 7.0D) {
                return 135.0D
                        + (1.0D - healthFraction) * 80.0D
                        + Math.max(0.0D, 7.0D - distance) * 5.0D;
            }
            return 5.0D;
        }

        double distancePenalty = Math.abs(distance - tactics.preferredRange) * 4.0D;
        if (distance < tactics.minRange) {
            distancePenalty += (tactics.minRange - distance) * 14.0D;
        } else if (distance > tactics.maxRange) {
            distancePenalty += (distance - tactics.maxRange) * 16.0D;
        }

        double score = 100.0D - distancePenalty;

        if (tactics.role == SpellRole.CONTROL) {
            score += distance >= 6.0D ? 12.0D : -10.0D;
        } else if (tactics.role == SpellRole.CLOSE) {
            score += distance <= tactics.maxRange ? 15.0D : 0.0D;
        } else if (tactics.role == SpellRole.RANGED) {
            score += distance >= 7.0D ? 10.0D : -4.0D;
        }

        // Small stable preference for higher-level rolls when two spells make
        // similar tactical sense.
        score += Math.min(8.0D, entry.level() * 1.25D);
        return score;
    }

    private static boolean canUseAtCurrentDistance(
            CyberNpcEntity caster,
            SpellTactics tactics,
            double distance
    ) {
        if (!isRoleUseful(caster, tactics, distance)) {
            return false;
        }

        if (tactics.role == SpellRole.HEAL
                || tactics.role == SpellRole.DEFENSE) {
            return true;
        }

        return distance >= tactics.minRange && distance <= tactics.maxRange;
    }

    private static boolean isRoleUseful(
            CyberNpcEntity caster,
            SpellTactics tactics,
            double distance
    ) {
        double healthFraction = caster.getMaxHealth() <= 0.0F
                ? 1.0D
                : caster.getHealth() / caster.getMaxHealth();

        if (tactics.role == SpellRole.HEAL) {
            return healthFraction < 0.78D;
        }

        if (tactics.role == SpellRole.DEFENSE) {
            return healthFraction < 0.62D || distance < 7.0D;
        }

        return true;
    }

    private static boolean isAttackSpellId(String spellId) {
        SpellRole role = tacticsFor(spellId).role;
        return role == SpellRole.CLOSE || role == SpellRole.RANGED;
    }

    public static String getSpellRoleName(String spellId) {
        return switch (tacticsFor(spellId).role) {
            case CLOSE -> "Close attack";
            case RANGED -> "Ranged attack";
            case CONTROL -> "Control";
            case HEAL -> "Heal";
            case DEFENSE -> "Defense";
        };
    }

    public static double estimatePotentialDamage(
            CyberNpcEntity caster,
            ItemStack spellBook
    ) {
        double best = 0.0D;

        for (SpellEntry entry : getBookSpells(spellBook)) {
            if (!isSupportedCombatSpell(entry.spellId())) {
                continue;
            }

            SpellRole role = tacticsFor(entry.spellId()).role;
            double estimate = switch (role) {
                case CLOSE, RANGED -> 5.0D + entry.level() * 2.5D;
                case CONTROL -> 2.0D + entry.level() * 1.25D;
                default -> 0.0D;
            };
            best = Math.max(best, estimate);
        }

        return best;
    }

    public static int getRemainingCooldownTicks(
            CyberNpcEntity caster,
            String spellId
    ) {
        if (caster == null || spellId == null || spellId.isBlank()) {
            return 0;
        }

        Map<String, Long> cooldowns = SPELL_COOLDOWNS.get(caster.getUUID());
        if (cooldowns == null) {
            return 0;
        }

        String normalized = normalizeSpellId(spellId);
        Long until = cooldowns.get(normalized);
        if (until == null) {
            return 0;
        }

        long remaining = until - caster.level().getGameTime();
        if (remaining <= 0L) {
            cooldowns.remove(normalized);
            if (cooldowns.isEmpty()) {
                SPELL_COOLDOWNS.remove(caster.getUUID());
            }
            return 0;
        }

        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static boolean isSpellReady(
            CyberNpcEntity caster,
            String spellId
    ) {
        return getRemainingCooldownTicks(caster, spellId) <= 0;
    }

    private static void startSpellCooldown(
            CyberNpcEntity caster,
            String spellId,
            int cooldownTicks
    ) {
        if (caster == null || spellId == null || spellId.isBlank()) {
            return;
        }

        String normalized = normalizeSpellId(spellId);
        SPELL_COOLDOWNS
                .computeIfAbsent(caster.getUUID(), ignored -> new HashMap<>())
                .put(
                        normalized,
                        caster.level().getGameTime() + Math.max(1, cooldownTicks)
                );
    }

    private static int getNextRelevantCooldownTicks(
            CyberNpcEntity caster,
            LivingEntity target,
            List<SpellEntry> spells
    ) {
        int best = Integer.MAX_VALUE;
        double distance = caster.distanceTo(target);

        for (SpellEntry entry : spells) {
            if (isExplicitlyUnsafeMobSpell(entry.spellId())
                    || !isSupportedCombatSpell(entry.spellId())) {
                continue;
            }

            SpellTactics tactics = tacticsFor(entry.spellId());
            if (!isRoleUseful(caster, tactics, distance)) {
                continue;
            }

            int remaining = getRemainingCooldownTicks(caster, entry.spellId());
            if (remaining > 0) {
                best = Math.min(best, remaining);
            }
        }

        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public static void clearCasterState(CyberNpcEntity caster) {
        if (caster == null) {
            return;
        }

        ACTIVE_CASTS.remove(caster.getUUID());
        SPELL_COOLDOWNS.remove(caster.getUUID());
    }

    private static String normalizeSpellId(String spellId) {
        String trimmed = spellId == null ? "" : spellId.trim();
        return trimmed.contains(":") ? trimmed : MOD_ID + ":" + trimmed;
    }

    private static SpellTactics tacticsFor(String spellId) {
        String normalized = spellId == null
                ? ""
                : (spellId.contains(":") ? spellId.trim() : MOD_ID + ":" + spellId.trim());
        return SPELL_TACTICS.getOrDefault(normalized, SpellTactics.mid());
    }

    private static boolean isExplicitlyUnsafeMobSpell(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return true;
        }

        return MOB_UNSAFE_SPELLS.contains(normalizeSpellId(spellId));
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
            Class<?> spellContainerClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.ISpellContainer"
            );
            Class<?> mutableSpellContainerClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable"
            );
            Class<?> spellSlotClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.SpellSlot"
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
            shouldAIStopCasting = abstractSpellClass.getMethod(
                    "shouldAIStopCasting",
                    int.class,
                    Mob.class,
                    LivingEntity.class
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

            spellContainerCreate = spellContainerClass.getMethod(
                    "create",
                    int.class,
                    boolean.class,
                    boolean.class
            );
            spellContainerGet = spellContainerClass.getMethod(
                    "get",
                    ItemStack.class
            );
            spellContainerSet = spellContainerClass.getMethod(
                    "set",
                    ItemStack.class,
                    spellContainerClass
            );
            spellContainerMutableCopy = spellContainerClass.getMethod(
                    "mutableCopy"
            );
            spellContainerGetActiveSpells = spellContainerClass.getMethod(
                    "getActiveSpells"
            );
            mutableAddSpell = mutableSpellContainerClass.getMethod(
                    "addSpell",
                    abstractSpellClass,
                    int.class,
                    boolean.class
            );
            mutableToImmutable = mutableSpellContainerClass.getMethod(
                    "toImmutable"
            );
            spellSlotGetSpell = spellSlotClass.getMethod("getSpell");
            spellSlotGetLevel = spellSlotClass.getMethod("getLevel");
            abstractSpellGetSpellId = abstractSpellClass.getMethod("getSpellId");

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

    private enum SpellRole {
        CLOSE,
        RANGED,
        CONTROL,
        HEAL,
        DEFENSE
    }

    private record SpellTactics(
            double minRange,
            double preferredRange,
            double maxRange,
            SpellRole role,
            boolean requiresLineOfSight
    ) {
        private static SpellTactics closeAoe() {
            return new SpellTactics(0.0D, 4.0D, 9.0D, SpellRole.CLOSE, false);
        }

        private static SpellTactics mid() {
            return new SpellTactics(2.0D, 10.0D, 24.0D, SpellRole.RANGED, true);
        }

        private static SpellTactics longRange() {
            return new SpellTactics(5.0D, 16.0D, 32.0D, SpellRole.RANGED, true);
        }
    }

    public record CombatPlan(
            boolean available,
            boolean coolingDown,
            int nextReadyTicks,
            boolean readyToCast,
            double minRange,
            double preferredRange,
            double maxRange,
            boolean requiresLineOfSight,
            String spellId,
            String role
    ) {
        public static CombatPlan none() {
            return new CombatPlan(
                    false,
                    false,
                    0,
                    false,
                    0.0D,
                    10.0D,
                    18.0D,
                    true,
                    "",
                    ""
            );
        }

        public static CombatPlan coolingDown(int nextReadyTicks) {
            return new CombatPlan(
                    false,
                    true,
                    Math.max(1, nextReadyTicks),
                    false,
                    0.0D,
                    14.0D,
                    24.0D,
                    true,
                    "",
                    "COOLDOWN"
            );
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
