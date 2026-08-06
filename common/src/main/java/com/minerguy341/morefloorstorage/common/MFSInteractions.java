package com.minerguy341.morefloorstorage.common;

import com.minerguy341.morefloorstorage.MFSConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

public final class MFSInteractions
{
    /**
     * Sneak-clicking a block with a clay-type item piles it up. This runs in the {@code ITEM_AFTER_BLOCK}
     * phase, the same one TerraFirmaCraft uses for its own item interactions, so a block that wants the
     * click for itself still gets first refusal.
     */
    public static void onUseItemOnBlock(UseItemOnBlockEvent event)
    {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
            || !MFSConfig.SERVER.enableClayPiles.get())
        {
            return;
        }

        final UseOnContext context = event.getUseOnContext();
        final @Nullable Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || !player.mayBuild())
        {
            return;
        }

        final ItemStack stack = context.getItemInHand();
        if (stack.isEmpty() || !stack.is(MFSTags.CLAY_PILE_ITEMS))
        {
            return;
        }

        final InteractionResult result = ClayPiling.place(context, stack);
        switch (result)
        {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> event.cancelWithResult(ItemInteractionResult.SUCCESS);
            case CONSUME, CONSUME_PARTIAL -> event.cancelWithResult(ItemInteractionResult.CONSUME);
            case FAIL -> event.cancelWithResult(ItemInteractionResult.FAIL);
            case PASS -> {}
        }
    }

    private MFSInteractions() {}
}
