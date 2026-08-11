package com.minerguy341.morefloorstorage.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.minerguy341.morefloorstorage.common.block.PileBlock;
import com.minerguy341.morefloorstorage.common.block.PileGroup;
import com.minerguy341.morefloorstorage.common.blockentity.PileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Placement half of the pile mechanic, for whichever kind of pile the held item belongs to. Mirrors how
 * TerraFirmaCraft handles ingot piling: sneak-click to add one to the pile you clicked, climbing the
 * column to the first level with room, and starting a new pile when the column is full.
 * <p>
 * Where the item actually lands depends on what it is landing in. Dropped into a pit that is closed in on
 * every side, material floods: it goes to the emptiest spot on the pit floor, starting a pile there if
 * there is not one yet, so the whole floor comes up together the way poured material would. Dropped onto
 * a merged group, it goes to the emptiest member so the shared pyramid grows evenly. Otherwise it goes
 * where it was put.
 */
public final class Piling
{
    /** How far up a column of piles a single click will look for room. */
    private static final int LIFT_LIMIT = 32;

    public static InteractionResult place(UseOnContext context, ItemStack stack, PileBlock pileBlock)
    {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        final BlockPos clicked = context.getClickedPos();
        final boolean clickedPile = level.getBlockState(clicked).is(pileBlock);

        // Clicking a pile adds to it; clicking anything else puts one against the face that was clicked
        BlockPos target = clickedPile ? clicked : new BlockPlaceContext(context).getClickedPos();

        // A click that lands nowhere is the block's own business, unless it was a pile that was clicked
        final InteractionResult nowhere = clickedPile ? InteractionResult.FAIL : InteractionResult.PASS;

        for (int lift = 0; lift < LIFT_LIMIT; lift++)
        {
            if (!level.getBlockState(target).is(pileBlock))
            {
                // Bare ground, so a new pile - if one can stand here at all
                return level.getBlockState(target).canBeReplaced()
                    ? startPile(level, target, player, stack, pileBlock)
                    : nowhere;
            }
            final BlockPos spot = roomAt(level, target, pileBlock);
            if (spot != null)
            {
                final BlockState spotState = level.getBlockState(spot);
                return spotState.is(pileBlock)
                    ? addToPile(level, spot, spotState, player, stack)
                    : startPile(level, spot, player, stack, pileBlock);
            }
            // Everything at this level is full, so the next one goes on top
            target = target.above();
        }
        return nowhere;
    }

    /**
     * @return where an item put into the pile at {@code pos} should actually go, or {@code null} if
     * everything this level has to offer is full.
     */
    private static @Nullable BlockPos roomAt(Level level, BlockPos pos, PileBlock pileBlock)
    {
        final List<BlockPos> spots = spread(level, pos, pileBlock);
        BlockPos emptiest = null;
        int fewest = Integer.MAX_VALUE;
        for (BlockPos spot : spots)
        {
            final BlockState spotState = level.getBlockState(spot);
            final int count = spotState.is(pileBlock) ? spotState.getValue(PileBlock.COUNT) : 0;
            // How much fits depends on where it stands: walls hold the heap in and let it take more
            if (spotState.is(pileBlock) && count >= PileBlock.capacityAt(level, spot))
            {
                continue;
            }
            if (count < fewest)
            {
                emptiest = spot;
                fewest = count;
            }
        }
        return emptiest;
    }

    /**
     * @return every spot an item put in at {@code pos} may land in: the floor of the pit it is standing
     * in, or failing that the members of its group, or failing that just itself.
     */
    private static List<BlockPos> spread(Level level, BlockPos pos, PileBlock pileBlock)
    {
        final List<BlockPos> pit = enclosedPit(level, pos, pileBlock);
        if (pit != null)
        {
            return pit;
        }
        final PileGroup group = PileGroup.at(level, pos);
        if (group != null)
        {
            final List<BlockPos> members = new ArrayList<>();
            group.forEach(members::add);
            return members;
        }
        return List.of(pos);
    }

    /**
     * Works out whether {@code pos} stands on the floor of a pit that is closed in on every side.
     * <p>
     * The floor is walked outwards over everything at this level that material could occupy - open space
     * with something solid under it, or a pile of this kind already standing there. Every step off that
     * floor has to run into a wall. One gap anywhere, and it is not a pit but a dip in the open ground,
     * so material stays where it was put rather than running off across the landscape.
     *
     * @return the cells of the pit floor, or {@code null} if this is not an enclosed pit.
     */
    private static @Nullable List<BlockPos> enclosedPit(Level level, BlockPos pos, PileBlock pileBlock)
    {
        final List<BlockPos> floor = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> pending = new ArrayDeque<>();
        seen.add(pos);
        pending.add(pos);
        while (!pending.isEmpty())
        {
            final BlockPos cell = pending.removeFirst();
            floor.add(cell);
            // A pit bigger than one pile can be is not one pile, so there is nothing to flood into
            if (floor.size() > PileGroup.MAX_SPAN * PileGroup.MAX_SPAN)
            {
                return null;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos next = cell.relative(direction);
                if (seen.contains(next))
                {
                    continue;
                }
                if (isFloor(level, next, pileBlock))
                {
                    seen.add(next);
                    pending.add(next);
                }
                else if (!level.getBlockState(next).isFaceSturdy(level, next, direction.getOpposite()))
                {
                    return null; // A way out, so not a pit
                }
            }
        }
        return floor;
    }

    /** Whether material could stand at this spot: room for it, and something solid holding it up. */
    private static boolean isFloor(Level level, BlockPos pos, PileBlock pileBlock)
    {
        final BlockState state = level.getBlockState(pos);
        if (!state.is(pileBlock) && !state.canBeReplaced())
        {
            return false;
        }
        final BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static InteractionResult startPile(Level level, BlockPos pos, @Nullable Player player, ItemStack stack, PileBlock pileBlock)
    {
        final BlockState state = pileBlock.defaultBlockState();
        if (!state.canSurvive(level, pos)) // Nothing to stand on, or nothing holding it up there
        {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide)
        {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof PileBlockEntity pile)
            {
                pile.addItem(takeOne(player, stack));
            }
        }
        playPlaceSound(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult addToPile(Level level, BlockPos pos, BlockState state, @Nullable Player player, ItemStack stack)
    {
        if (!level.isClientSide)
        {
            level.setBlock(pos, state.setValue(PileBlock.COUNT, state.getValue(PileBlock.COUNT) + 1), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof PileBlockEntity pile)
            {
                pile.addItem(takeOne(player, stack));
            }
        }
        playPlaceSound(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Splits a single item off the held stack, leaving creative-mode stacks alone.
     */
    private static ItemStack takeOne(@Nullable Player player, ItemStack stack)
    {
        if (player != null && player.isCreative())
        {
            return stack.copyWithCount(1);
        }
        return stack.split(1);
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState state)
    {
        final SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
    }

    private Piling() {}
}
