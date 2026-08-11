package com.minerguy341.tfcmorefloorstorage;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.util.Metal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional config-driven override for TFC metal ingot / double-ingot pile meshes.
 * Clay uses {@link ClayPileBlockModel} instead.
 */
public final class IngotPileMeshRenderer
{
    private IngotPileMeshRenderer() {}

    public static TextureAtlasSprite renderSingle(
        IngotPileBlockEntity pile,
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int ingots
    )
    {
        return renderPile(
            pile,
            poseStack,
            buffer,
            packedLight,
            packedOverlay,
            ingots,
            8,
            4,
            0.25f,
            0.125f,
            IngotMeshClientConfig.INGOT_WIDTH_TEXELS.get().floatValue(),
            IngotMeshClientConfig.INGOT_HEIGHT_TEXELS.get().floatValue(),
            IngotMeshClientConfig.INGOT_LENGTH_TEXELS.get().floatValue(),
            IngotMeshClientConfig.INGOT_INSET_TEXELS.get().floatValue()
        );
    }

    public static TextureAtlasSprite renderDouble(
        IngotPileBlockEntity pile,
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int ingots
    )
    {
        return renderPile(
            pile,
            poseStack,
            buffer,
            packedLight,
            packedOverlay,
            ingots,
            6,
            3,
            0.33f,
            1f / 6f,
            IngotMeshClientConfig.DOUBLE_WIDTH_TEXELS.get().floatValue(),
            IngotMeshClientConfig.DOUBLE_HEIGHT_TEXELS.get().floatValue(),
            IngotMeshClientConfig.DOUBLE_LENGTH_TEXELS.get().floatValue(),
            IngotMeshClientConfig.DOUBLE_INSET_TEXELS.get().floatValue()
        );
    }

    private static TextureAtlasSprite renderPile(
        IngotPileBlockEntity pile,
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int ingots,
        int perLayer,
        int gridX,
        float cellX,
        float baseLayerHeight,
        float widthTexels,
        float heightTexels,
        float lengthTexels,
        float insetTexels
    )
    {
        final Function<ResourceLocation, TextureAtlasSprite> textureAtlas =
            Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS);

        final float sizeScale = IngotMeshClientConfig.SIZE_SCALE.get().floatValue();
        final float layerHeight = baseLayerHeight * IngotMeshClientConfig.LAYER_HEIGHT_SCALE.get().floatValue();
        final float bevel = IngotMeshClientConfig.TRAPEZOID_BEVEL_TEXELS.get().floatValue() * sizeScale;
        final boolean quirk = IngotMeshClientConfig.MATCH_VANILLA_SCALE_QUIRK.get();

        widthTexels *= sizeScale;
        heightTexels *= sizeScale;
        lengthTexels *= sizeScale;

        TextureAtlasSprite sprite = null;
        for (int i = 0; i < ingots; i++)
        {
            final Metal metal = pile.getOrCacheMetal(i);
            sprite = textureAtlas.apply(metal.getSoftTextureId());

            final int layer = (i + perLayer) / perLayer;
            final boolean oddLayer = (layer % 2) == 1;
            final float x = (i % gridX) * cellX;
            final float y = (layer - 1) * layerHeight;
            final float z = i % perLayer >= gridX ? 0.5f : 0;

            poseStack.pushPose();
            if (oddLayer)
            {
                poseStack.translate(0.5f, 0f, 0.5f);
                poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                poseStack.translate(-0.5f, 0f, -0.5f);
            }

            poseStack.translate(x, y, z);

            final float scale = 0.0625f / 2f;
            final CuboidBounds bounds = cuboidBounds(scale, insetTexels, widthTexels, heightTexels, lengthTexels, quirk);
            final float bevelWorld = scale * bevel;

            RenderHelpers.renderTexturedTrapezoidalCuboid(
                poseStack,
                buffer,
                sprite,
                packedLight,
                packedOverlay,
                bounds.minX,
                bounds.maxX,
                bounds.minZ,
                bounds.maxZ,
                bounds.minX + bevelWorld,
                bounds.maxX - bevelWorld,
                bounds.minZ + bevelWorld,
                bounds.maxZ - bevelWorld,
                bounds.minY,
                bounds.maxY,
                widthTexels,
                heightTexels,
                lengthTexels,
                oddLayer
            );

            poseStack.popPose();
        }

        if (sprite == null)
        {
            sprite = RenderHelpers.missingTexture();
        }
        return sprite;
    }

    static CuboidBounds cuboidBounds(
        float scale,
        float insetTexels,
        float widthTexels,
        float heightTexels,
        float lengthTexels,
        boolean quirk
    )
    {
        final float minX = scale * insetTexels;
        final float minY = scale * 0f;
        final float minZ = scale * insetTexels;

        final float maxX;
        final float maxY;
        final float maxZ;
        if (quirk)
        {
            maxX = scale * (minX + widthTexels);
            maxY = scale * (minY + heightTexels);
            maxZ = scale * (minZ + lengthTexels);
        }
        else
        {
            maxX = scale * (insetTexels + widthTexels);
            maxY = scale * heightTexels;
            maxZ = scale * (insetTexels + lengthTexels);
        }
        return new CuboidBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    record CuboidBounds(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {}
}
