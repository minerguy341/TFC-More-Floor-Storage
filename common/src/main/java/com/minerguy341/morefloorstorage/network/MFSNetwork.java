package com.minerguy341.morefloorstorage.network;

import com.minerguy341.morefloorstorage.common.FloorStoragePickup;
import com.minerguy341.morefloorstorage.common.ToolLeaning;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MFSNetwork
{
    public static void register(RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PickupFloorStoragePacket.TYPE, PickupFloorStoragePacket.CODEC, MFSNetwork::onPickup);
        registrar.playToServer(LeanToolPacket.TYPE, LeanToolPacket.CODEC, MFSNetwork::onLean);
    }

    private static void onPickup(PickupFloorStoragePacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
            {
                FloorStoragePickup.pickup(player);
            }
        });
    }

    private static void onLean(LeanToolPacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
            {
                ToolLeaning.tryLean(player);
            }
        });
    }

    private MFSNetwork() {}
}
