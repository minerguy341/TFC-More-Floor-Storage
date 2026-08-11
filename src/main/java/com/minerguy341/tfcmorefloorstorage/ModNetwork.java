package com.minerguy341.tfcmorefloorstorage;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge SimpleChannel networking for tool leaning.
 */
public final class ModNetwork
{
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TFCMoreFloorStorage.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private static int nextId = 0;

    public static void register()
    {
        CHANNEL.registerMessage(
            nextId++,
            LeanToolPacket.class,
            LeanToolPacket::encode,
            LeanToolPacket::decode,
            LeanToolPacket::handle
        );
    }

    public static void sendLeanToServer()
    {
        CHANNEL.sendToServer(LeanToolPacket.INSTANCE);
    }

    private ModNetwork() {}

    /**
     * Empty "player pressed lean key" packet; server re-derives the target from the player's look.
     */
    public static final class LeanToolPacket
    {
        public static final LeanToolPacket INSTANCE = new LeanToolPacket();

        private LeanToolPacket() {}

        public static void encode(LeanToolPacket packet, FriendlyByteBuf buf) {}

        public static LeanToolPacket decode(FriendlyByteBuf buf)
        {
            return INSTANCE;
        }

        public static void handle(LeanToolPacket packet, Supplier<NetworkEvent.Context> ctxSupplier)
        {
            final NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                final ServerPlayer player = ctx.getSender();
                if (player != null)
                {
                    ToolLeaning.tryLean(player);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
