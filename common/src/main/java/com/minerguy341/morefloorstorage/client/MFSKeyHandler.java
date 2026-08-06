package com.minerguy341.morefloorstorage.client;

import com.minerguy341.morefloorstorage.common.MFSTags;
import com.minerguy341.morefloorstorage.common.block.MFSBlocks;
import com.minerguy341.morefloorstorage.network.LeanToolPacket;
import com.minerguy341.morefloorstorage.network.PickupFloorStoragePacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.dries007.tfc.client.TFCKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Everything hanging off TerraFirmaCraft's floor storage key (V by default):
 * <ul>
 *     <li>Tap it aiming at a wall with a tool in hand to lean the tool against that wall.</li>
 *     <li>Sneak and tap it to pick items back out of whatever floor storage you are aiming at. This case
 *     is routed here from {@code TFCPlaceBlockKeyMixin}, which also stops TFC placing an item at the same
 *     time.</li>
 * </ul>
 */
public final class MFSKeyHandler
{
    /**
     * Called at the head of TFC's own key handler.
     *
     * @return {@code true} if TFC's handling should be skipped for this event.
     */
    public static boolean onTfcPlaceKey(InputEvent.Key event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown() || !TFCKeyBindings.PLACE_BLOCK.isDown())
        {
            return false;
        }

        // TFC fires on any key event while the key is held, so only act on the press of the key itself -
        // but suppress TFC's placement for the whole time it is held with sneak, not just on that press.
        if (event.getAction() == GLFW.GLFW_PRESS && matchesPlaceBlock(event))
        {
            PacketDistributor.sendToServer(PickupFloorStoragePacket.INSTANCE);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

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
            return; // Sneak is the pickup gesture
        }

        final ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !held.is(MFSTags.LEANABLE))
        {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        // A tool needs a wall to lean on, unless we are adding to tools that are already leaning
        if (hit.getDirection().getAxis().isVertical()
            && !mc.level.getBlockState(hit.getBlockPos()).is(MFSBlocks.LEANING_TOOL.get()))
        {
            return;
        }

        PacketDistributor.sendToServer(LeanToolPacket.INSTANCE);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean matchesPlaceBlock(InputEvent.Key event)
    {
        return TFCKeyBindings.PLACE_BLOCK.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()));
    }

    private MFSKeyHandler() {}
}
