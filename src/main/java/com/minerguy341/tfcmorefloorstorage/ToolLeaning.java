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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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

        // Collision clip (not outline): leaning tools have no collision, so the ray reaches the
        // wall behind them. Outline pick was eating adjacent-column aims into the existing lean.
        final BlockHitResult hit = clipThroughLeans(player, ToolLeaningConfig.INTERACTION_RANGE.get().floatValue());
        if (hit.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        final Level level = player.level();
        final BlockPos hitPos = hit.getBlockPos();
        final BlockState hitState = level.getBlockState(hitPos);
        final LeaningToolBlock leaningBlock = ModBlocks.LEANING_TOOL.get();

        final Direction face = hit.getDirection();
        if (face.getAxis().isVertical() || !hitState.isFaceSturdy(level, hitPos, face))
        {
            return false;
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

    /**
     * Same size gate as TFC ground placed items ({@code maxPlacedItemSize} / {@code maxPlacedLargeItemSize}).
     */
    public static boolean isLeanableSize(ItemStack stack)
    {
        final Size size = ItemSizeManager.get(stack).getSize(stack);
        return size.isEqualOrSmallerThan(TFCConfig.SERVER.maxPlacedLargeItemSize.get());
    }

    /**
     * Block pick that ignores leaning-tool outlines so placement follows the wall face you aim at.
     */
    private static BlockHitResult clipThroughLeans(Player player, float range)
    {
        final Vec3 eye = player.getEyePosition(1.0f);
        final Vec3 look = player.getViewVector(1.0f);
        final Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
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
