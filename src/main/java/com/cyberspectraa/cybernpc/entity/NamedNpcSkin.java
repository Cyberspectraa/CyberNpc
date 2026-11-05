package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Fixed, artist-authored skins for named story characters and two service
 * roles. The mapping is shared by the renderer and the player model selection,
 * so wide/slim arm UVs always agree with the supplied 64x64 skin.
 *
 * Story IDs and role names stay unchanged; no NBT or registry migration.
 */
public enum NamedNpcSkin {
    KAI("kai", false),
    KATIE("katie", false),
    ELARA("elara", true),
    ROWAN("rowan", false),
    FELIX("felix", false),
    NADIA("nadia", true),
    ORIN("orin", false),
    POPE("pope", false),
    GUILD_RECEPTIONIST("guild_receptionist", false);

    private final ResourceLocation texture;
    private final boolean slimArms;

    NamedNpcSkin(String fileName, boolean slimArms) {
        this.texture = new ResourceLocation(
                CyberNpc.MOD_ID, "textures/entity/characters/" + fileName + ".png");
        this.slimArms = slimArms;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public boolean slimArms() {
        return slimArms;
    }

    /**
     * Story IDs are authoritative even if the player changes the NPC's name.
     * Pope and receptionist skins apply to their existing service roles.
     * Retain the legacy Mason custom-name fallback for older worlds.
     */
    public static NamedNpcSkin forNpc(CyberNpcEntity npc) {
        String storyId = npc.getStoryNpcId();
        if (storyId != null) {
            NamedNpcSkin named = switch (storyId.toLowerCase(Locale.ROOT)) {
                case "mason" -> KAI; // persisted story ID is intentionally not renamed
                case "town_npc_1" -> KATIE;
                case "town_npc_2" -> ELARA;
                case "town_npc_3" -> ROWAN;
                case "town_npc_4" -> FELIX;
                case "town_npc_5" -> NADIA;
                case "town_npc_6" -> ORIN;
                default -> null;
            };
            if (named != null) return named;
        }

        if (npc.getCustomName() != null
                && "Mason".equalsIgnoreCase(npc.getCustomName().getString())) {
            return KAI;
        }

        return switch (NpcServiceRole.fromRole(npc.getRole())) {
            case POPE -> POPE;
            case GUILD_RECEPTIONIST -> GUILD_RECEPTIONIST;
            default -> null;
        };
    }
}
