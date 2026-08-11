package com.minerguy341.morefloorstorage.compat.tfc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Half a TerraFirmaCraft ingot, drawn by TerraFirmaCraft: the same bar with the same one-texel bevel off
 * the top, cut to half its length and drawn at half scale.
 * <p>
 * It is what a pile falls back to for anything with no lump model of its own, so a heap of clay stacks
 * like a heap of ingots rather than like a heap of blobs. Going through TFC's own helper rather than
 * emitting the vertices here is the point: the texture is then mapped and the faces shaded exactly the
 * way TFC's ingot piles are, so the two sit side by side without one of them looking almost right.
 * <p>
 * The bar is centred on the origin in X and Z and stands on {@code y = 0}, so the caller can turn it
 * about its own base.
 * <p>
 * At 3.5 by 2 by 3.75 pixels it sits in a pile's grid of 3 pixels across with courses 2.48 pixels apart,
 * pressing about half a pixel into its neighbours and leaving about half a pixel between courses - a
 * tight stack with a mortar line. Shrink {@link #TEXEL} to pull them apart, remembering that a pile's
 * grid is five across a block, not four like an ingot pile's.
 */
public final class IngotMesh
{
    /** An ingot in TerraFirmaCraft, in texels, halved along its length. */
    private static final float WIDTH_TEXELS = 7f;
    private static final float HEIGHT_TEXELS = 4f;
    private static final float LENGTH_TEXELS = 7.5f;

    /** How much narrower the top is than the base, per side, in texels. */
    private static final float BEVEL_TEXELS = 1f;

    /** Half an ordinary block texel, so the whole thing comes out half the size of a real ingot. */
    public static final float TEXEL = 1f / 32f;

    /** Width of a bar across its base, in blocks. */
    public static final float WIDTH = WIDTH_TEXELS * TEXEL;
    /** Height of a bar, in blocks. */
    public static final float HEIGHT = HEIGHT_TEXELS * TEXEL;

    public static void render(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int packedLight, int packedOverlay)
    {
        final float halfWidth = WIDTH_TEXELS * TEXEL / 2f;
        final float halfLength = LENGTH_TEXELS * TEXEL / 2f;
        final float bevel = BEVEL_TEXELS * TEXEL;

        RenderHelpers.renderTexturedTrapezoidalCuboid(
            pose,
            buffer,
            sprite,
            packedLight,
            packedOverlay,
            -halfWidth,           // base, in X
            halfWidth,
            -halfLength,          // base, in Z
            halfLength,
            -halfWidth + bevel,   // top, drawn in from the base on every side
            halfWidth - bevel,
            -halfLength + bevel,
            halfLength - bevel,
            0f,                   // standing on the layer it belongs to
            HEIGHT_TEXELS * TEXEL,
            WIDTH_TEXELS,         // the size it is drawn at, so its texture comes out at that size
            HEIGHT_TEXELS,
            LENGTH_TEXELS,
            false);
    }

    private IngotMesh() {}
}
