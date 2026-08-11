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
 * <p>
 * Lean ({@code XP}) is applied before the upright turn so flat sprites tip into the wall.
 * Some tools ({@link ModTags#LEAN_FLIP_FACING}) need a yaw flip before leaning so the blade
 * faces left; that flip also swaps local {@code ±Z}, so the lean sign is inverted for those.
 */
public class LeaningToolRenderer implements BlockEntityRenderer<LeaningToolBlockEntity>
{
    private static final float LEAN_ANGLE = 22f;
    private static final float SLOT_SPACING = 0.2f;
    /** Tool center height; FIXED models are origin-centered. */
    private static final float CENTER_Y = 0.42f;
    /**
     * Toward wall (local {@code -Z}). Wall face is at {@code -0.5}; keep close enough that the
     * leaned tip meets the block without burying the handle.
     */
    private static final float WALL_OFFSET = -0.34f;
    private static final float SCALE = 0.72f;
    /**
     * Flat tool sprites are drawn corner-to-corner; this quarter-turn stands the handle on the floor
     * with the head up (same as Claude's working lean renderer).
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
            final boolean flipFacing = stack.is(ModTags.LEAN_FLIP_FACING);
            final float lateral = large
                ? 0f
                : (slot - (LeaningToolBlock.SLOTS - 1) / 2f) * SLOT_SPACING;

            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));

            // Foot near wall; optional left-facing yaw; tip into wall; stand flat sprites upright.
            pose.translate(lateral, CENTER_Y, WALL_OFFSET);
            if (flipFacing)
            {
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            // Yaw flip swaps local ±Z, so lean the other way to keep tipping into the wall.
            pose.mulPose(Axis.XP.rotationDegrees(flipFacing ? LEAN_ANGLE : -LEAN_ANGLE));
            if (flatSprite)
            {
                pose.mulPose(Axis.ZP.rotationDegrees(UPRIGHT_TURN));
            }
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
