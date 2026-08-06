package com.minerguy341.morefloorstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor.ARGB32;

/**
 * One lump in a pile - clay, ore, whatever the pile holds - as real geometry rather than the item's flat
 * inventory icon.
 * <p>
 * A lump is a squat frustum - a box with a smaller top than bottom - centred on the origin in X and Z
 * and standing on {@code y = 0}, so the caller can rotate it about its own base. The vertex order is
 * TerraFirmaCraft's, which is what its ingot piles use, but the texture is sampled from a window inside
 * the sprite instead of from its corner: item icons are usually transparent at the edges, and a lump
 * wants solid colour on every face.
 */
public final class LumpGeometry
{
    /** Width of a lump across its base, in blocks. */
    public static final float WIDTH = 0.25f;
    /** Height of a lump, in blocks. */
    public static final float HEIGHT = 0.1875f;
    /** How much narrower the top is than the base, per side, in blocks. */
    public static final float TAPER = 0.05f;

    /**
     * The slice of the sprite to texture with, as a fraction of it. The middle is used because the corners
     * of an item icon are usually empty, and because it is the most representative part of a chunk of ore,
     * whose sprite is mostly stone around the edges.
     */
    private static final float UV_MIN = 0.25f;
    private static final float UV_MAX = 0.75f;

    public static void render(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int packedLight, int packedOverlay)
    {
        final float p = WIDTH / 2f;      // half width at the base
        final float q = p - TAPER;       // half width at the top
        final float y1 = HEIGHT;

        // Sides facing along X, then the top and bottom, then the sides facing along Z
        emit(pose, buffer, sprite, packedLight, packedOverlay, new float[][] {
            {-p, 0, -p, 0, 1, 1}, {-p, 0, p, 1, 1, 1}, {-q, y1, q, 1, 0, 1}, {-q, y1, -q, 0, 0, 1},
            {p, 0, p, 1, 0, -1}, {p, 0, -p, 0, 0, -1}, {q, y1, -q, 0, 1, -1}, {q, y1, q, 1, 1, -1},
        }, 1, 0, 0);
        emit(pose, buffer, sprite, packedLight, packedOverlay, new float[][] {
            {-q, y1, -q, 0, 1, 1}, {-q, y1, q, 1, 1, 1}, {q, y1, q, 1, 0, 1}, {q, y1, -q, 0, 0, 1},
            {-p, 0, p, 1, 0, -1}, {-p, 0, -p, 0, 0, -1}, {p, 0, -p, 0, 1, -1}, {p, 0, p, 1, 1, -1},
        }, 0, 1, 0);
        emit(pose, buffer, sprite, packedLight, packedOverlay, new float[][] {
            {p, 0, -p, 0, 1, 1}, {-p, 0, -p, 1, 1, 1}, {-q, y1, -q, 1, 0, 1}, {q, y1, -q, 0, 0, 1},
            {-p, 0, p, 1, 0, -1}, {p, 0, p, 0, 0, -1}, {q, y1, q, 0, 1, -1}, {-q, y1, q, 1, 1, -1},
        }, 0, 0, 1);
    }

    /**
     * @param vertices rows of {@code x, y, z, u, v, normalSign}, four to a quad, wound so the quad faces
     *                 outwards. {@code u} and {@code v} are 0 or 1 and pick a corner of the UV window.
     */
    private static void emit(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int packedLight, int packedOverlay, float[][] vertices, float normalX, float normalY, float normalZ)
    {
        for (float[] vertex : vertices)
        {
            final float sign = vertex[5];
            final float shade = shadeFor(normalX * sign, normalY * sign, normalZ * sign);
            buffer.addVertex(pose.last().pose(), vertex[0], vertex[1], vertex[2])
                .setColor(ARGB32.colorFromFloat(1f, shade, shade, shade))
                .setUv(sprite.getU(lerp(vertex[3])), sprite.getV(lerp(vertex[4])))
                .setLight(packedLight)
                .setOverlay(packedOverlay)
                .setNormal(pose.last(), normalX * sign, normalY * sign, normalZ * sign);
        }
    }

    private static float lerp(float fraction)
    {
        return UV_MIN + (UV_MAX - UV_MIN) * fraction;
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
