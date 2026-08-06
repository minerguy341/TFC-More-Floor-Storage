package com.minerguy341.morefloorstorage.network;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "The player pressed sneak + the floor storage key." Carries nothing: the server re-traces the player's
 * look vector itself rather than trusting a client-supplied position.
 */
public enum PickupFloorStoragePacket implements CustomPacketPayload
{
    INSTANCE;

    public static final CustomPacketPayload.Type<PickupFloorStoragePacket> TYPE =
        new CustomPacketPayload.Type<>(MoreFloorStorage.id("pickup_floor_storage"));

    public static final StreamCodec<ByteBuf, PickupFloorStoragePacket> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
