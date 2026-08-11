package com.minerguy341.morefloorstorage.compat.jade;

import java.util.LinkedHashMap;
import java.util.Map;

import com.minerguy341.morefloorstorage.MoreFloorStorage;
import com.minerguy341.morefloorstorage.common.block.PileBlock;
import com.minerguy341.morefloorstorage.common.blockentity.PileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * What a pile is holding, as one "{@code 12x Clay}" line per kind of item, and how full it is.
 * <p>
 * A merged two by two is one pile, so it is summarised as one: the four blocks are tallied together and
 * their capacities added up, which is also the quickest way to see that a group has actually formed.
 */
public enum PileContentsProvider implements IBlockComponentProvider
{
    INSTANCE;

    private static final ResourceLocation UID = MoreFloorStorage.id("pile_contents");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        final Level level = accessor.getLevel();
        final BlockPos pos = accessor.getPosition();
        final Map<Item, Integer> counts = new LinkedHashMap<>(); // Deterministic line order
        int capacity = 0;

        final BlockPos origin = PileBlock.groupOrigin(level, pos);
        if (origin == null)
        {
            capacity = PileBlock.capacityAt(level, pos);
            if (accessor.getBlockEntity() instanceof PileBlockEntity pile)
            {
                pile.countInto(counts);
            }
        }
        else
        {
            for (int dx = 0; dx < 2; dx++)
            {
                for (int dz = 0; dz < 2; dz++)
                {
                    final BlockPos member = origin.offset(dx, 0, dz);
                    capacity += PileBlock.capacityAt(level, member);
                    if (level.getBlockEntity(member) instanceof PileBlockEntity pile)
                    {
                        pile.countInto(counts);
                    }
                }
            }
        }

        PileBlockEntity.describe(counts, tooltip::add);

        int held = 0;
        for (int count : counts.values())
        {
            held += count;
        }
        tooltip.add(Component.translatable("morefloorstorage.tooltip.filled", held, capacity));
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }
}
