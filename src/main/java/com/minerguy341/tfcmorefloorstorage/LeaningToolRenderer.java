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
 * Lean ({@code XP}) must run while local {@code X} is still along the wall; a prior {@code ZP}
 * spin remaps that axis and tips tools sideways. Facing/upright spins happen after the lean.
 */
public class LeaningToolRenderer implements BlockEntityRenderer<LeaningToolBlockEntity>
{
    /**
     * Pose constants match mesh-visualizer/defaults.json (side-view verified).
     * Degrees off vertical, tip toward the wall.
     */
    private static final float LEAN_ANGLE = 28f;
    private static final float SLOT_SPACING = 0.2f;
    /** Tool center height; FIXED models are origin-centered. */
    private static final float CENTER_Y = 0.42f;
    /**
     * Toward wall (local {@code -Z}). Wall face is at {@code -0.5}.
     */
    private static final float WALL_OFFSET = -0.34f;
    /**
     * Long tip-heavy tools ({@link ModTags#LEAN_CLEAR_WALL}): ~0.23 block off the wall face.
     */
    private static final float CLEAR_WALL_OFFSET = -0.27f;
    /**
     * Knives / tuyeres ({@link ModTags#LEAN_CLOSER_WALL}): 0.05 closer to the wall than clear-wall.
     */
    private static final float CLOSER_WALL_OFFSET = -0.32f;
    private static final float SCALE = 0.72f;
    /**
     * Flat tool sprites are drawn corner-to-corner; this quarter-turn stands the handle on the floor
     * with the head up (same as Claude's working lean renderer).
     */
    private static final float UPRIGHT_TURN = -45f;
    /**
     * Extra spin for knives/chisels/tuyeres/saws (clockwise when viewing the FIXED sprite face),
     * applied after the lean so it does not steal the tip-into-wall axis.
     */
    private static final float FLIP_FACING_TURN = 90f;

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
            final float wallOffset = stack.is(ModTags.LEAN_CLOSER_WALL)
                ? CLOSER_WALL_OFFSET
                : stack.is(ModTags.LEAN_CLEAR_WALL) ? CLEAR_WALL_OFFSET : WALL_OFFSET;
            final float lateral = large
                ? 0f
                : (slot - (LeaningToolBlock.SLOTS - 1) / 2f) * SLOT_SPACING;

            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));

            // Foot near wall → tip into -Z → then facing/upright spins in the leaned frame.
            pose.translate(lateral, CENTER_Y, wallOffset);
            pose.mulPose(Axis.XP.rotationDegrees(-LEAN_ANGLE));
            if (flipFacing)
            {
                pose.mulPose(Axis.ZP.rotationDegrees(FLIP_FACING_TURN));
            }
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
