package com.minerguy341.tfcmorefloorstorage;

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
 * Stands each leaned tool up and tips it back into the wall.
 * Ported from Claude's {@code LeaningToolRenderer}.
 */
public class LeaningToolRenderer implements BlockEntityRenderer<LeaningToolBlockEntity>
{
    private static final float LEAN_ANGLE = 14f;
    private static final float SLOT_SPACING = 0.22f;
    /** Flat item sprites need a quarter turn to stand the tool diagonal upright. */
    private static final float UPRIGHT_TURN = -45f;

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

            final BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, seed);
            final boolean flatSprite = !model.isGui3d();

            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
            pose.translate((slot - (LeaningToolBlock.SLOTS - 1) / 2f) * SLOT_SPACING, 0.45f, -0.16f);
            pose.mulPose(Axis.XP.rotationDegrees(-LEAN_ANGLE));
            if (flatSprite)
            {
                pose.mulPose(Axis.ZP.rotationDegrees(UPRIGHT_TURN));
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
