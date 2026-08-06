package com.minerguy341.morefloorstorage;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class MFSConfig
{
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static
    {
        final Pair<Server, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    public static final class Server
    {
        public final ModConfigSpec.BooleanValue enableClayPiles;
        public final ModConfigSpec.BooleanValue enableOrePiles;
        public final ModConfigSpec.BooleanValue enableToolLeaning;
        public final ModConfigSpec.BooleanValue enableSneakPickup;
        public final ModConfigSpec.BooleanValue sneakPickupWholeStack;
        public final ModConfigSpec.DoubleValue interactionRange;

        private Server(ModConfigSpec.Builder builder)
        {
            builder.push("piles");
            enableClayPiles = builder
                .comment("If clay-type items can be stacked into clay piles on the ground, the way ingots stack into ingot piles.")
                .define("enableClayPiles", true);
            enableOrePiles = builder
                .comment("If ore can be stacked into ore piles on the ground. Covers TerraFirmaCraft's small native",
                    "deposits and its poor, normal and rich graded ores.")
                .define("enableOrePiles", true);
            builder.pop();

            builder.push("tool_leaning");
            enableToolLeaning = builder
                .comment("If tools can be leaned up against the side of a block, in the style of Vintage Story.")
                .define("enableToolLeaning", true);
            builder.pop();

            builder.push("floor_storage");
            enableSneakPickup = builder
                .comment("If sneak + TerraFirmaCraft's floor storage key picks items back up out of floor storage.")
                .define("enableSneakPickup", true);
            sneakPickupWholeStack = builder
                .comment("If sneak + the floor storage key empties the whole pile at once, rather than taking a single item.")
                .define("sneakPickupWholeStack", false);
            interactionRange = builder
                .comment("How far away, in blocks, the server will look for floor storage to pick up from.")
                .defineInRange("interactionRange", 5.0d, 1.0d, 16.0d);
            builder.pop();
        }
    }
}
