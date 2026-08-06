package com.minerguy341.morefloorstorage.common;

import com.minerguy341.morefloorstorage.MFSConfig;
import com.minerguy341.morefloorstorage.common.block.LeaningToolBlock;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import com.minerguy341.morefloorstorage.common.blockentity.LeaningToolBlockEntity;
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
 * Vintage Story style tool leaning. Aiming at the side of a solid block and pressing TerraFirmaCraft's
 * floor storage key stands the held tool up against that wall.
 */
public final class ToolLeaning
{
    /**
     * Server-authoritative entry point: everything is re-derived from the player's own look vector, so a
     * client can only ask to lean a tool, never dictate where it ends up.
     *
     * @return {@code true} if a tool was leaned.
     */
    public static boolean tryLean(ServerPlayer player)
    {
        if (!MFSConfig.SERVER.enableToolLeaning.get() || !player.mayBuild())
        {
            return false;
        }

        final ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.is(MFSTags.LEANABLE))
        {
            return false;
        }

        final HitResult ray = player.pick(MFSConfig.SERVER.interactionRange.get(), 1.0f, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        final Level level = player.level();
        final BlockPos hitPos = hit.getBlockPos();
        final BlockState hitState = level.getBlockState(hitPos);
        final LeaningToolBlock leaningBlock = MFSBlocks.LEANING_TOOL.get();

        // Aiming at tools that are already leaning there - slot another one in beside them
        if (hitState.is(leaningBlock))
        {
            return insert(level, hitPos, player, held, LeaningToolBlock.slotFromHit(hitState, hitPos, hit.getLocation()));
        }

        final Direction face = hit.getDirection();
        if (face.getAxis().isVertical() || !hitState.isFaceSturdy(level, hitPos, face))
        {
            return false; // Nothing to lean against
        }

        final BlockPos target = hitPos.relative(face);
        final BlockState targetState = level.getBlockState(target);
        if (targetState.is(leaningBlock))
        {
            return insert(level, target, player, held, LeaningToolBlock.slotFromHit(targetState, target, hit.getLocation()));
        }
        if (!targetState.canBeReplaced())
        {
            return false;
        }

        final BlockState newState = leaningBlock.defaultBlockState().setValue(LeaningToolBlock.FACING, face.getOpposite());
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

    private static boolean insert(Level level, BlockPos pos, Player player, ItemStack held, int preferredSlot)
    {
        if (!(level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning))
        {
            return false;
        }

        int slot = preferredSlot;
        if (!leaning.getTool(slot).isEmpty())
        {
            slot = leaning.firstFreeSlot();
        }
        if (slot == -1)
        {
            return false;
        }

        final ItemStack tool = player.isCreative() ? held.copyWithCount(1) : held.split(1);
        if (!leaning.setTool(slot, tool))
        {
            // Should be unreachable, but never eat the player's tool if it somehow is not
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
