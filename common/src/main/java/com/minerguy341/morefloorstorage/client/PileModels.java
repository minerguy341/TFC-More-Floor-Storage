package com.minerguy341.morefloorstorage.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.compat.tfc.IngotMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Per-item models for the lumps in a pile.
 * <p>
 * An item gets its own lump geometry by having a model at
 * {@code morefloorstorage:block/pile/<item namespace>/<item path>} - so {@code tfc:ore/rich_native_copper}
 * looks for {@code morefloorstorage:block/pile/tfc/ore/rich_native_copper}. Anything found under that
 * directory is loaded, whether it ships with this mod or arrives in a resource pack, so adding or
 * replacing a lump model needs no code and no registration. A model with no geometry in it, just a
 * particle texture, keeps {@link IngotMesh}'s bar and only changes what it is drawn in. Items with
 * neither fall back to the bar in their own icon.
 * <p>
 * A model is measured rather than assumed: it is placed by its own bounding box, so it need not be
 * centred in its block or sit at any particular scale, and it is only shrunk if it would otherwise
 * overflow a lump's share of the pile. Two models of different sizes stay different sizes.
 */
public final class PileModels
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** No trailing slash: ResourceManager.listResources rejects one, and ids are built by joining. */
    private static final String DIRECTORY = "block/pile";

    /**
     * The width a lump model is authored at, matching TerraFirmaCraft's groundcover models, and the width
     * one that wide comes out. A bigger model renders bigger. Deliberately its own number rather than the
     * built-in lump's: what a chunk of ore should look like has nothing to do with how big a bar of clay
     * is.
     */
    private static final float REFERENCE_SPAN = 6f / 16f;
    private static final float REFERENCE_WIDTH = 0.25f;

    /** Past this, a lump starts crowding its neighbours in the pile, so it gets shrunk to fit. */
    private static final float MAX_WIDTH = 0.30f;

    private static final Map<Item, Lump> CACHE = new IdentityHashMap<>();

    /**
     * What to draw for one item.
     * <p>
     * A {@code model} is geometry of its own, together with the transform that seats it: centred on its
     * spot and standing on it. With no model there is still a {@code sprite} to fall back on, which
     * {@link IngotMesh} wraps round its bar.
     */
    public record Lump(@Nullable BakedModel model, @Nullable TextureAtlasSprite sprite,
                       float scale, float offsetX, float offsetY, float offsetZ)
    {
        private static final Lump NOTHING = new Lump(null, null, 1f, 0f, 0f, 0f);

        public void applyTo(PoseStack pose)
        {
            pose.scale(scale, scale, scale);
            pose.translate(offsetX, offsetY, offsetZ);
        }
    }

    public static ResourceLocation modelIdFor(ResourceLocation itemId)
    {
        return MoreFloorStorage.id(DIRECTORY + "/" + itemId.getNamespace() + "/" + itemId.getPath());
    }

    /**
     * Standalone models are not baked unless something asks for them, and nothing references these, so
     * every file present under the directory is registered by hand.
     */
    public static void registerAdditional(ModelEvent.RegisterAdditional event)
    {
        // Lump models are cosmetic, and this walks whatever resource packs happen to be installed. A
        // failure here used to take the whole client down, and misleadingly: the broken resource reload
        // restarted mod loading, which made another mod's non-idempotent setup run twice and crash
        // first. Better to lose the models than the game.
        try
        {
            discover(event);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to discover pile lump models; piles will fall back to plain lumps", e);
        }
    }

    private static void discover(ModelEvent.RegisterAdditional event)
    {
        Minecraft.getInstance().getResourceManager()
            .listResources("models/" + DIRECTORY, path -> path.getPath().endsWith(".json"))
            .keySet()
            .forEach(path -> {
                // A pack adds lump models by writing into this mod's namespace, the same way it would
                // override one of ours, so anything else here is not addressed to us
                if (!path.getNamespace().equals(MoreFloorStorage.MOD_ID))
                {
                    return;
                }
                final String withoutExtension = path.getPath()
                    .substring("models/".length(), path.getPath().length() - ".json".length());
                event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(path.getNamespace(), withoutExtension)));
            });
    }

    public static void onBakingCompleted(ModelEvent.BakingCompleted event)
    {
        CACHE.clear();
    }

    /**
     * @return what to draw for this item. Never {@code null}: an item with nothing of its own gets a
     * {@link Lump} with neither model nor sprite, and the caller falls back to the item's own icon.
     */
    public static Lump lookup(ItemStack stack, RandomSource random)
    {
        return CACHE.computeIfAbsent(stack.getItem(), item -> measure(item, random));
    }

    private static Lump measure(Item item, RandomSource random)
    {
        final ModelManager models = Minecraft.getInstance().getModelManager();
        final BakedModel model = models.getModel(
            ModelResourceLocation.standalone(modelIdFor(BuiltInRegistries.ITEM.getKey(item))));
        // getModel hands back the missing model rather than null, which caches the absence for free
        if (model == models.getMissingModel())
        {
            return Lump.NOTHING;
        }

        final float[] bounds = boundsOf(model, random);
        if (bounds == null)
        {
            // A model with no geometry in it, just a texture, says "draw the built-in bar in this". Clay
            // uses that to be drawn in the clay block's texture rather than in the clay ball icon, which
            // is a blob with empty corners and looks like one when it is wrapped round a bar.
            return new Lump(null, model.getParticleIcon(), 1f, 0f, 0f, 0f);
        }

        final float width = Math.max(bounds[3] - bounds[0], bounds[5] - bounds[2]);
        float scale = REFERENCE_WIDTH / REFERENCE_SPAN;
        if (width * scale > MAX_WIDTH && width > 0)
        {
            scale = MAX_WIDTH / width; // Only the oversized get trimmed; the rest keep their relative size
        }
        return new Lump(model, model.getParticleIcon(), scale,
            -(bounds[0] + bounds[3]) / 2f,  // centred horizontally on its spot in the pile
            -bounds[1],                     // standing on the layer, not floating above it
            -(bounds[2] + bounds[5]) / 2f);
    }

    /**
     * The model's own extent, as {@code minX, minY, minZ, maxX, maxY, maxZ}, read back off its baked
     * quads - the only way to know how big someone else's model is.
     */
    private static float @Nullable [] boundsOf(BakedModel model, RandomSource random)
    {
        final List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random));
        for (Direction direction : Direction.values())
        {
            quads.addAll(model.getQuads(null, direction, random));
        }
        if (quads.isEmpty())
        {
            return null;
        }

        final float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (BakedQuad quad : quads)
        {
            final int[] vertices = quad.getVertices();
            final int stride = vertices.length / 4; // Position is always the first three ints of a vertex
            for (int vertex = 0; vertex < 4; vertex++)
            {
                for (int axis = 0; axis < 3; axis++)
                {
                    final float value = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                    bounds[axis] = Math.min(bounds[axis], value);
                    bounds[axis + 3] = Math.max(bounds[axis + 3], value);
                }
            }
        }
        return bounds;
    }

    private PileModels() {}
}
