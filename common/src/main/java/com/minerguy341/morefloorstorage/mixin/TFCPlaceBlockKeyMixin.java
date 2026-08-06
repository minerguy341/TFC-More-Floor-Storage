package com.minerguy341.morefloorstorage.mixin;

import com.minerguy341.morefloorstorage.client.MFSKeyHandler;
import net.dries007.tfc.client.ClientForgeEventHandler;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TerraFirmaCraft sends its "place a block on the floor" packet whenever the floor storage key is held,
 * without caring whether the player is sneaking. We want sneak + that key to mean the opposite - take an
 * item back out - so this takes the key event over before TFC sees it, but only while sneaking. Ordinary
 * presses fall through to TFC untouched.
 */
@Mixin(value = ClientForgeEventHandler.class, remap = false)
public class TFCPlaceBlockKeyMixin
{
    @Inject(method = "onKeyEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void morefloorstorage$sneakPickup(InputEvent.Key event, CallbackInfo ci)
    {
        if (MFSKeyHandler.onTfcPlaceKey(event))
        {
            ci.cancel();
        }
    }
}
