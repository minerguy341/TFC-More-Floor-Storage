package com.minerguy341.morefloorstorage.client;

import com.minerguy341.morefloorstorage.common.block.LeaningToolBlock;
import com.minerguy341.morefloorstorage.common.blockentity.LeaningToolBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Stands each leaned tool up on the floor and tips it back into the wall.
 * <p>
 * The local frame is set up so that after the yaw rotation the wall is always to the north, which keeps
 * the slot offsets in one axis and matches {@link LeaningToolBlock#slotFromHit}.
 */
public class LeaningToolRenderer implements BlockEntityRenderer<LeaningToolBlockEntity>
{
    /** How far the top of a tool tips towards the wall, in degrees off vertical. */
    private static final float LEAN_ANGLE = 14f;
    /** Spacing between neighbouring tools, in blocks. */
    private static final float SLOT_SPACING = 0.22f;

    @Override
    public void render(LeaningToolBlockEntity leaning, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        final Level level = leaning.getLevel();
        if (level == null)
        {
            return;
        }

        final Direction facing = leaning.getBlockState().getValue(LeaningToolBlock.FACING);
        final int seed = leaning.getBlockPos().hashCode();

        for (int slot = 0; slot < LeaningToolBlock.SLOTS; slot++)
        {
            final ItemStack stack = leaning.getTool(slot);
            if (stack.isEmpty())
            {
                continue;
            }

            // Flat sprites need a quarter turn to stand the tool's diagonal upright; real 3D models do not
            final BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, seed);
            final boolean flatSprite = !model.isGui3d();

            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
            pose.translate((slot - (LeaningToolBlock.SLOTS - 1) / 2f) * SLOT_SPACING, 0.45f, -0.16f);
            pose.mulPose(Axis.XP.rotationDegrees(-LEAN_ANGLE));
            if (flatSprite)
            {
                pose.mulPose(Axis.ZP.rotationDegrees(45f));
            }
            pose.scale(0.7f, 0.7f, 0.7f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buffers, level, seed + slot);

            pose.popPose();
        }
    }

    @Override
    public int getViewDistance()
    {
        return 32;
    }
}
