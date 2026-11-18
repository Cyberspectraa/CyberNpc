package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Lightweight non-verbal voice layer. CyberNpc deliberately does not speak:
 * Wild NPCs use the supplied, cleaned male/female grunt recordings.
 * Existing non-Wild reaction sounds are deliberately left unchanged.
 */
final class NpcVocalizationController {
    private static final int AMBIENT_MIN_TICKS = 700;
    private static final int AMBIENT_RANDOM_TICKS = 900;
    private static final int REACTION_COOLDOWN_TICKS = 70;
    private static final int HURT_COOLDOWN_TICKS = 24;

    private final CyberNpcEntity npc;

    private int ambientCooldown;
    private int reactionCooldown;
    private int hurtCooldown;

    NpcVocalizationController(CyberNpcEntity npc) {
        this.npc = npc;
        resetAmbient();
    }

    void tick() {
        if (reactionCooldown > 0) {
            reactionCooldown--;
        }
        if (hurtCooldown > 0) {
            hurtCooldown--;
        }

        if (!canMakePassiveSound()) {
            return;
        }

        if (ambientCooldown > 0) {
            ambientCooldown--;
            return;
        }

        resetAmbient();

        if (npc.getRandom().nextFloat() < 0.58F) {
            play(voice(npc.getRandom().nextBoolean() ? 0 : 1),
                    0.50F, passivePitch());
        }
    }

    void react(NpcReactionIcon icon) {
        if (icon == null
                || icon == NpcReactionIcon.NONE
                || reactionCooldown > 0
                || !npc.isAlive()) {
            return;
        }

        if (npc.getNpcType() == NpcType.WILD) {
            // Match the short syllable to the emotion. No Piglin reactions.
            int syllable = switch (icon) {
                case THINKING, CONFUSED, GROUP_INVITE, HOME, SLEEP, ANNOYED -> 1;
                case SURPRISED, DANGER, SCARED, SAD, ANGRY, COMBAT -> 2;
                default -> 0;
            };
            play(voice(syllable),
                    syllable == 2 ? 0.70F : 0.58F,
                    reactionPitch());
            reactionCooldown = REACTION_COOLDOWN_TICKS
                    + npc.getRandom().nextInt(31);
            return;
        }

        SoundEvent sound;
        float volume;
        float pitch = reactionPitch();

        switch (icon) {
            case HAPPY, FRIENDLY, GREETING, GROUP_ACCEPT, FOOD, MOUNT -> {
                sound = npc.getRandom().nextBoolean()
                        ? SoundEvents.PIGLIN_CELEBRATE
                        : SoundEvents.PIGLIN_AMBIENT;
                volume = 0.30F;
                pitch += 0.08F;
            }
            case THINKING, CONFUSED, GROUP_INVITE, HOME, SLEEP -> {
                sound = npc.getRandom().nextBoolean()
                        ? SoundEvents.PIGLIN_ADMIRING_ITEM
                        : SoundEvents.PIGLIN_AMBIENT;
                volume = 0.25F;
                pitch -= 0.03F;
            }
            case SURPRISED, DANGER -> {
                sound = SoundEvents.PIGLIN_RETREAT;
                volume = 0.34F;
                pitch += 0.04F;
            }
            case SCARED, SAD -> {
                sound = SoundEvents.PIGLIN_RETREAT;
                volume = 0.31F;
                pitch -= 0.08F;
            }
            case ANNOYED -> {
                sound = SoundEvents.PIGLIN_JEALOUS;
                volume = 0.29F;
                pitch -= 0.05F;
            }
            case ANGRY, COMBAT -> {
                sound = SoundEvents.PIGLIN_ANGRY;
                volume = 0.36F;
                pitch -= 0.08F;
            }
            default -> {
                sound = SoundEvents.PIGLIN_AMBIENT;
                volume = 0.24F;
            }
        }

        play(sound, volume, pitch);
        reactionCooldown = REACTION_COOLDOWN_TICKS
                + npc.getRandom().nextInt(31);
    }

    void hurt(float damage) {
        if (hurtCooldown > 0 || !npc.isAlive()) {
            return;
        }

        play(
                voice(2),
                damage >= 5.0F ? 0.78F : 0.66F,
                reactionPitch() - (damage >= 5.0F ? 0.07F : 0.02F)
        );
        hurtCooldown = HURT_COOLDOWN_TICKS;
        reactionCooldown = Math.max(reactionCooldown, 20);
    }

    /** Uses the Wild-NPC recordings, not the dialogue screen's voice assets. */
    private SoundEvent voice(int syllable) {
        boolean female = npc.getAppearanceGender() == NpcAppearance.Gender.FEMALE;
        return switch (syllable) {
            case 1 -> female ? ModSounds.WILD_FEMALE_MM.get()
                    : ModSounds.WILD_MALE_MM.get();
            case 2 -> female ? ModSounds.WILD_FEMALE_UH.get()
                    : ModSounds.WILD_MALE_UH.get();
            default -> female ? ModSounds.WILD_FEMALE_HM.get()
                    : ModSounds.WILD_MALE_HM.get();
        };
    }

    private boolean canMakePassiveSound() {
        return npc.getNpcType() == NpcType.WILD
                && npc.isAlive()
                && !npc.isSleeping()
                && !npc.isCombatActive()
                && !npc.isZombifying();
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        if (npc.level().isClientSide) {
            return;
        }

        npc.level().playSound(
                null,
                npc.getX(),
                npc.getY(),
                npc.getZ(),
                sound,
                SoundSource.NEUTRAL,
                volume,
                Math.max(0.55F, Math.min(1.25F, pitch))
        );
    }

    private float passivePitch() {
        float personality = switch (npc.getPersonality()) {
            case RECKLESS, AGGRESSIVE -> -0.08F;
            case SKITTISH -> 0.10F;
            case PATIENT, STUBBORN -> -0.04F;
            default -> 0.0F;
        };
        return 1.0F + personality * 0.35F
                + (npc.getRandom().nextFloat() - 0.5F) * 0.07F;
    }

    private float reactionPitch() {
        return 1.0F + (npc.getRandom().nextFloat() - 0.5F) * 0.08F;
    }

    private void resetAmbient() {
        ambientCooldown = AMBIENT_MIN_TICKS
                + npc.getRandom().nextInt(AMBIENT_RANDOM_TICKS + 1);
    }
}
