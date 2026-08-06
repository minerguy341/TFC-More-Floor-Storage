package com.minerguy341.morefloorstorage.common;

import com.minerguy341.morefloorstorage.MFSConfig;
import com.minerguy341.morefloorstorage.common.block.ClayPileBlock;
import com.minerguy341.morefloorstorage.common.block.LeaningToolBlock;
import com.minerguy341.morefloorstorage.compat.tfc.TFCFloorStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Sneak + TerraFirmaCraft's floor storage key takes things back out of floor storage, whichever kind of
 * floor storage is under the crosshair: our clay piles and leaning tools, TFC's placed items and shelves,
 * and TFC's ingot and double ingot piles.
 */
public final class FloorStoragePickup
{
    /**
     * @return {@code true} if something was picked up.
     */
    public static boolean pickup(ServerPlayer player)
    {
        if (!MFSConfig.SERVER.enableSneakPickup.get())
        {
            return false;
        }

        final HitResult ray = player.pick(MFSConfig.SERVER.interactionRange.get(), 1.0f, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        final Level level = player.level();
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = level.getBlockState(pos);
        final boolean whole = MFSConfig.SERVER.sneakPickupWholeStack.get();

        if (state.getBlock() instanceof LeaningToolBlock leaning)
        {
            return leaning.takeTool(level, pos, state, player, hit);
        }
        if (state.getBlock() instanceof ClayPileBlock pile)
        {
            return pile.removeFromTop(level, pos, player, whole);
        }
        return TFCFloorStorage.tryPickup(level, pos, state, player, hit, whole);
    }

    private FloorStoragePickup() {}
}
