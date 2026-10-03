package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.building.BuildingMarkerType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class BuildingMarkerItem extends Item {
    private final BuildingMarkerType markerType;

    public BuildingMarkerItem(
            BuildingMarkerType markerType,
            Properties properties
    ) {
        super(properties);
        this.markerType = markerType;
    }

    public BuildingMarkerType markerType() {
        return markerType;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal(markerType.displayName() + " Marker")
                        .withStyle(ChatFormatting.AQUA)
        );

        if (markerType.isPrimary()) {
            tooltip.add(
                    Component.literal(
                            "Put this in an item frame inside the main room."
                    ).withStyle(ChatFormatting.GRAY)
            );
            tooltip.add(
                    Component.literal(
                            "CyberNpc detects the enclosed room automatically."
                    ).withStyle(ChatFormatting.GRAY)
            );
        } else if (markerType.isRoomMarker()) {
            tooltip.add(
                    Component.literal(
                            "Put this in an item frame inside the room."
                    ).withStyle(ChatFormatting.GRAY)
            );
            tooltip.add(
                    Component.literal(
                            "It links to the main building automatically."
                    ).withStyle(ChatFormatting.GRAY)
            );
        } else {
            tooltip.add(
                    Component.literal(
                            "Put this in an item frame at the exact activity spot."
                    ).withStyle(ChatFormatting.GRAY)
            );
        }
    }
}
