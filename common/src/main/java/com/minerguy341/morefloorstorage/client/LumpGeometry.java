package com.minerguy341.morefloorstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor.ARGB32;

/**
 * One lump in a pile, for anything with no lump model of its own - see {@link PileModels}.
 * <p>
 * It is half a TerraFirmaCraft ingot: the same bar, the same one-texel bevel off the top, cut to half its
 * length and drawn at half scale, so a heap of clay stacks like a heap of ingots rather than like a heap
 * of blobs.
 * <p>
 * That comes to 3.5 by 2 by 3.75 pixels, against a grid 3 pixels across and courses 2.48 pixels apart.
 * So bars press about half a pixel into their neighbours and leave about half a pixel of daylight between
 * courses - a tight stack with a mortar line, which is the intent. It is less crowded than the frustum it
 * replaces, which was 4 pixels across and overlapped by a whole one. Shrink {@link #TEXEL} to pull them
 * apart, and remember the pile's own grid is five across a block, not four like an ingot pile's.
 * <p>
 * It is centred on the origin in X and Z and stands on {@code y = 0}, so the caller can turn it about its
 * own base. The vertex order is TerraFirmaCraft's, which is what its ingot piles use.
 */
public final class LumpGeometry
{
    /** An ingot in TerraFirmaCraft, in texels, halved along its length. */
    private static final float WIDTH_TEXELS = 7f;
    private static final float HEIGHT_TEXELS = 4f;
    private static final float LENGTH_TEXELS = 7.5f;

    /** How much narrower the top is than the base, per side, in texels. */
    private static final float BEVEL_TEXELS = 1f;

    /** Half an ordinary block texel, so the whole thing comes out half the size of a real ingot. */
    public static final float TEXEL = 1f / 32f;

    /** Width of a lump across its base, in blocks, which is what the pile lays its grid out around. */
    public static final float WIDTH = WIDTH_TEXELS * TEXEL;
    /** Height of a lump, in blocks. */
    public static final float HEIGHT = HEIGHT_TEXELS * TEXEL;

    public static void render(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int packedLight, int packedOverlay)
    {
        final float halfWidth = WIDTH_TEXELS * TEXEL / 2f;
        final float halfLength = LENGTH_TEXELS * TEXEL / 2f;
        final float bevel = BEVEL_TEXELS * TEXEL;
        final float topWidth = halfWidth - bevel;
        final float topLength = halfLength - bevel;
        final float top = HEIGHT_TEXELS * TEXEL;

        final float x0 = -halfWidth, x1 = halfWidth, tx0 = -topWidth, tx1 = topWidth;
        final float z0 = -halfLength, z1 = halfLength, tz0 = -topLength, tz1 = topLength;

        // Faces looking along X: as wide as the lump is long, as tall as it is tall
        emit(pose, buffer, sprite, packedLight, packedOverlay, LENGTH_TEXELS, HEIGHT_TEXELS, new float[][] {
            {x0, 0, z0, 0, 1, 1}, {x0, 0, z1, 1, 1, 1}, {tx0, top, tz1, 1, 0, 1}, {tx0, top, tz0, 0, 0, 1},
            {x1, 0, z1, 1, 0, -1}, {x1, 0, z0, 0, 0, -1}, {tx1, top, tz0, 0, 1, -1}, {tx1, top, tz1, 1, 1, -1},
        }, 1, 0, 0);
        // Top and bottom: the full footprint
        emit(pose, buffer, sprite, packedLight, packedOverlay, WIDTH_TEXELS, LENGTH_TEXELS, new float[][] {
            {tx0, top, tz0, 0, 1, 1}, {tx0, top, tz1, 1, 1, 1}, {tx1, top, tz1, 1, 0, 1}, {tx1, top, tz0, 0, 0, 1},
            {x0, 0, z1, 1, 0, -1}, {x0, 0, z0, 0, 0, -1}, {x1, 0, z0, 0, 1, -1}, {x1, 0, z1, 1, 1, -1},
        }, 0, 1, 0);
        // Faces looking along Z: the two ends of the bar
        emit(pose, buffer, sprite, packedLight, packedOverlay, WIDTH_TEXELS, HEIGHT_TEXELS, new float[][] {
            {x1, 0, z0, 0, 1, 1}, {x0, 0, z0, 1, 1, 1}, {tx0, top, tz0, 1, 0, 1}, {tx1, top, tz0, 0, 0, 1},
            {x0, 0, z1, 1, 0, -1}, {x1, 0, z1, 0, 0, -1}, {tx1, top, tz1, 0, 1, -1}, {tx0, top, tz1, 1, 1, -1},
        }, 0, 0, 1);
    }

    /**
     * @param vertices rows of {@code x, y, z, u, v, normalSign}, four to a quad, wound so the quad faces
     *                 outwards. {@code u} and {@code v} are 0 or 1 and pick a corner of the UV window.
     * @param acrossTexels how big this face is, so its texture comes out at the size it was drawn at
     */
    private static void emit(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int packedLight, int packedOverlay, float acrossTexels, float downTexels, float[][] vertices, float normalX, float normalY, float normalZ)
    {
        for (float[] vertex : vertices)
        {
            final float sign = vertex[5];
            final float shade = shadeFor(normalX * sign, normalY * sign, normalZ * sign);
            buffer.addVertex(pose.last().pose(), vertex[0], vertex[1], vertex[2])
                .setColor(ARGB32.colorFromFloat(1f, shade, shade, shade))
                .setUv(sprite.getU(window(vertex[3], acrossTexels)), sprite.getV(window(vertex[4], downTexels)))
                .setLight(packedLight)
                .setOverlay(packedOverlay)
                .setNormal(pose.last(), normalX * sign, normalY * sign, normalZ * sign);
        }
    }

    /**
     * The slice of the sprite a face takes: as many texels as the face is wide, out of the sixteen a block
     * texture has, taken from the middle.
     * <p>
     * Sampling at the texture's own scale is what keeps clay looking like clay rather than a smear, and
     * taking it from the middle matters for the items that fall back to their inventory icon, whose
     * corners are usually empty.
     */
    private static float window(float fraction, float texels)
    {
        final float span = Math.min(1f, texels / 16f);
        return (1f - span) / 2f + span * fraction;
    }

    /**
     * Minecraft's fixed per-direction diffuse shading, so lumps sit in the world's lighting the way an
     * ordinary block face does.
     */
    private static float shadeFor(float normalX, float normalY, float normalZ)
    {
        if (normalY > 0.5f) return 1f;
        if (normalY < -0.5f) return 0.5f;
        if (Math.abs(normalZ) > 0.5f) return 0.8f;
        if (Math.abs(normalX) > 0.5f) return 0.6f;
        return 1f;
    }

    private LumpGeometry() {}
}
