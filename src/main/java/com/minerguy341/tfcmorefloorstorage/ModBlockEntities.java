package com.minerguy341.tfcmorefloorstorage;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TFCMoreFloorStorage.MOD_ID);

    public static final RegistryObject<BlockEntityType<ClayPileBlockEntity>> CLAY_PILE = BLOCK_ENTITIES.register(
        "clay_pile",
        () -> BlockEntityType.Builder.of(ClayPileBlockEntity::new, ModBlocks.CLAY_PILE.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<LeaningToolBlockEntity>> LEANING_TOOL = BLOCK_ENTITIES.register(
        "leaning_tool",
        () -> BlockEntityType.Builder.of(LeaningToolBlockEntity::new, ModBlocks.LEANING_TOOL.get()).build(null)
    );

    private ModBlockEntities() {}
}
