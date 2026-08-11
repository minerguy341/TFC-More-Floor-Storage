package com.minerguy341.morefloorstorage.common.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

/**
 * A run of piles that behaves as a single pile: a solid rectangle of same-kind piles, at least two
 * blocks on each side and at most {@link #MAX_SPAN}. Four piles in a square merge, and so does a three
 * by two, a five by three, and anything else that fills its rectangle - which is what lets a heap fill
 * the floor of a pit rather than being stuck in one block.
 * <p>
 * Which rectangle a pile belongs to has to be something every member works out for itself and agrees on,
 * or two piles would draw overlapping pyramids that disagree about where the apex is. So the answer is
 * derived from the run of touching piles as a whole, which is the same set whichever member you start
 * from: the biggest rectangle in it is taken as one pile, then the biggest in what is left, and so on
 * until no rectangle remains. A pile beside a two by two therefore leaves the square alone and stays a
 * loose pile until it completes a bigger rectangle.
 * <p>
 * A run of more than {@link #MAX_SPAN} squared piles, or one spread wider than {@link #MAX_SPAN} in
 * either direction, is more than one pile can be and does not merge at all.
 */
public record PileGroup(BlockPos origin, int spanX, int spanZ)
{
    /** The most blocks a merged pile spans in one direction. */
    public static final int MAX_SPAN = 5;

    /** The most piles that can touch each other and still be worth resolving into rectangles. */
    private static final int MAX_MEMBERS = MAX_SPAN * MAX_SPAN;

    public int size()
    {
        return spanX * spanZ;
    }

    public BlockPos member(int cellX, int cellZ)
    {
        return origin.offset(cellX, 0, cellZ);
    }

    /** This block's column within the group, counting from the minimum corner. */
    public int cellX(BlockPos pos)
    {
        return pos.getX() - origin.getX();
    }

    public int cellZ(BlockPos pos)
    {
        return pos.getZ() - origin.getZ();
    }

    public void forEach(Consumer<BlockPos> action)
    {
        for (int cellX = 0; cellX < spanX; cellX++)
        {
            for (int cellZ = 0; cellZ < spanZ; cellZ++)
            {
                action.accept(member(cellX, cellZ));
            }
        }
    }

    /** The members along one edge of the group, which are the ones a wall on that side backs onto. */
    public List<BlockPos> edge(Direction direction)
    {
        final List<BlockPos> members = new ArrayList<>();
        switch (direction)
        {
            case NORTH -> { for (int x = 0; x < spanX; x++) members.add(member(x, 0)); }
            case SOUTH -> { for (int x = 0; x < spanX; x++) members.add(member(x, spanZ - 1)); }
            case WEST -> { for (int z = 0; z < spanZ; z++) members.add(member(0, z)); }
            case EAST -> { for (int z = 0; z < spanZ; z++) members.add(member(spanX - 1, z)); }
            default -> throw new IllegalArgumentException("Not a horizontal direction: " + direction);
        }
        return members;
    }

    /**
     * @return the merged pile {@code pos} belongs to, or {@code null} if it stands on its own.
     */
    public static @Nullable PileGroup at(BlockGetter level, BlockPos pos)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof PileBlock kind))
        {
            return null;
        }
        // Almost every pile is a lone one, and this settles those in four lookups rather than a walk
        if (!hasNeighbour(level, pos, kind))
        {
            return null;
        }

        final List<BlockPos> run = touching(level, pos, kind);
        if (run == null || run.size() < 4) // Nothing smaller than a two by two is a rectangle worth having
        {
            return null;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos cell : run)
        {
            minX = Math.min(minX, cell.getX());
            maxX = Math.max(maxX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        final int width = maxX - minX + 1;
        final int depth = maxZ - minZ + 1;
        if (width > MAX_SPAN || depth > MAX_SPAN)
        {
            return null; // Spread too wide to be one pile, however many blocks it is
        }

        final boolean[][] present = new boolean[width][depth];
        for (BlockPos cell : run)
        {
            present[cell.getX() - minX][cell.getZ() - minZ] = true;
        }

        // Biggest rectangle first, then the biggest in what it leaves, until one of them holds this pile
        while (true)
        {
            final int[] best = largestRectangle(present, width, depth);
            if (best == null)
            {
                return null;
            }
            final BlockPos corner = new BlockPos(minX + best[0], pos.getY(), minZ + best[1]);
            final PileGroup group = new PileGroup(corner, best[2], best[3]);
            if (group.cellX(pos) >= 0 && group.cellX(pos) < group.spanX()
                && group.cellZ(pos) >= 0 && group.cellZ(pos) < group.spanZ())
            {
                return group;
            }
            for (int x = best[0]; x < best[0] + best[2]; x++)
            {
                for (int z = best[1]; z < best[1] + best[3]; z++)
                {
                    present[x][z] = false;
                }
            }
        }
    }

    private static boolean hasNeighbour(BlockGetter level, BlockPos pos, PileBlock kind)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (level.getBlockState(pos.relative(direction)).is(kind))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return every pile of this kind reachable from {@code pos} by touching sides, or {@code null} if
     * there are more of them than could ever make up one pile.
     */
    private static @Nullable List<BlockPos> touching(BlockGetter level, BlockPos pos, PileBlock kind)
    {
        final List<BlockPos> found = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> pending = new ArrayDeque<>();
        seen.add(pos);
        pending.add(pos);
        while (!pending.isEmpty())
        {
            final BlockPos cell = pending.removeFirst();
            found.add(cell);
            if (found.size() > MAX_MEMBERS)
            {
                return null;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos next = cell.relative(direction);
                if (level.getBlockState(next).is(kind) && seen.add(next))
                {
                    pending.add(next);
                }
            }
        }
        return found;
    }

    /**
     * @return {@code minX, minZ, spanX, spanZ} of the largest filled rectangle, or {@code null} if none
     * is at least two by two. Ties go to the wider one, then to the one nearest the minimum corner,
     * which is what makes the choice the same for every member.
     */
    private static int @Nullable [] largestRectangle(boolean[][] present, int width, int depth)
    {
        int[] best = null;
        for (int x = 0; x < width; x++)
        {
            for (int z = 0; z < depth; z++)
            {
                for (int spanX = 2; spanX <= MAX_SPAN && x + spanX <= width; spanX++)
                {
                    for (int spanZ = 2; spanZ <= MAX_SPAN && z + spanZ <= depth; spanZ++)
                    {
                        if (!filled(present, x, z, spanX, spanZ))
                        {
                            break; // Nothing deeper at this width can be filled either
                        }
                        // Strictly better only, so the scan order settles ties towards the corner
                        if (best == null || spanX * spanZ > best[2] * best[3]
                            || (spanX * spanZ == best[2] * best[3] && spanX > best[2]))
                        {
                            best = new int[] {x, z, spanX, spanZ};
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean filled(boolean[][] present, int x, int z, int spanX, int spanZ)
    {
        for (int dx = 0; dx < spanX; dx++)
        {
            for (int dz = 0; dz < spanZ; dz++)
            {
                if (!present[x + dx][z + dz])
                {
                    return false;
                }
            }
        }
        return true;
    }
}
