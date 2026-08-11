package com.minerguy341.tfcmorefloorstorage;

import net.dries007.tfc.util.BlockItemPlacement;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.InteractionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shift-click clay balls to create / fill {@link ClayPileBlock}s (mirrors TFC ingot piling).
 */
public final class ClayPiling
{
    private ClayPiling() {}

    public static void register()
    {
        final BlockItemPlacement placement = new BlockItemPlacement(() -> Items.AIR, ModBlocks.CLAY_PILE);
        InteractionManager.register(Ingredient.of(Items.CLAY_BALL), false, (stack, context) -> doPiling(placement, stack, context));
    }

    private static InteractionResult doPiling(BlockItemPlacement placement, ItemStack stack, UseOnContext context)
    {
        final Player player = context.getPlayer();
        if (player == null || !player.mayBuild() || !player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }

        final Level level = context.getLevel();
        final BlockPos posClicked = context.getClickedPos();
        final BlockState stateClicked = level.getBlockState(posClicked);
        final ClayPileBlock pileBlock = ModBlocks.CLAY_PILE.get();

        if (stateClicked.is(pileBlock))
        {
            final int current = stateClicked.getValue(ClayPileBlock.COUNT);
            if (current < ClayPileBlock.MAX_COUNT)
            {
                final ItemStack insert = stack.split(1);
                Helpers.playPlaceSound(level, posClicked, stateClicked);
                level.setBlock(posClicked, stateClicked.setValue(ClayPileBlock.COUNT, current + 1), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(posClicked) instanceof ClayPileBlockEntity pile)
                {
                    pile.addClay(insert);
                }
                return InteractionResult.SUCCESS;
            }

            BlockPos topPos = posClicked;
            BlockState topState;
            do
            {
                topPos = topPos.above();
                topState = level.getBlockState(topPos);
            } while (topState.is(pileBlock) && topState.getValue(ClayPileBlock.COUNT) == ClayPileBlock.MAX_COUNT);

            if (topState.is(pileBlock))
            {
                final ItemStack insert = stack.split(1);
                final int topCount = topState.getValue(ClayPileBlock.COUNT);
                Helpers.playPlaceSound(level, topPos, topState);
                level.setBlock(topPos, topState.setValue(ClayPileBlock.COUNT, topCount + 1), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(topPos) instanceof ClayPileBlockEntity pile)
                {
                    pile.addClay(insert);
                }
                return InteractionResult.SUCCESS;
            }
            else if (topState.isAir())
            {
                final ItemStack before = stack.copy();
                final BlockPos belowTop = topPos.below();
                final UseOnContext topContext = new UseOnContext(player, context.getHand(), new BlockHitResult(Vec3.ZERO, Direction.UP, belowTop, false));
                final InteractionResult result = placement.onItemUse(stack, topContext);
                if (result.consumesAction())
                {
                    before.setCount(1);
                    if (level.getBlockEntity(topPos) instanceof ClayPileBlockEntity pile)
                    {
                        pile.addClay(before);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        final ItemStack before = stack.copyWithCount(1);
        final BlockPos placedPos = new BlockPlaceContext(context).getClickedPos();
        final InteractionResult result = placement.onItemUse(stack, context);
        if (result.consumesAction() && level.getBlockEntity(placedPos) instanceof ClayPileBlockEntity pile)
        {
            pile.addClay(before);
        }
        return result;
    }
}
