package com.minerguy341.tfcmorefloorstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ClayPileBlockEntity extends TFCBlockEntity
{
    private final List<ItemStack> stacks = new ArrayList<>();

    public ClayPileBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.CLAY_PILE.get(), pos, state);
    }

    public void addClay(ItemStack stack)
    {
        stacks.add(stack);
        markForSync();
    }

    public ItemStack removeClay()
    {
        if (!stacks.isEmpty())
        {
            final ItemStack stack = stacks.remove(stacks.size() - 1);
            markForSync();
            return stack;
        }
        return ItemStack.EMPTY;
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

    public ItemStack stackAt(int index)
    {
        if (index < 0 || index >= stacks.size())
        {
            return ItemStack.EMPTY;
        }
        return stacks.get(index);
    }

    public ItemStack getPickedItemStack()
    {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        final ListTag list = new ListTag();
        for (ItemStack stack : stacks)
        {
            list.add(stack.save(new CompoundTag()));
        }
        tag.put("stacks", list);
        super.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag)
    {
        stacks.clear();
        final ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            stacks.add(ItemStack.of(list.getCompound(i)));
        }
        super.loadAdditional(tag);
    }
}
