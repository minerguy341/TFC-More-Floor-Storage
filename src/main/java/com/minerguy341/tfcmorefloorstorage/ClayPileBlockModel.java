package com.minerguy341.tfcmorefloorstorage;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.model.SimpleStaticBlockEntityModel;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Neat clay pile mesh: half-length square blobs, 4×4 per layer from the block corner, no criss-cross.
 * Capacity 128 = 16 × 8 layers. Supports vanilla clay, kaolin clay, and fire clay textures.
 */
public enum ClayPileBlockModel implements SimpleStaticBlockEntityModel<ClayPileBlockModel, ClayPileBlockEntity>
{
    INSTANCE;

    private static final ResourceLocation VANILLA_CLAY_TEXTURE = new ResourceLocation("minecraft", "block/clay");
    private static final ResourceLocation KAOLIN_CLAY_TEXTURE = new ResourceLocation("tfc", "block/white_kaolin_clay");
    private static final ResourceLocation FIRE_CLAY_TEXTURE = new ResourceLocation("tfc", "block/fire_clay_block");

    private static final float WIDTH_TEXELS = 7f;
    private static final float HEIGHT_TEXELS = 4f;
    private static final float LENGTH_TEXELS = 7.5f; // half of TFC's 15
    private static final float CELL = 0.25f;
    private static final float LAYER_HEIGHT = 0.125f;

    @Override
    public TextureAtlasSprite render(ClayPileBlockEntity pile, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay)
    {
        final int count = pile.getBlockState().getValue(ClayPileBlock.COUNT);
        final Function<ResourceLocation, TextureAtlasSprite> atlas =
            Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS);

        final float scale = 0.0625f / 2f;
        final float bevel = scale; // 1 texel
        final float minX = 0f;
        final float minY = 0f;
        final float minZ = 0f;
        final float maxX = scale * WIDTH_TEXELS;
        final float maxY = scale * HEIGHT_TEXELS;
        final float maxZ = scale * LENGTH_TEXELS;

        TextureAtlasSprite particle = null;
        for (int i = 0; i < count; i++)
        {
            final ItemStack stack = pile.stackAt(i);
            final TextureAtlasSprite sprite = atlas.apply(textureFor(stack));
            particle = sprite;

            final int layer = i / ClayPileBlock.PER_LAYER;
            final int indexInLayer = i % ClayPileBlock.PER_LAYER;
            final float x = (indexInLayer % 4) * CELL;
            final float z = (indexInLayer / 4) * CELL;
            final float y = layer * LAYER_HEIGHT;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            RenderHelpers.renderTexturedTrapezoidalCuboid(
                poseStack,
                buffer,
                sprite,
                packedLight,
                packedOverlay,
                minX,
                maxX,
                minZ,
                maxZ,
                minX + bevel,
                maxX - bevel,
                minZ + bevel,
                maxZ - bevel,
                minY,
                maxY,
                WIDTH_TEXELS,
                HEIGHT_TEXELS,
                LENGTH_TEXELS,
                false
            );

            poseStack.popPose();
        }

        return particle != null ? particle : atlas.apply(VANILLA_CLAY_TEXTURE);
    }

    static ResourceLocation textureFor(ItemStack stack)
    {
        if (stack.is(TFCItems.KAOLIN_CLAY.get()))
        {
            return KAOLIN_CLAY_TEXTURE;
        }
        if (stack.is(TFCItems.FIRE_CLAY.get()))
        {
            return FIRE_CLAY_TEXTURE;
        }
        // Vanilla clay_ball and any unexpected fallback
        if (stack.is(Items.CLAY_BALL) || stack.isEmpty())
        {
            return VANILLA_CLAY_TEXTURE;
        }
        return VANILLA_CLAY_TEXTURE;
    }

    @Override
    public BlockEntityType<ClayPileBlockEntity> type()
    {
        return ModBlockEntities.CLAY_PILE.get();
    }

    @Override
    public int faces(ClayPileBlockEntity blockEntity)
    {
        return blockEntity.getBlockState().getValue(ClayPileBlock.COUNT) * 6;
    }
}
