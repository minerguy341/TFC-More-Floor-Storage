package com.minerguy341.morefloorstorage.common;

import com.minerguy341.morefloorstorage.common.block.ClayPileBlock;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import com.minerguy341.morefloorstorage.common.blockentity.ClayPileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Placement half of the clay pile mechanic. Mirrors how TerraFirmaCraft handles ingot piling: sneak-click
 * with a clay-type item to add one to the pile you clicked, climbing the column to the first pile with
 * room, and starting a new pile when the column is full or there is no pile there yet.
 */
public final class ClayPiling
{
    public static InteractionResult place(UseOnContext context, ItemStack stack)
    {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        final BlockPos clicked = context.getClickedPos();
        final BlockState clickedState = level.getBlockState(clicked);
        final ClayPileBlock pileBlock = MFSBlocks.CLAY_PILE.get();

        if (clickedState.is(pileBlock))
        {
            // Walk up the column until we find a pile with room, or run off the top of it
            BlockPos target = clicked;
            BlockState targetState = clickedState;
            while (targetState.is(pileBlock) && targetState.getValue(ClayPileBlock.COUNT) >= ClayPileBlock.MAX_ITEMS)
            {
                target = target.above();
                targetState = level.getBlockState(target);
            }

            if (targetState.is(pileBlock))
            {
                return addToPile(level, target, targetState, player, stack);
            }
            return targetState.canBeReplaced()
                ? startPile(level, target, player, stack)
                : InteractionResult.FAIL;
        }

        // Not a pile - place one against the face that was clicked, if there is room for it
        final BlockPos target = new BlockPlaceContext(context).getClickedPos();
        final BlockState targetState = level.getBlockState(target);
        if (targetState.is(pileBlock))
        {
            return addToPile(level, target, targetState, player, stack);
        }
        return targetState.canBeReplaced()
            ? startPile(level, target, player, stack)
            : InteractionResult.PASS;
    }

    private static InteractionResult startPile(Level level, BlockPos pos, @Nullable Player player, ItemStack stack)
    {
        final BlockState state = MFSBlocks.CLAY_PILE.get().defaultBlockState();
        if (!state.canSurvive(level, pos))
        {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide)
        {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile)
            {
                pile.addItem(takeOne(player, stack));
            }
        }
        playPlaceSound(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult addToPile(Level level, BlockPos pos, BlockState state, @Nullable Player player, ItemStack stack)
    {
        final int count = state.getValue(ClayPileBlock.COUNT);
        if (count >= ClayPileBlock.MAX_ITEMS)
        {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide)
        {
            level.setBlock(pos, state.setValue(ClayPileBlock.COUNT, count + 1), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile)
            {
                pile.addItem(takeOne(player, stack));
            }
        }
        playPlaceSound(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Splits a single item off the held stack, leaving creative-mode stacks alone.
     */
    private static ItemStack takeOne(@Nullable Player player, ItemStack stack)
    {
        if (player != null && player.isCreative())
        {
            return stack.copyWithCount(1);
        }
        return stack.split(1);
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState state)
    {
        final SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
    }

    private ClayPiling() {}
}
