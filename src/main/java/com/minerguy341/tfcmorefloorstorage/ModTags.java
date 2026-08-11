package com.minerguy341.tfcmorefloorstorage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags
{
    public static final TagKey<Item> PILEABLE_CLAYS = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "pileable_clays")
    );

    private ModTags() {}
}
