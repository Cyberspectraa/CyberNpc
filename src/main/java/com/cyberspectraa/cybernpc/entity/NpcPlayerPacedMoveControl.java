package com.cyberspectraa.cybernpc.entity;

import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * CyberNpc's pathfinding goals request different movement multipliers, but
 * running on foot should never outrun a vanilla sprinting player.  Limit the
 * MoveControl output rather than rewriting every combat, flee and follow goal.
 * This also corrects already-saved NPCs with the older high speed attributes.
 */
final class NpcPlayerPacedMoveControl extends MoveControl {
    // Vanilla player speeds on level ground, in blocks per game tick.
    private static final float PLAYER_WALK_SPEED = 0.215F;
    private static final float PLAYER_SPRINT_SPEED = 0.2806F;

    private final CyberNpcEntity npc;

    NpcPlayerPacedMoveControl(CyberNpcEntity npc) {
        super(npc);
        this.npc = npc;
    }

    @Override
    public void tick() {
        super.tick();

        // Keep dedicated mount, water and climbing behaviour intact.
        if (npc.isPassenger() || npc.isInWater() || npc.isInLava()
                || npc.onClimbable()) {
            return;
        }

        float speedLimit = npc.isSprinting()
                ? PLAYER_SPRINT_SPEED
                : PLAYER_WALK_SPEED;
        if (npc.getSpeed() > speedLimit) {
            npc.setSpeed(speedLimit);
        }
    }
}
