package com.minerguy341.morefloorstorage.client;

import java.util.List;

import com.minerguy341.morefloorstorage.common.block.PileBlock;
import com.minerguy341.morefloorstorage.common.block.PileGroup;
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
 * see {@link PileModels} - is drawn with it, and scattered a little so a heap of ore reads as a heap.
 * Anything else gets {@link LumpGeometry}'s half ingot, laid square and turned across the course below,
 * so a heap of clay stacks the way TerraFirmaCraft's ingot piles do. One renderer serves every kind of
 * pile either way.
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

        // A merged group draws as one pyramid spanning the whole rectangle, each block contributing its
        // own cell of it. Multiplying a layer's grid by the span in both directions multiplies its cells
        // by the number of blocks, so a full group fills it exactly.
        final PileGroup group = PileGroup.at(level, pos);
        final int lean = PileBlock.leanOf(level, pos);
        // Walls widen every layer, so they decide the grid the lumps are laid out on
        final int walls = PileBlock.wallsAt(level, pos);
        final int cellX = group == null ? 0 : group.cellX(pos);
        final int cellZ = group == null ? 0 : group.cellZ(pos);

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
            // group when merged - so they are shifted back by however far this cell is from that centre.
            final float x;
            final float z;
            if (group == null)
            {
                x = 0.5f + PileLayout.offsetInLayer(layer, column, walls);
                z = 0.5f + PileLayout.offsetInLayer(layer, row, walls);
            }
            else
            {
                x = (group.spanX() / 2f - cellX)
                    + PileLayout.offsetInMergedLayer(layer, cellX * grid + column, walls, group.spanX());
                z = (group.spanZ() / 2f - cellZ)
                    + PileLayout.offsetInMergedLayer(layer, cellZ * grid + row, walls, group.spanZ());
            }

            pose.pushPose();
            pose.translate(x, PileLayout.heightOf(layer), z);

            final PileModels.Lump lump = PileModels.lookup(stack, random);
            if (lump.model() != null)
            {
                // A lump of something with a shape of its own is scattered, so a heap of ore reads as a
                // heap rather than as a grid
                pose.mulPose(Axis.YP.rotationDegrees(jitter(seed, i, 0) * MAX_SPIN));
                pose.mulPose(Axis.XP.rotationDegrees(jitter(seed, i, 1) * MAX_TILT));
                pose.mulPose(Axis.ZP.rotationDegrees(jitter(seed, i, 2) * MAX_TILT));
                lump.applyTo(pose);
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateWithoutAO(
                    level, lump.model(), pile.getBlockState(), pos, pose, buffer, false,
                    random, seed + i, packedOverlay, ModelData.EMPTY, RenderType.cutout());
            }
            else
            {
                // Bars are stacked, not scattered: each course laid square and turned across the one
                // below it, the way TerraFirmaCraft's ingot piles are built
                if (layer % 2 == 1)
                {
                    pose.mulPose(Axis.YP.rotationDegrees(90f));
                }
                LumpGeometry.render(pose, buffer, spriteFor(stack, lump, level, seed + i), packedLight, packedOverlay);
            }

            pose.popPose();
        }
    }

    /**
     * What to wrap a bar in: the texture the item's lump model names, or failing that the item's own icon.
     */
    private static TextureAtlasSprite spriteFor(ItemStack stack, PileModels.Lump lump, Level level, int seed)
    {
        return lump.sprite() != null
            ? lump.sprite()
            : Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, seed).getParticleIcon();
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
