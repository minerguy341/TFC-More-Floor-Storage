package com.minerguy341.tfcmorefloorstorage.mixin;

import java.util.List;

import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IngotPileBlockEntity.class)
public interface IngotPileBlockEntityAccessor
{
    @Accessor(value = "entries", remap = false)
    List<Object> tfcmfs$getEntries();
}
