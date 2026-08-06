package com.minerguy341.morefloorstorage.client;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.common.blockentity.MFSBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MoreFloorStorage.MOD_ID, dist = Dist.CLIENT)
public final class MFSClient
{
    public MFSClient(ModContainer mod, IEventBus bus)
    {
        bus.addListener(MFSClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(MFSKeyHandler::onKeyInput);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(MFSBlockEntities.CLAY_PILE.get(), context -> new PileRenderer());
        event.registerBlockEntityRenderer(MFSBlockEntities.ORE_PILE.get(), context -> new PileRenderer());
        event.registerBlockEntityRenderer(MFSBlockEntities.LEANING_TOOL.get(), context -> new LeaningToolRenderer());
    }
}
