package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.client.render.ChosoPlushItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class ChosoPlushItem extends Item {
    public ChosoPlushItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ChosoPlushItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ChosoPlushItemRenderer();
                }
                return renderer;
            }
        });
    }
}
