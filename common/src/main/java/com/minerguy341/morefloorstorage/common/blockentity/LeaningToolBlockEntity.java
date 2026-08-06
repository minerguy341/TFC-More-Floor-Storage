package com.minerguy341.morefloorstorage.common.blockentity;

import java.util.function.Consumer;

import com.minerguy341.morefloorstorage.common.block.LeaningToolBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds up to {@link LeaningToolBlock#SLOTS} tools leaned against one wall, indexed left to right as
 * seen from the room side.
 */
public class LeaningToolBlockEntity extends SyncedBlockEntity
{
    private final NonNullList<ItemStack> tools = NonNullList.withSize(LeaningToolBlock.SLOTS, ItemStack.EMPTY);

    public LeaningToolBlockEntity(BlockPos pos, BlockState state)
    {
        super(MFSBlockEntities.LEANING_TOOL.get(), pos, state);
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
     * @return the index of the first slot with nothing in it, or {@code -1} if the block is full.
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
     * Removes the tool in {@code slot}, falling back to the last occupied slot if that one is empty,
     * so that a slightly-off click still does what the player meant.
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        ContainerHelper.saveAllItems(tag, tools, true, registries);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        // NonNullList.withSize is fixed-size, so reset by assignment rather than clear()/add()
        for (int slot = 0; slot < tools.size(); slot++)
        {
            tools.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, tools, registries);
        super.loadAdditional(tag, registries);
    }
}
