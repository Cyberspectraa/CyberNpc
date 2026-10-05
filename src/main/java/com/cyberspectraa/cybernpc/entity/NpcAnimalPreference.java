package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/**
 * Stable per-NPC animal taste. Opinions are based on NPC identity + species,
 * so an NPC keeps liking/disliking the same animal type across sessions.
 */
final class NpcAnimalPreference {
    private NpcAnimalPreference() {
    }

    static Opinion opinion(
            CyberNpcEntity npc,
            LivingEntity entity
    ) {
        if (!(entity instanceof Animal animal)) {
            return Opinion.NEUTRAL;
        }

        if (animal instanceof AbstractHorse
                && "ranger".equals(npc.getWildClass())) {
            return Opinion.LIKE;
        }

        String species = BuiltInRegistries.ENTITY_TYPE
                .getKey(animal.getType())
                .toString();

        int hash = 17;
        hash = 31 * hash + npc.getUUID().hashCode();
        hash = 31 * hash + species.hashCode();
        hash = 31 * hash + npc.getPersonality().ordinal() * 131;

        int roll = Math.floorMod(hash, 100);

        int likeBelow = switch (npc.getPersonality()) {
            case PATIENT, LOYAL -> 53;
            case BALANCED, PROTECTIVE -> 47;
            case BRAVE, OPPORTUNISTIC -> 43;
            case TACTICAL, CAUTIOUS -> 39;
            case STUBBORN -> 36;
            case SKITTISH -> 31;
            case AGGRESSIVE -> 27;
            case RECKLESS -> 24;
        };

        int dislikeAbove = switch (npc.getPersonality()) {
            case SKITTISH -> 67;
            case CAUTIOUS -> 73;
            case AGGRESSIVE, RECKLESS -> 76;
            case STUBBORN -> 79;
            default -> 84;
        };

        if (animal.isBaby()) {
            likeBelow = Math.min(70, likeBelow + 12);
            dislikeAbove = Math.min(96, dislikeAbove + 8);
        }

        if (animal instanceof Cat) {
            likeBelow = Math.min(72, likeBelow + 8);
        } else if (animal instanceof Wolf
                && (npc.getPersonality() == WildNpcPersonality.SKITTISH
                || npc.getPersonality() == WildNpcPersonality.CAUTIOUS)) {
            dislikeAbove -= 8;
        }

        if (roll < likeBelow) {
            return Opinion.LIKE;
        }
        if (roll >= dislikeAbove) {
            return Opinion.DISLIKE;
        }
        return Opinion.NEUTRAL;
    }

    static String animalName(LivingEntity entity) {
        return entity == null
                ? "animal"
                : entity.getType().getDescription().getString();
    }

    enum Opinion {
        LIKE,
        NEUTRAL,
        DISLIKE
    }
}
