package com.minerguy341.tfcmorefloorstorage;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, TFCMoreFloorStorage.MOD_ID);

    public static final RegistryObject<ClayPileBlock> CLAY_PILE = BLOCKS.register("clay_pile", () -> new ClayPileBlock(
        ExtendedProperties.of(MapColor.CLAY)
            .strength(0.5f)
            .sound(SoundType.GRAVEL)
            .noOcclusion()
            .blockEntity(ModBlockEntities.CLAY_PILE)
    ));

    private ModBlocks() {}
}
