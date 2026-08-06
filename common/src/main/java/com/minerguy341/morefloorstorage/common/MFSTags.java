package com.minerguy341.morefloorstorage.common;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MFSTags
{
    /**
     * Items that can be stacked into a clay pile. Defaults to TFC's clay and fire clay knapping tags,
     * which is exactly the set of "clay types" in a TFC world, plus anything a pack chooses to add.
     */
    public static final TagKey<Item> CLAY_PILE_ITEMS = item("clay_pile_items");

    /**
     * Items that can be stacked into an ore pile. Defaults to TFC's small ore pieces - the native
     * deposits scattered on the ground - and its poor, normal and rich graded ores.
     */
    public static final TagKey<Item> ORE_PILE_ITEMS = item("ore_pile_items");

    /**
     * Items that can be leaned against the side of a block. Defaults to the common tool tags.
     */
    public static final TagKey<Item> LEANABLE = item("leanable");

    private static TagKey<Item> item(String path)
    {
        return TagKey.create(Registries.ITEM, MoreFloorStorage.id(path));
    }

    private MFSTags() {}
}
