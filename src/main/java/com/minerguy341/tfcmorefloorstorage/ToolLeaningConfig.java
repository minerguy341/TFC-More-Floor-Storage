package com.minerguy341.tfcmorefloorstorage;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server config for tool leaning (ported from Claude's morefloorstorage branch).
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
            .comment("If tools can be leaned up against the side of a block, in the style of Vintage Story.")
            .define("enableToolLeaning", true);
        INTERACTION_RANGE = builder
            .comment("How far away, in blocks, the server will look when leaning a tool.")
            .defineInRange("interactionRange", 5.0d, 1.0d, 16.0d);
        builder.pop();
        SPEC = builder.build();
    }

    private ToolLeaningConfig() {}
}
