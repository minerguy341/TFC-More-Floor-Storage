package com.minerguy341.tfcmorefloorstorage.mixin;

import com.minerguy341.tfcmorefloorstorage.IngotMeshClientConfig;
import com.minerguy341.tfcmorefloorstorage.IngotPileMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dries007.tfc.client.model.DoubleIngotPileBlockModel;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blocks.devices.DoubleIngotPileBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoubleIngotPileBlockModel.class)
public abstract class DoubleIngotPileBlockModelMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcmfs$replaceDoubleIngotMesh(
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

        final int ingots = pile.getBlockState().getValue(DoubleIngotPileBlock.DOUBLE_COUNT);
        cir.setReturnValue(IngotPileMeshRenderer.renderDouble(pile, poseStack, buffer, packedLight, packedOverlay, ingots));
    }
}
