package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.block.DropBoxBlock;
import com.cyberspectraa.cybernpc.block.LetterBoxBlock;
import com.cyberspectraa.cybernpc.block.GuardPostBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CyberNpc.MOD_ID);

    public static final RegistryObject<Block> DROP_BOX =
            BLOCKS.register("drop_box", () ->
                    new DropBoxBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_RED)
                                    .strength(2.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    ));

    public static final RegistryObject<Block> LETTER_BOX =
            BLOCKS.register("letter_box", () ->
                    new LetterBoxBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(2.0F, 3.0F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion()
                    ));

    public static final RegistryObject<Block> GUARD_POST =
            BLOCKS.register("guard_post", () ->
                    new GuardPostBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .strength(0.2F)
                                    .noCollission()
                                    .noOcclusion()
                    ));

    private ModBlocks() {
    }
}
