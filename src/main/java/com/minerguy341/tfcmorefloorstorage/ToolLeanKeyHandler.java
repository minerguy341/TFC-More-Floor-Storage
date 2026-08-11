package com.minerguy341.tfcmorefloorstorage;

import com.mojang.blaze3d.platform.InputConstants;
import net.dries007.tfc.client.TFCKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Presses TFC's floor storage key (V by default) while holding a leanable tool to lean it on a wall.
 * Ported from Claude's {@code MFSKeyHandler} lean path (pickup path not included here).
 */
@Mod.EventBusSubscriber(modid = TFCMoreFloorStorage.MOD_ID, value = Dist.CLIENT)
public final class ToolLeanKeyHandler
{
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null)
        {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS || !matchesPlaceBlock(event))
        {
            return;
        }
        if (mc.player.isShiftKeyDown())
        {
            return;
        }
        if (!ToolLeaningConfig.ENABLE_TOOL_LEANING.get())
        {
            return;
        }

        final ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !held.is(ModTags.LEANABLE))
        {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        if (hit.getDirection().getAxis().isVertical()
            && !mc.level.getBlockState(hit.getBlockPos()).is(ModBlocks.LEANING_TOOL.get()))
        {
            return;
        }

        ModNetwork.sendLeanToServer();
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean matchesPlaceBlock(InputEvent.Key event)
    {
        return TFCKeyBindings.PLACE_BLOCK.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()));
    }

    private ToolLeanKeyHandler() {}
}
