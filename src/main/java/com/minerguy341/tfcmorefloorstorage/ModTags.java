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

    /** Items that can be leaned against a wall (defaults to TFC tool-rack tools). */
    public static final TagKey<Item> LEANABLE = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "leanable")
    );

    /**
     * Flat sprites that need a 90° clockwise turn after leaning (knives, chisels, tuyeres, saws).
     */
    public static final TagKey<Item> LEAN_FLIP_FACING = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "lean_flip_facing")
    );

    /**
     * Longer sprites that need a room-side wall offset (saws, chisels, swords, maces, rods, etc.).
     */
    public static final TagKey<Item> LEAN_CLEAR_WALL = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "lean_clear_wall")
    );

    /** Knives and tuyeres: same clear-wall group, seated 0.05 closer to the wall. */
    public static final TagKey<Item> LEAN_CLOSER_WALL = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "lean_closer_wall")
    );

    private ModTags() {}
}
