package com.minerguy341.tfcmorefloorstorage;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server config for tool leaning.
 */
public final class ToolLeaningConfig
{
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TOOL_LEANING;
    public static final ForgeConfigSpec.DoubleValue INTERACTION_RANGE;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("tool_leaning");
        ENABLE_TOOL_LEANING = builder
            .comment(
                "If tools can be leaned up against the side of a block, in the style of Vintage Story.",
                "Capacity uses the same size rules as TFC ground placed items:",
                "  - size ≤ tfc.server.maxPlacedItemSize → up to 4 tools per block",
                "  - larger, up to maxPlacedLargeItemSize → 1 tool alone",
                "  - bigger than that → cannot lean"
            )
            .define("enableToolLeaning", true);
        INTERACTION_RANGE = builder
            .comment("How far away, in blocks, the server will look when leaning a tool.")
            .defineInRange("interactionRange", 5.0d, 1.0d, 16.0d);
        builder.pop();
        SPEC = builder.build();
    }

    private ToolLeaningConfig() {}
}
