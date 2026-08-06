package com.minerguy341.morefloorstorage.common;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.minerguy341.morefloorstorage.MFSConfig;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import com.minerguy341.morefloorstorage.common.block.PileBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

public final class MFSInteractions
{
    /**
     * The kinds of pile there are, and what each one accepts. First match wins, so an item in two tags
     * lands in whichever pile is listed first.
     */
    private static final List<PileKind> PILE_KINDS = List.of(
        new PileKind(MFSTags.CLAY_PILE_ITEMS, MFSBlocks.CLAY_PILE, () -> MFSConfig.SERVER.enableClayPiles.get()),
        new PileKind(MFSTags.ORE_PILE_ITEMS, MFSBlocks.ORE_PILE, () -> MFSConfig.SERVER.enableOrePiles.get()));

    /**
     * Sneak-clicking a block with a pile-able item stacks it up. This runs in the {@code ITEM_AFTER_BLOCK}
     * phase, the same one TerraFirmaCraft uses for its own item interactions, so a block that wants the
     * click for itself still gets first refusal.
     */
    public static void onUseItemOnBlock(UseItemOnBlockEvent event)
    {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK)
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
        if (stack.isEmpty())
        {
            return;
        }

        final @Nullable PileKind kind = kindFor(stack);
        if (kind == null)
        {
            return;
        }

        switch (Piling.place(context, stack, kind.block().get()))
        {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> event.cancelWithResult(ItemInteractionResult.SUCCESS);
            case CONSUME, CONSUME_PARTIAL -> event.cancelWithResult(ItemInteractionResult.CONSUME);
            case FAIL -> event.cancelWithResult(ItemInteractionResult.FAIL);
            case PASS -> {}
        }
    }

    private static @Nullable PileKind kindFor(ItemStack stack)
    {
        for (PileKind kind : PILE_KINDS)
        {
            if (kind.enabled().getAsBoolean() && stack.is(kind.items()))
            {
                return kind;
            }
        }
        return null;
    }

    private record PileKind(TagKey<Item> items, Supplier<PileBlock> block, BooleanSupplier enabled) {}

    private MFSInteractions() {}
}
