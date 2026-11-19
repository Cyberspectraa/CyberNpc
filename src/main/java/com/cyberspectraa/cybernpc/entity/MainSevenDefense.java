package com.cyberspectraa.cybernpc.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;

/**
 * A small, deliberately defensive combat layer for the seven authored town
 * characters. These NPCs do not use Wild NPC class AI, hunting, faction
 * aggression or guard routines. They only react to monsters close to them.
 */
public final class MainSevenDefense {
    private record Loadout(Item item, String name) {}

    // Fixed, save-stable vanilla equipment; no optional weapon mod dependency.
    // Individual names preserve character identity even when an item is reused.
    private static final Map<String, Loadout> WEAPONS = Map.of(
            "mason", new Loadout(Items.IRON_SWORD, "Kai's Blade"),
            "town_npc_1", new Loadout(Items.STONE_SWORD, "Katie's Shortblade"),
            "town_npc_2", new Loadout(Items.IRON_AXE, "Elara's Trail Axe"),
            "town_npc_3", new Loadout(Items.STONE_AXE, "Rowan's Hatchet"),
            "town_npc_4", new Loadout(Items.GOLDEN_SWORD, "Felix's Gilded Sword"),
            "town_npc_5", new Loadout(Items.IRON_PICKAXE, "Nadia's Workshop Pick"),
            "town_npc_6", new Loadout(Items.GOLDEN_AXE, "Orin's Keeper Axe")
    );

    private MainSevenDefense() {}

    public static boolean isMainSeven(CyberNpcEntity npc) {
        return npc != null
                && npc.getNpcType() != NpcType.WILD
                && WEAPONS.containsKey(npc.getStoryNpcId());
    }

    /**
     * Assign equipment only when the main hand is empty. This means existing
     * characters in old worlds are equipped on tick, while manually supplied
     * or datapack-controlled weapons are not overwritten.
     */
    public static void ensureWeapon(CyberNpcEntity npc) {
        if (npc == null || npc.level().isClientSide
                || !npc.isAlive() || !isMainSeven(npc)
                || !npc.getMainHandItem().isEmpty()) {
            return;
        }
        Loadout profile = WEAPONS.get(npc.getStoryNpcId());
        ItemStack weapon = new ItemStack(profile.item());
        weapon.setHoverName(Component.literal(profile.name()));
        npc.setItemSlot(EquipmentSlot.MAINHAND, weapon);
    }

    public static final class DefenseGoal extends Goal {
        private static final double NOTICE_RADIUS = 7.0D;
        private static final double CLOSE_DANGER_SQR = 16.0D;
        private static final double ATTACK_RANGE_SQR = 6.25D;
        private static final double MAX_CHASE_FROM_START_SQR = 100.0D;
        private static final int HIT_COOLDOWN = 20;

        private final CyberNpcEntity npc;
        @Nullable private Mob enemy;
        @Nullable private Vec3 startingSpot;
        private int hitCooldown;

        public DefenseGoal(CyberNpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isMainSeven(npc) || !npc.isAlive() || npc.isZombifying()
                    || npc.tickCount % 8 != Math.floorMod(npc.getUUID().hashCode(), 8)) {
                return false;
            }
            enemy = npc.level().getEntitiesOfClass(
                    Mob.class,
                    npc.getBoundingBox().inflate(NOTICE_RADIUS, 4.0D, NOTICE_RADIUS),
                    mob -> mob != npc
                            && mob.isAlive()
                            && mob instanceof Enemy
                            && !mob.isAlliedTo(npc)
                            && (mob.getTarget() == npc
                                || npc.distanceToSqr(mob) <= CLOSE_DANGER_SQR)
                            && npc.getSensing().hasLineOfSight(mob)
            ).stream().min(Comparator.comparingDouble(npc::distanceToSqr))
                    .orElse(null);
            return enemy != null;
        }

        @Override
        public boolean canContinueToUse() {
            return isMainSeven(npc)
                    && npc.isAlive()
                    && !npc.isZombifying()
                    && enemy != null
                    && enemy.isAlive()
                    && enemy instanceof Enemy
                    && !enemy.isAlliedTo(npc)
                    && npc.distanceToSqr(enemy) <= 144.0D
                    && startingSpot != null
                    && npc.distanceToSqr(startingSpot) <= MAX_CHASE_FROM_START_SQR
                    && enemy.distanceToSqr(startingSpot) <= MAX_CHASE_FROM_START_SQR;
        }

        @Override
        public void start() {
            startingSpot = npc.position();
            hitCooldown = 0;
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (enemy == null || !enemy.isAlive()) return;
            if (hitCooldown > 0) hitCooldown--;

            npc.getLookControl().setLookAt(enemy, 30.0F, 30.0F);

            if (npc.distanceToSqr(enemy) > ATTACK_RANGE_SQR) {
                npc.getNavigation().moveTo(enemy, 1.08D);
                return;
            }

            npc.getNavigation().stop();
            if (hitCooldown <= 0 && npc.getSensing().hasLineOfSight(enemy)) {
                npc.swing(InteractionHand.MAIN_HAND);
                npc.doHurtTarget(enemy);
                hitCooldown = HIT_COOLDOWN;
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            // Return to the spot where the fight began, but never teleport
            // the character or pull them across a large area of the town.
            if (startingSpot != null && npc.distanceToSqr(startingSpot) > 4.0D) {
                npc.getNavigation().moveTo(
                        startingSpot.x, startingSpot.y, startingSpot.z, 0.78D);
            }
            enemy = null;
            startingSpot = null;
            hitCooldown = 0;
        }
    }
}
