package com.minerguy341.morefloorstorage.common.block;

import java.util.function.Supplier;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.common.blockentity.MFSBlockEntities;
import com.minerguy341.morefloorstorage.common.blockentity.PileBlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MFSBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MoreFloorStorage.MOD_ID);

    public static final DeferredBlock<PileBlock> CLAY_PILE = pile("clay_pile", MapColor.CLAY, SoundType.GRAVEL, 0.5f,
        () -> MFSBlockEntities.CLAY_PILE);

    public static final DeferredBlock<PileBlock> ORE_PILE = pile("ore_pile", MapColor.STONE, SoundType.STONE, 0.7f,
        () -> MFSBlockEntities.ORE_PILE);

    public static final DeferredBlock<LeaningToolBlock> LEANING_TOOL = BLOCKS.register("leaning_tool", () -> new LeaningToolBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .instabreak()
            .sound(SoundType.WOOD)
            .noOcclusion()
            .noCollission()
            .isViewBlocking((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
    ));

    /**
     * Registers one kind of pile. The block entity type is reached through a supplier of a supplier
     * because the type is registered against the block being created here.
     */
    private static DeferredBlock<PileBlock> pile(String name, MapColor color, SoundType sound, float strength,
                                                 Supplier<Supplier<BlockEntityType<PileBlockEntity>>> blockEntityType)
    {
        return BLOCKS.register(name, () -> new PileBlock(
            BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(strength)
                .sound(sound)
                .noOcclusion()
                .dynamicShape() // The shape depends on whether neighbouring piles have merged into a wide pyramid
                .isViewBlocking((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable() // Contents are dropped by the block entity instead
            , blockEntityType.get()));
    }

    private MFSBlocks() {}
}
