package com.minerguy341.tfcmorefloorstorage.mixin;

import com.minerguy341.tfcmorefloorstorage.IngotMeshClientConfig;
import com.minerguy341.tfcmorefloorstorage.IngotPileMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dries007.tfc.client.model.IngotPileBlockModel;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blocks.devices.IngotPileBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IngotPileBlockModel.class)
public abstract class IngotPileBlockModelMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcmfs$replaceIngotMesh(
        IngotPileBlockEntity pile,
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        CallbackInfoReturnable<TextureAtlasSprite> cir
    )
    {
        if (!IngotMeshClientConfig.ENABLED.get())
        {
            return;
        }

        final int ingots = pile.getBlockState().getValue(IngotPileBlock.COUNT);
        cir.setReturnValue(IngotPileMeshRenderer.renderSingle(pile, poseStack, buffer, packedLight, packedOverlay, ingots));
    }
}
