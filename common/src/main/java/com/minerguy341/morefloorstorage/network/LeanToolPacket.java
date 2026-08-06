package com.minerguy341.morefloorstorage.network;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "The player pressed the floor storage key while aiming at a wall." As with
 * {@link PickupFloorStoragePacket}, the server works out the target itself.
 */
public enum LeanToolPacket implements CustomPacketPayload
{
    INSTANCE;

    public static final CustomPacketPayload.Type<LeanToolPacket> TYPE =
        new CustomPacketPayload.Type<>(MoreFloorStorage.id("lean_tool"));

    public static final StreamCodec<ByteBuf, LeanToolPacket> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
