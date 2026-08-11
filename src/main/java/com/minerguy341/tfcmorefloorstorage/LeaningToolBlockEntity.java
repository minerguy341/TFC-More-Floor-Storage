package com.minerguy341.tfcmorefloorstorage;

import java.util.function.Consumer;

import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.dries007.tfc.common.capabilities.size.Size;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds leaned tools against one wall. Capacity follows TFC placed-item size rules:
 * items ≤ {@code maxPlacedItemSize} share up to {@link LeaningToolBlock#SLOTS} slots;
 * larger items (up to {@code maxPlacedLargeItemSize}) occupy the block alone.
 */
public class LeaningToolBlockEntity extends TFCBlockEntity
{
    public static final int SLOT_LARGE = 0;

    private final NonNullList<ItemStack> tools = NonNullList.withSize(LeaningToolBlock.SLOTS, ItemStack.EMPTY);
    private boolean holdingLargeItem;

    public LeaningToolBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.LEANING_TOOL.get(), pos, state);
        this.holdingLargeItem = false;
    }

    public boolean isHoldingLargeItem()
    {
        return holdingLargeItem;
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

    /**
     * Whether this stack may be leaned here given TFC size config and current contents.
     */
    public boolean canAccept(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        final Size size = ItemSizeManager.get(stack).getSize(stack);
        final Size maxSmall = TFCConfig.SERVER.maxPlacedItemSize.get();
        final Size maxLarge = TFCConfig.SERVER.maxPlacedLargeItemSize.get();

        if (size.isEqualOrSmallerThan(maxSmall))
        {
            return !holdingLargeItem && firstFreeSlot() != -1;
        }
        if (!size.isEqualOrSmallerThan(maxSmall) && size.isEqualOrSmallerThan(maxLarge))
        {
            return isEmpty();
        }
        return false; // bigger than maxPlacedLargeItemSize
    }

    /**
     * Inserts into {@code preferredSlot} if free, otherwise the first free slot.
     * Large-sized items always use the centre (slot 0) and claim the whole block.
     *
     * @return {@code true} if inserted
     */
    public boolean tryInsert(int preferredSlot, ItemStack stack)
    {
        if (!canAccept(stack))
        {
            return false;
        }

        final Size size = ItemSizeManager.get(stack).getSize(stack);
        final Size maxSmall = TFCConfig.SERVER.maxPlacedItemSize.get();

        if (size.isEqualOrSmallerThan(maxSmall))
        {
            int slot = preferredSlot;
            if (slot < 0 || slot >= tools.size() || !tools.get(slot).isEmpty())
            {
                slot = firstFreeSlot();
            }
            if (slot == -1)
            {
                return false;
            }
            tools.set(slot, stack);
            markForSync();
            return true;
        }

        // Large: sole occupant in slot 0
        tools.set(SLOT_LARGE, stack);
        holdingLargeItem = true;
        markForSync();
        return true;
    }

    public int firstFreeSlot()
    {
        if (holdingLargeItem)
        {
            return -1;
        }
        for (int slot = 0; slot < tools.size(); slot++)
        {
            if (tools.get(slot).isEmpty())
            {
                return slot;
            }
        }
        return -1;
    }

    public ItemStack removeTool(int slot)
    {
        if (holdingLargeItem)
        {
            slot = SLOT_LARGE;
        }
        else if (slot < 0 || slot >= tools.size() || tools.get(slot).isEmpty())
        {
            slot = lastOccupiedSlot();
        }
        if (slot == -1)
        {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = tools.get(slot);
        tools.set(slot, ItemStack.EMPTY);
        holdingLargeItem = false;
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
        holdingLargeItem = false;
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
        tag.putBoolean("holdingLargeItem", holdingLargeItem);
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
        holdingLargeItem = tag.getBoolean("holdingLargeItem");
        // Recover flag if older data / desync
        if (!holdingLargeItem)
        {
            int filled = 0;
            for (ItemStack stack : tools)
            {
                if (!stack.isEmpty())
                {
                    filled++;
                }
            }
            if (filled == 1 && !tools.get(SLOT_LARGE).isEmpty())
            {
                final Size size = ItemSizeManager.get(tools.get(SLOT_LARGE)).getSize(tools.get(SLOT_LARGE));
                if (!size.isEqualOrSmallerThan(TFCConfig.SERVER.maxPlacedItemSize.get()))
                {
                    holdingLargeItem = true;
                }
            }
        }
        super.loadAdditional(tag);
    }
}
