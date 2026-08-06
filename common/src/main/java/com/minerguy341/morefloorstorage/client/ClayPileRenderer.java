package com.minerguy341.morefloorstorage.client;

import java.util.List;

import com.minerguy341.morefloorstorage.common.block.ClayPileBlock;
import com.minerguy341.morefloorstorage.common.block.ClayPileLayout;
import com.minerguy341.morefloorstorage.common.blockentity.ClayPileBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Draws a clay pile as the stepped pyramid described by {@link ClayPileLayout}: each item sits at its own
 * spot in its layer, so the heap visibly grows lump by lump as clay is added and shrinks again as it is
 * taken off the top.
 * <p>
 * Lumps are real geometry rather than the item's flat inventory icon - see {@link ClayLumpGeometry} - so
 * a pile reads as a heap of clay rather than a stack of paper discs. All the icon is used for is its
 * colour, which means any clay-type item, including a modded one, gets a lump that looks like itself.
 */
public class ClayPileRenderer implements BlockEntityRenderer<ClayPileBlockEntity>
{
    /** How far a lump may be spun about its own base, in degrees, to break up the grid. */
    private static final float MAX_SPIN = 25f;
    /** How far a lump may be tipped off level, in degrees. */
    private static final float MAX_TILT = 6f;

    @Override
    public void render(ClayPileBlockEntity pile, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        final Level level = pile.getLevel();
        if (level == null)
        {
            return;
        }

        final List<ItemStack> stacks = pile.getStacks();
        final BlockPos pos = pile.getBlockPos();
        final int seed = pos.hashCode();
        final VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());

        // When four full piles merge, each one draws its own 64 items as one quadrant of a pyramid
        // spanning the whole two by two. Doubling a layer's grid quadruples it, so the counts line up
        // exactly and no items have to move between block entities.
        final BlockPos origin = ClayPileBlock.mergeOrigin(level, pos);
        final int quadrantX = origin == null ? 0 : pos.getX() - origin.getX();
        final int quadrantZ = origin == null ? 0 : pos.getZ() - origin.getZ();

        for (int i = 0; i < stacks.size(); i++)
        {
            final ItemStack stack = stacks.get(i);
            if (stack.isEmpty())
            {
                continue;
            }

            final int layer = ClayPileLayout.layerOf(i);
            final int within = ClayPileLayout.indexInLayer(i);
            final int grid = ClayPileLayout.GRID[layer];
            final int column = within % grid;
            final int row = within / grid;

            // Offsets are from the centre of this block when standing alone, and from the centre of the
            // two by two when merged - which is a corner of this block, hence the (1 - quadrant) shift.
            final float x;
            final float z;
            if (origin == null)
            {
                x = 0.5f + ClayPileLayout.offsetInLayer(layer, column);
                z = 0.5f + ClayPileLayout.offsetInLayer(layer, row);
            }
            else
            {
                x = (1 - quadrantX) + ClayPileLayout.offsetInMergedLayer(layer, quadrantX * grid + column);
                z = (1 - quadrantZ) + ClayPileLayout.offsetInMergedLayer(layer, quadrantZ * grid + row);
            }

            final TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer()
                .getModel(stack, level, null, seed + i)
                .getParticleIcon();

            pose.pushPose();
            pose.translate(x, ClayPileLayout.heightOf(layer), z);
            pose.mulPose(Axis.YP.rotationDegrees(jitter(seed, i, 0) * MAX_SPIN));
            pose.mulPose(Axis.XP.rotationDegrees(jitter(seed, i, 1) * MAX_TILT));
            pose.mulPose(Axis.ZP.rotationDegrees(jitter(seed, i, 2) * MAX_TILT));

            ClayLumpGeometry.render(pose, buffer, sprite, packedLight, packedOverlay);

            pose.popPose();
        }
    }

    /**
     * A stable pseudo-random value in {@code [-1, 1]} for one item, so a pile looks the same every frame
     * and every time the chunk is reloaded, but two piles side by side do not look identical.
     */
    private static float jitter(int seed, int index, int channel)
    {
        int hash = seed * 31 + index;
        hash = hash * 31 + channel;
        hash ^= hash >>> 15;
        hash *= 0x2c1b3c6d;
        hash ^= hash >>> 12;
        return ((hash & 0xffff) / 32767.5f) - 1f;
    }

    @Override
    public int getViewDistance()
    {
        return 24;
    }
}
