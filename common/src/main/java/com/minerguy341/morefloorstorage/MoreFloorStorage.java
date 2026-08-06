package com.minerguy341.morefloorstorage;

import com.minerguy341.morefloorstorage.common.MFSInteractions;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import com.minerguy341.morefloorstorage.common.blockentity.MFSBlockEntities;
import com.minerguy341.morefloorstorage.network.MFSNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A TerraFirmaCraft addon focused on getting more out of the ground you walk on:
 * <ul>
 *     <li>Clay piles, which apply TFC's ingot pile mechanic to clay-type items.</li>
 *     <li>Tools that lean against walls, in the style of Vintage Story.</li>
 *     <li>Sneak + TFC's floor storage key to pick items back up out of floor storage.</li>
 * </ul>
 */
@Mod(MoreFloorStorage.MOD_ID)
public final class MoreFloorStorage
{
    public static final String MOD_ID = "morefloorstorage";

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public MoreFloorStorage(ModContainer mod, IEventBus bus)
    {
        mod.registerConfig(ModConfig.Type.SERVER, MFSConfig.SERVER_SPEC);

        MFSBlocks.BLOCKS.register(bus);
        MFSBlockEntities.BLOCK_ENTITIES.register(bus);

        bus.addListener(MFSNetwork::register);

        NeoForge.EVENT_BUS.addListener(MFSInteractions::onUseItemOnBlock);
    }
}
