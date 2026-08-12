package com.minerguy341.tfcmorefloorstorage;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dev/test commands. {@code /tfcmfs lean} leans the held tool at the aimed wall face
 * (same server path as the V key), useful when synthetic key events are unreliable.
 */
@Mod.EventBusSubscriber(modid = TFCMoreFloorStorage.MOD_ID)
public final class ModCommands
{
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("tfcmfs")
                .then(Commands.literal("lean")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        final ServerPlayer player = ctx.getSource().getPlayerOrException();
                        final boolean ok = ToolLeaning.tryLean(player);
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(ok ? "Leaned tool against wall." : "Could not lean (aim at a sturdy vertical face)."),
                            true
                        );
                        return ok ? 1 : 0;
                    }))
        );
    }

    private ModCommands() {}
}
