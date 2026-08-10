package com.minerguy341.tfcmorefloorstorage.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.dries007.tfc.common.blockentities.IngotPileBlockEntity$Entry")
public interface IngotPileEntryAccessor
{
    @Accessor(value = "stack", remap = false)
    ItemStack tfcmfs$getStack();
}
