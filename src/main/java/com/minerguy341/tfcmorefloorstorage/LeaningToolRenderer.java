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
 * Stands each leaned tool on the floor and tips it into the wall.
 * Local frame: after yaw, {@code -Z} points toward the wall ({@link LeaningToolBlock#FACING}).
 */
public class LeaningToolRenderer implements BlockEntityRenderer<LeaningToolBlockEntity>
{
    /** Degrees off vertical, tipping the head toward the wall. */
    private static final float LEAN_ANGLE = 22f;
    /** Lateral spacing between neighbouring tools. */
    private static final float SLOT_SPACING = 0.2f;
    /** How far from block centre toward the wall (wall is at local z = -0.5). */
    private static final float WALL_OFFSET = -0.40f;
    /** Pivot height above the floor (near the handle butt). */
    private static final float FOOT_Y = 0.04f;
    /** Raise the FIXED item model so the handle sits on the pivot / ground. */
    private static final float MODEL_LIFT = 0.42f;
    private static final float SCALE = 0.78f;
    /**
     * Flat sprites are drawn corner-to-corner; this quarter-turn stands the tool upright
     * before the lean is applied.
     */
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
        final boolean large = leaning.isHoldingLargeItem();

        for (int slot = 0; slot < LeaningToolBlock.SLOTS; slot++)
        {
            final ItemStack stack = leaning.getTool(slot);
            if (stack.isEmpty())
            {
                continue;
            }

            final BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, seed);
            final boolean flatSprite = !model.isGui3d();
            final float lateral = large
                ? 0f
                : (slot - (LeaningToolBlock.SLOTS - 1) / 2f) * SLOT_SPACING;

            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));

            // Foot near the floor, close to the wall; lean tips the head into the wall
            pose.translate(lateral, FOOT_Y, WALL_OFFSET);
            if (flatSprite)
            {
                pose.mulPose(Axis.ZP.rotationDegrees(UPRIGHT_TURN));
            }
            pose.mulPose(Axis.XP.rotationDegrees(-LEAN_ANGLE));
            pose.translate(0f, MODEL_LIFT, 0f);
            pose.scale(SCALE, SCALE, SCALE);

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
