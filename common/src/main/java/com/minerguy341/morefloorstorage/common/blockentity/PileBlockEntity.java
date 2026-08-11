package com.minerguy341.morefloorstorage.common.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.minerguy341.morefloorstorage.common.block.PileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the individual items making up a pile. Every entry is a stack of exactly one, in the order it
 * was placed, so that the top of the pile is the last thing added - the same contract TerraFirmaCraft's
 * ingot piles use.
 * <p>
 * Shared by every kind of pile; which items are allowed in is decided at placement time by
 * {@link com.minerguy341.morefloorstorage.common.MFSInteractions}, not here.
 */
public class PileBlockEntity extends SyncedBlockEntity
{
    private final List<ItemStack> stacks = new ArrayList<>();

    public PileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    /**
     * @return {@code true} if the item was accepted into the pile.
     */
    public boolean addItem(ItemStack stack)
    {
        // The block decides how much fits where it stands; this is only the backstop
        if (stack.isEmpty() || stacks.size() >= PileBlock.MAX_ITEMS)
        {
            return false;
        }
        stacks.add(stack.copyWithCount(1));
        markForSync();
        return true;
    }

    /**
     * Removes and returns the item at the top of the pile, or an empty stack if there is none.
     */
    public ItemStack removeTop()
    {
        if (stacks.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = stacks.remove(stacks.size() - 1);
        markForSync();
        return stack;
    }

    public void removeAll(Consumer<ItemStack> consumer)
    {
        for (ItemStack stack : stacks)
        {
            consumer.accept(stack);
        }
        stacks.clear();
        markForSync();
    }

    public int size()
    {
        return stacks.size();
    }

    public List<ItemStack> getStacks()
    {
        return Collections.unmodifiableList(stacks);
    }

    public ItemStack getPickedItemStack()
    {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(stacks.size() - 1).copy();
    }

    /**
     * Summarises the pile as "{@code 12x Clay}" lines, one per distinct item, for waila-style tooltips.
     */
    public void fillTooltip(Consumer<Component> tooltip)
    {
        final Map<Item, Integer> counts = new LinkedHashMap<>(); // Deterministic iteration order
        countInto(counts);
        describe(counts, tooltip);
    }

    /**
     * Tallies this pile's contents into {@code counts}, so the four blocks of a merged group can be
     * summarised as the one pile they are rather than as four separate lists.
     */
    public void countInto(Map<Item, Integer> counts)
    {
        for (ItemStack stack : stacks)
        {
            counts.merge(stack.getItem(), 1, Integer::sum);
        }
    }

    /**
     * Turns a tally from {@link #countInto} into one "{@code 12x Clay}" line per distinct item.
     */
    public static void describe(Map<Item, Integer> counts, Consumer<Component> tooltip)
    {
        counts.forEach((item, count) -> tooltip.accept(
            Component.literal(count + "x ").append(item.getDescription())));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        final ListTag list = new ListTag();
        for (ItemStack stack : stacks)
        {
            list.add(stack.save(registries));
        }
        tag.put("stacks", list);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        stacks.clear();
        final ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            final ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty())
            {
                stacks.add(stack);
            }
        }
        super.loadAdditional(tag, registries);
    }
}
