package com.minerguy341.tfcmorefloorstorage;

import java.util.function.Consumer;

import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds up to {@link LeaningToolBlock#SLOTS} tools leaned against one wall.
 * Ported from Claude's {@code LeaningToolBlockEntity}.
 */
public class LeaningToolBlockEntity extends TFCBlockEntity
{
    private final NonNullList<ItemStack> tools = NonNullList.withSize(LeaningToolBlock.SLOTS, ItemStack.EMPTY);

    public LeaningToolBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.LEANING_TOOL.get(), pos, state);
    }

    public ItemStack getTool(int slot)
    {
        return slot >= 0 && slot < tools.size() ? tools.get(slot) : ItemStack.EMPTY;
    }

    public boolean isEmpty()
    {
        for (ItemStack stack : tools)
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    public boolean setTool(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= tools.size() || !tools.get(slot).isEmpty())
        {
            return false;
        }
        tools.set(slot, stack);
        markForSync();
        return true;
    }

    /**
     * @return first free slot index, or {@code -1} if full.
     */
    public int firstFreeSlot()
    {
        for (int slot = 0; slot < tools.size(); slot++)
        {
            if (tools.get(slot).isEmpty())
            {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Removes the tool in {@code slot}, falling back to the last occupied slot if empty.
     */
    public ItemStack removeTool(int slot)
    {
        if (slot < 0 || slot >= tools.size() || tools.get(slot).isEmpty())
        {
            slot = lastOccupiedSlot();
        }
        if (slot == -1)
        {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = tools.get(slot);
        tools.set(slot, ItemStack.EMPTY);
        markForSync();
        return stack;
    }

    public void removeAll(Consumer<ItemStack> consumer)
    {
        for (int slot = 0; slot < tools.size(); slot++)
        {
            final ItemStack stack = tools.get(slot);
            if (!stack.isEmpty())
            {
                consumer.accept(stack);
                tools.set(slot, ItemStack.EMPTY);
            }
        }
        markForSync();
    }

    private int lastOccupiedSlot()
    {
        for (int slot = tools.size() - 1; slot >= 0; slot--)
        {
            if (!tools.get(slot).isEmpty())
            {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        ContainerHelper.saveAllItems(tag, tools, true);
        super.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag)
    {
        for (int slot = 0; slot < tools.size(); slot++)
        {
            tools.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, tools);
        super.loadAdditional(tag);
    }
}
