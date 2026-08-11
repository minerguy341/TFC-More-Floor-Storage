package com.minerguy341.tfcmorefloorstorage;

import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.dries007.tfc.common.capabilities.size.Size;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Vintage Story style tool leaning, with TFC placed-item size capacity rules.
 */
public final class ToolLeaning
{
    public static boolean tryLean(ServerPlayer player)
    {
        if (!ToolLeaningConfig.ENABLE_TOOL_LEANING.get() || !player.mayBuild())
        {
            return false;
        }

        final ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.is(ModTags.LEANABLE))
        {
            return false;
        }
        if (!isLeanableSize(held))
        {
            return false;
        }

        final HitResult ray = player.pick(ToolLeaningConfig.INTERACTION_RANGE.get().floatValue(), 1.0f, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        final Level level = player.level();
        final BlockPos hitPos = hit.getBlockPos();
        final BlockState hitState = level.getBlockState(hitPos);
        final LeaningToolBlock leaningBlock = ModBlocks.LEANING_TOOL.get();

        if (hitState.is(leaningBlock))
        {
            return insert(level, hitPos, player, held, LeaningToolBlock.slotFromHit(hitState, hitPos, hit.getLocation()));
        }

        final Direction face = hit.getDirection();
        if (face.getAxis().isVertical() || !hitState.isFaceSturdy(level, hitPos, face))
        {
            return false;
        }

        final Direction facing = face.getOpposite();
        final BlockPos target = hitPos.relative(face);

        // Fill an existing lean on this wall cell (or an immediate neighbour along the wall)
        // up to capacity before starting a new block — avoids one-tool-per-column by accident.
        if (insertIntoExisting(level, target, facing, player, held, hit.getLocation()))
        {
            return true;
        }

        final BlockState targetState = level.getBlockState(target);
        if (!targetState.canBeReplaced())
        {
            return false;
        }

        final BlockState newState = leaningBlock.defaultBlockState().setValue(LeaningToolBlock.FACING, facing);
        if (!newState.canSurvive(level, target))
        {
            return false;
        }

        level.setBlock(target, newState, Block.UPDATE_ALL);
        if (!insert(level, target, player, held, LeaningToolBlock.slotFromHit(newState, target, hit.getLocation())))
        {
            level.removeBlock(target, false);
            return false;
        }
        return true;
    }

    /**
     * Tries the aimed cell first, then left/right along the wall, so repeated leans against the
     * same stretch of wall stack into one block until it is full.
     */
    private static boolean insertIntoExisting(Level level, BlockPos target, Direction facing, Player player, ItemStack held, net.minecraft.world.phys.Vec3 hitLocation)
    {
        final Direction along = facing.getClockWise();
        final BlockPos[] candidates = {
            target,
            target.relative(along),
            target.relative(along.getOpposite())
        };

        for (final BlockPos pos : candidates)
        {
            final BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.LEANING_TOOL.get()) || state.getValue(LeaningToolBlock.FACING) != facing)
            {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning) || !leaning.canAccept(held))
            {
                continue;
            }
            return insert(level, pos, player, held, LeaningToolBlock.slotFromHit(state, pos, hitLocation));
        }
        return false;
    }

    /**
     * Same size gate as TFC ground placed items ({@code maxPlacedItemSize} / {@code maxPlacedLargeItemSize}).
     */
    public static boolean isLeanableSize(ItemStack stack)
    {
        final Size size = ItemSizeManager.get(stack).getSize(stack);
        return size.isEqualOrSmallerThan(TFCConfig.SERVER.maxPlacedLargeItemSize.get());
    }

    private static boolean insert(Level level, BlockPos pos, Player player, ItemStack held, int preferredSlot)
    {
        if (!(level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning))
        {
            return false;
        }
        if (!leaning.canAccept(held))
        {
            return false;
        }

        final ItemStack tool = player.isCreative() ? held.copyWithCount(1) : held.split(1);
        if (!leaning.tryInsert(preferredSlot, tool))
        {
            if (!player.isCreative())
            {
                player.getInventory().placeItemBackInInventory(tool);
            }
            return false;
        }

        final SoundType sound = level.getBlockState(pos).getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
        return true;
    }

    private ToolLeaning() {}
}
