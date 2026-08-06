package com.minerguy341.morefloorstorage.common.blockentity;

import java.util.function.Supplier;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MFSBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MoreFloorStorage.MOD_ID);

    public static final Supplier<BlockEntityType<PileBlockEntity>> CLAY_PILE = pile("clay_pile", MFSBlocks.CLAY_PILE);
    public static final Supplier<BlockEntityType<PileBlockEntity>> ORE_PILE = pile("ore_pile", MFSBlocks.ORE_PILE);

    public static final Supplier<BlockEntityType<LeaningToolBlockEntity>> LEANING_TOOL = BLOCK_ENTITIES.register(
        "leaning_tool",
        () -> BlockEntityType.Builder.of(LeaningToolBlockEntity::new, MFSBlocks.LEANING_TOOL.get()).build(null));

    /**
     * Every kind of pile shares {@link PileBlockEntity} but needs its own type, so that a block entity
     * created for one kind is never accepted by another.
     */
    private static Supplier<BlockEntityType<PileBlockEntity>> pile(String name, Supplier<? extends Block> block)
    {
        // The type is looked up again at creation time; it cannot be captured, since it is what is being built
        final Supplier<BlockEntityType<PileBlockEntity>>[] self = newHolder();
        self[0] = BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder
            .of((pos, state) -> new PileBlockEntity(self[0].get(), pos, state), block.get())
            .build(null));
        return self[0];
    }

    @SuppressWarnings("unchecked")
    private static Supplier<BlockEntityType<PileBlockEntity>>[] newHolder()
    {
        return new Supplier[1];
    }

    private MFSBlockEntities() {}
}
