package com.minerguy341.morefloorstorage.compat.tfc;

import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blockentities.PlacedItemBlockEntity;
import net.dries007.tfc.common.blocks.devices.IngotPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The bits of the pickup mechanic that reach into TerraFirmaCraft's own floor storage. Kept in one place
 * so that everything touching TFC internals is easy to find when porting to a new TFC version.
 */
public final class TFCFloorStorage
{
    public static boolean tryPickup(Level level, BlockPos pos, BlockState state, ServerPlayer player, BlockHitResult hit, boolean whole)
    {
        final BlockEntity entity = level.getBlockEntity(pos);

        // Placed items, and shelves, which reuse the same block entity. Passing an empty stack while the
        // player is sneaking is exactly the case TFC treats as "take the item in this slot".
        if (entity instanceof PlacedItemBlockEntity placedItem)
        {
            return placedItem.onRightClick(player, ItemStack.EMPTY, hit);
        }

        // Ingot piles, and double ingot piles, which subclass them with a wider count property
        if (state.getBlock() instanceof IngotPileBlock pileBlock && entity instanceof IngotPileBlockEntity)
        {
            return removeIngots(level, pos, pileBlock, player, whole);
        }

        return false;
    }

    private static boolean removeIngots(Level level, BlockPos pos, IngotPileBlock pileBlock, ServerPlayer player, boolean whole)
    {
        // As in TFC, ingots always come off the top of the column
        BlockPos topPos = pos;
        while (level.getBlockState(topPos.above()).is(pileBlock))
        {
            topPos = topPos.above();
        }

        final BlockState topState = level.getBlockState(topPos);
        if (!(level.getBlockEntity(topPos) instanceof IngotPileBlockEntity pile))
        {
            return false;
        }

        final IntegerProperty countProperty = pileBlock.getCountProperty();
        final int count = topState.getValue(countProperty);
        final int taken = whole ? count : 1;

        for (int i = 0; i < taken; i++)
        {
            final ItemStack ingot = pile.removeIngot();
            if (!ingot.isEmpty() && !player.isCreative())
            {
                ItemHandlerHelper.giveItemToPlayer(player, ingot);
            }
        }

        if (count - taken <= 0)
        {
            level.removeBlock(topPos, false);
        }
        else
        {
            level.setBlock(topPos, topState.setValue(countProperty, count - taken), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private TFCFloorStorage() {}
}
