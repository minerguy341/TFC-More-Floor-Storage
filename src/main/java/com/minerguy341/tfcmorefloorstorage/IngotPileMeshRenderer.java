package com.minerguy341.tfcmorefloorstorage;

import java.util.List;
import java.util.function.Function;

import com.minerguy341.tfcmorefloorstorage.mixin.IngotPileBlockEntityAccessor;
import com.minerguy341.tfcmorefloorstorage.mixin.IngotPileEntryAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.util.Metal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Procedural ingot / double-ingot drawing, adapted from TFC's
 * {@code IngotPileBlockModel} / {@code DoubleIngotPileBlockModel} with config-driven sizes.
 * <p>
 * Layout (single): layers of 8 on a 4×2 grid, alternating 90° yaw.
 * Layout (double): layers of 6 on a 3×2 grid, alternating 90° yaw.
 * <p>
 * Special case: vanilla {@link Items#CLAY_BALL} uses half length (≈ square blob) and the clay block texture.
 */
public final class IngotPileMeshRenderer
{
    /** Half of the default 15-texel length → ~7.5, close to the 7-texel width. */
    private static final float VANILLA_CLAY_LENGTH_FACTOR = 0.5f;
    private static final ResourceLocation VANILLA_CLAY_TEXTURE = new ResourceLocation("minecraft", "block/clay");

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
            /*perLayer*/ 8,
            /*gridX*/ 4,
            /*cellX*/ 0.25f,
            /*baseLayerHeight*/ 0.125f,
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
            /*perLayer*/ 6,
            /*gridX*/ 3,
            /*cellX*/ 0.33f,
            /*baseLayerHeight*/ 1f / 6f,
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

        final float baseWidth = widthTexels * sizeScale;
        final float baseHeight = heightTexels * sizeScale;
        final float baseLength = lengthTexels * sizeScale;

        TextureAtlasSprite sprite = null;
        for (int i = 0; i < ingots; i++)
        {
            final ItemStack stack = stackAt(pile, i);
            final boolean vanillaClay = stack.is(Items.CLAY_BALL);

            float pieceWidth = baseWidth;
            float pieceHeight = baseHeight;
            float pieceLength = baseLength;

            if (vanillaClay)
            {
                pieceLength *= VANILLA_CLAY_LENGTH_FACTOR;
                sprite = textureAtlas.apply(VANILLA_CLAY_TEXTURE);
            }
            else
            {
                final Metal metal = pile.getOrCacheMetal(i);
                sprite = textureAtlas.apply(metal.getSoftTextureId());
            }

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
            // Keep shortened clay blobs centered in the slot along length.
            if (vanillaClay)
            {
                final float lengthDelta = baseLength - pieceLength;
                poseStack.translate(0f, 0f, scale * (lengthDelta * 0.5f));
            }

            final CuboidBounds bounds = cuboidBounds(scale, insetTexels, pieceWidth, pieceHeight, pieceLength, quirk);
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
                pieceWidth,
                pieceHeight,
                pieceLength,
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

    /**
     * Builds world-space AABB for one ingot.
     * When {@code quirk} is true, mirrors TFC's {@code max = scale * (min + size)} math.
     */
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

    private static ItemStack stackAt(IngotPileBlockEntity pile, int index)
    {
        final List<Object> entries = ((IngotPileBlockEntityAccessor) pile).tfcmfs$getEntries();
        if (index < 0 || index >= entries.size())
        {
            return ItemStack.EMPTY;
        }
        return ((IngotPileEntryAccessor) entries.get(index)).tfcmfs$getStack();
    }

    record CuboidBounds(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {}
}
