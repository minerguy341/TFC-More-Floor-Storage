package com.minerguy341.morefloorstorage.client;

import java.util.List;

import com.minerguy341.morefloorstorage.common.block.PileBlock;
import com.minerguy341.morefloorstorage.common.block.PileLayout;
import com.minerguy341.morefloorstorage.common.blockentity.PileBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a pile as the stepped pyramid described by {@link PileLayout}: each item sits at its own spot in
 * its layer, so the heap visibly grows lump by lump as items are added and shrinks again as they are
 * taken off the top.
 * <p>
 * Lumps are real geometry rather than the item's flat inventory icon. An item with a model of its own -
 * see {@link PileModels} - is drawn with it; anything else falls back to {@link LumpGeometry}'s generic
 * frustum, textured from the middle of the item's sprite. Either way a pile reads as a heap rather than
 * a stack of paper discs, and one renderer serves every kind of pile.
 */
public class PileRenderer implements BlockEntityRenderer<PileBlockEntity>
{
    /** How far a lump may be spun about its own base, in degrees, to break up the grid. */
    private static final float MAX_SPIN = 25f;
    /** How far a lump may be tipped off level, in degrees. */
    private static final float MAX_TILT = 6f;
    private final RandomSource random = RandomSource.create();

    @Override
    public void render(PileBlockEntity pile, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay)
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

        // A group of four draws as one pyramid spanning the whole two by two, each block contributing
        // its own quadrant. Doubling a layer's grid quadruples it, so four full piles fill it exactly.
        // Each block heaps towards the group's middle, which is the corner it shares with the others.
        final BlockPos origin = PileBlock.groupOrigin(level, pos);
        final int lean = PileBlock.leanOf(level, pos);
        // Walls widen every layer, so they decide the grid the lumps are laid out on
        final int walls = PileBlock.wallsAt(level, pos);
        final int quadrantX = origin == null ? 0 : pos.getX() - origin.getX();
        final int quadrantZ = origin == null ? 0 : pos.getZ() - origin.getZ();

        // Knock a wall out from beside a full pit and the heap holds more than its new shape has room
        // for until it has finished spilling. Draw what fits: the rest is leaving.
        final int drawn = Math.min(stacks.size(), PileLayout.capacity(walls));

        for (int i = 0; i < drawn; i++)
        {
            final ItemStack stack = stacks.get(i);
            if (stack.isEmpty())
            {
                continue;
            }

            final int layer = PileLayout.layerOf(i, walls);
            final int within = PileLayout.indexInLayer(i, walls);
            final int grid = PileLayout.gridOf(layer, walls);
            final int cell = PileLayout.cellOf(layer, within, walls, lean);
            final int column = cell % grid;
            final int row = cell / grid;

            // Offsets are from the centre of this block when standing alone, and from the centre of the
            // two by two when merged - which is a corner of this block, hence the (1 - quadrant) shift.
            final float x;
            final float z;
            if (origin == null)
            {
                x = 0.5f + PileLayout.offsetInLayer(layer, column, walls);
                z = 0.5f + PileLayout.offsetInLayer(layer, row, walls);
            }
            else
            {
                x = (1 - quadrantX) + PileLayout.offsetInMergedLayer(layer, quadrantX * grid + column, walls);
                z = (1 - quadrantZ) + PileLayout.offsetInMergedLayer(layer, quadrantZ * grid + row, walls);
            }

            pose.pushPose();
            pose.translate(x, PileLayout.heightOf(layer), z);
            pose.mulPose(Axis.YP.rotationDegrees(jitter(seed, i, 0) * MAX_SPIN));
            pose.mulPose(Axis.XP.rotationDegrees(jitter(seed, i, 1) * MAX_TILT));
            pose.mulPose(Axis.ZP.rotationDegrees(jitter(seed, i, 2) * MAX_TILT));

            final PileModels.Lump lump = PileModels.lookup(stack, random);
            if (lump != null)
            {
                lump.applyTo(pose);
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateWithoutAO(
                    level, lump.model(), pile.getBlockState(), pos, pose, buffer, false,
                    random, seed + i, packedOverlay, ModelData.EMPTY, RenderType.cutout());
            }
            else
            {
                final TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer()
                    .getModel(stack, level, null, seed + i)
                    .getParticleIcon();
                LumpGeometry.render(pose, buffer, sprite, packedLight, packedOverlay);
            }

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
