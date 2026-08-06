package com.minerguy341.morefloorstorage.common.block;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MFSBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MoreFloorStorage.MOD_ID);

    public static final DeferredBlock<ClayPileBlock> CLAY_PILE = BLOCKS.register("clay_pile", () -> new ClayPileBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.CLAY)
            .strength(0.5f)
            .sound(SoundType.GRAVEL)
            .noOcclusion()
            .isViewBlocking((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable() // Contents are dropped by the block entity instead
    ));

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

    private MFSBlocks() {}
}
