package com.minerguy341.tfcmorefloorstorage;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client config knobs for TFC ingot / double-ingot pile mesh geometry.
 * <p>
 * TFC defaults (vanilla look) are documented beside each value. Changing these only affects
 * how piles are drawn; block capacity ({@code COUNT} / {@code DOUBLE_COUNT}) is unchanged.
 */
public final class IngotMeshClientConfig
{
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue MATCH_VANILLA_SCALE_QUIRK;
    public static final ForgeConfigSpec.DoubleValue SIZE_SCALE;
    public static final ForgeConfigSpec.DoubleValue LAYER_HEIGHT_SCALE;

    public static final ForgeConfigSpec.DoubleValue INGOT_WIDTH_TEXELS;
    public static final ForgeConfigSpec.DoubleValue INGOT_HEIGHT_TEXELS;
    public static final ForgeConfigSpec.DoubleValue INGOT_LENGTH_TEXELS;
    public static final ForgeConfigSpec.DoubleValue INGOT_INSET_TEXELS;

    public static final ForgeConfigSpec.DoubleValue DOUBLE_WIDTH_TEXELS;
    public static final ForgeConfigSpec.DoubleValue DOUBLE_HEIGHT_TEXELS;
    public static final ForgeConfigSpec.DoubleValue DOUBLE_LENGTH_TEXELS;
    public static final ForgeConfigSpec.DoubleValue DOUBLE_INSET_TEXELS;

    public static final ForgeConfigSpec.DoubleValue TRAPEZOID_BEVEL_TEXELS;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
            "Experimental controls for TFC's procedural ingot pile meshes.",
            "Piles bake static quads; after editing this file press F3+T (or relog) so models rebuild.",
            "Vanilla reference: ~7x4x15 texels, layers of 8 on a 4x2 grid, alternating 90° rotation."
        ).push("ingot_mesh");

        ENABLED = builder
            .comment("If true, replace TFC ingot/double-ingot pile mesh rendering with this mod's geometry.")
            .define("enabled", true);

        MATCH_VANILLA_SCALE_QUIRK = builder
            .comment(
                "TFC computes maxX/Y/Z as scale * (min + size) where min is already scaled.",
                "Keep true to match stock TFC silhouettes exactly when using default texel sizes.",
                "Set false for cleaner math: max = scale * (inset + size)."
            )
            .define("matchVanillaScaleQuirk", true);

        SIZE_SCALE = builder
            .comment("Multiplies ingot width/height/length (and bevel). 1.0 = TFC size. Try 0.85 for denser-looking bars.")
            .defineInRange("sizeScale", 1.0D, 0.25D, 2.0D);

        LAYER_HEIGHT_SCALE = builder
            .comment("Multiplies vertical spacing between layers. 1.0 = TFC spacing.")
            .defineInRange("layerHeightScale", 1.0D, 0.25D, 2.0D);

        builder.push("single_ingot");
        INGOT_WIDTH_TEXELS = builder.comment("Default 7").defineInRange("widthTexels", 7.0D, 1.0D, 32.0D);
        INGOT_HEIGHT_TEXELS = builder.comment("Default 4").defineInRange("heightTexels", 4.0D, 1.0D, 32.0D);
        INGOT_LENGTH_TEXELS = builder.comment("Default 15").defineInRange("lengthTexels", 15.0D, 1.0D, 32.0D);
        INGOT_INSET_TEXELS = builder.comment("Default 0.5 — pads the ingot inside its cell").defineInRange("insetTexels", 0.5D, 0.0D, 8.0D);
        builder.pop();

        builder.push("double_ingot");
        DOUBLE_WIDTH_TEXELS = builder.comment("Default 10").defineInRange("widthTexels", 10.0D, 1.0D, 32.0D);
        DOUBLE_HEIGHT_TEXELS = builder.comment("Default 5").defineInRange("heightTexels", 5.0D, 1.0D, 32.0D);
        DOUBLE_LENGTH_TEXELS = builder.comment("Default 15").defineInRange("lengthTexels", 15.0D, 1.0D, 32.0D);
        DOUBLE_INSET_TEXELS = builder.comment("Default 0.5").defineInRange("insetTexels", 0.5D, 0.0D, 8.0D);
        builder.pop();

        TRAPEZOID_BEVEL_TEXELS = builder
            .comment("Top-face inset used by renderTexturedTrapezoidalCuboid. Default 1 texel (TFC passes +scale / -scale).")
            .defineInRange("trapezoidBevelTexels", 1.0D, 0.0D, 4.0D);

        builder.pop();
        SPEC = builder.build();
    }

    private IngotMeshClientConfig() {}

    public static void logActiveSettings()
    {
        if (!ENABLED.get())
        {
            TFCMoreFloorStorage.LOGGER.info("Ingot mesh override disabled; TFC vanilla meshes are used.");
            return;
        }
        TFCMoreFloorStorage.LOGGER.info(
            "Ingot mesh override ON: sizeScale={}, layerHeightScale={}, single={}x{}x{}, double={}x{}x{}, quirk={}",
            SIZE_SCALE.get(),
            LAYER_HEIGHT_SCALE.get(),
            INGOT_WIDTH_TEXELS.get(),
            INGOT_HEIGHT_TEXELS.get(),
            INGOT_LENGTH_TEXELS.get(),
            DOUBLE_WIDTH_TEXELS.get(),
            DOUBLE_HEIGHT_TEXELS.get(),
            DOUBLE_LENGTH_TEXELS.get(),
            MATCH_VANILLA_SCALE_QUIRK.get()
        );
    }
}
