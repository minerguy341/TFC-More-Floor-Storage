package com.minerguy341.morefloorstorage.common.blockentity;

import java.util.function.Supplier;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MFSBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MoreFloorStorage.MOD_ID);

    public static final Supplier<BlockEntityType<ClayPileBlockEntity>> CLAY_PILE = BLOCK_ENTITIES.register(
        "clay_pile",
        () -> BlockEntityType.Builder.of(ClayPileBlockEntity::new, MFSBlocks.CLAY_PILE.get()).build(null));

    public static final Supplier<BlockEntityType<LeaningToolBlockEntity>> LEANING_TOOL = BLOCK_ENTITIES.register(
        "leaning_tool",
        () -> BlockEntityType.Builder.of(LeaningToolBlockEntity::new, MFSBlocks.LEANING_TOOL.get()).build(null));

    private MFSBlockEntities() {}
}
