package com.minerguy341.tfcmorefloorstorage;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, TFCMoreFloorStorage.MOD_ID);

    public static final RegistryObject<ClayPileBlock> CLAY_PILE = BLOCKS.register("clay_pile", () -> new ClayPileBlock(
        ExtendedProperties.of(MapColor.CLAY)
            .strength(0.5f)
            .sound(SoundType.GRAVEL)
            .noOcclusion()
            .blockEntity(ModBlockEntities.CLAY_PILE)
    ));

    public static final RegistryObject<LeaningToolBlock> LEANING_TOOL = BLOCKS.register("leaning_tool", () -> new LeaningToolBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .strength(0.2f)
            .sound(SoundType.WOOD)
            .noOcclusion()
    ));

    private ModBlocks() {}
}
