package com.minerguy341.morefloorstorage.common.block;

import java.util.Arrays;
import java.util.Comparator;

import net.minecraft.util.Mth;

/**
 * The shape of a pile, shared by the block's voxel shape and the renderer so the outline always matches
 * what you can see. Every kind of pile uses the same one.
 * <p>
 * Items sit on a grid, one lump every {@link #SPACING} of a block, in six layers. Left to itself a heap
 * slumps, so each layer up holds a smaller square than the one below and the pile is a stepped pyramid.
 * Walls stop it slumping: for every side that is walled in, each layer keeps one more row before it has
 * to start tapering, up to the full width of the base. So a pile against a wall holds more than one in
 * the open, a pile in a corner more again, and a pile in a pit is a straight column.
 * <pre>
 *   walls   layer grids     capacity
 *     0     5 4 3 3 2 1        64
 *     1     5 5 4 4 3 2        95
 *     2     5 5 5 5 4 3       125
 *     3     5 5 5 5 5 4       141
 *     4     5 5 5 5 5 5       150
 * </pre>
 * Four piles in a two by two are one pile spanning both blocks: the same layers, with the grid doubled.
 * Doubling a grid quadruples it, which is exactly four members' worth, so each block contributes its own
 * items as one quadrant and nothing has to move between block entities.
 */
public final class PileLayout
{
    public static final int LAYERS = 6;

    /** Rows across the widest layer, and so the most any layer can be widened to. */
    public static final int BASE_GRID = 5;

    /** The most sides that can be walled in. */
    public static final int MAX_WALLS = 4;

    /** Distance between the centres of neighbouring lumps, in blocks. */
    private static final float SPACING = 0.1875f;

    /** Half a lump, used to keep the outermost ones inside the block. */
    private static final float ITEM_HALF = 0.125f;

    /** Rows per layer with nothing holding the heap in. */
    private static final int[] GRID = {5, 4, 3, 3, 2, 1};

    /**
     * Height at which each layer's lumps stand, in blocks. Spaced slightly closer than a lump is tall,
     * so each layer settles into the one below rather than hovering over it.
     */
    private static final float[] HEIGHT = {0.0f, 0.155f, 0.31f, 0.465f, 0.62f, 0.775f};

    /** Top of each layer's collision box, in pixels. Layer {@code n} starts where {@code n - 1} ends. */
    private static final int[] TOP = {3, 6, 8, 11, 13, 16};

    /** Running item total up to and including each layer, per wall count. */
    private static final int[][] CUMULATIVE = buildCumulative();

    /** The nine directions a pile can heap towards, as {@code (leanX + 1) * 3 + (leanZ + 1)}. */
    public static final int LEAN_NONE = 4;

    /** Cells of each layer, ordered outwards from wherever the pile heaps towards. */
    private static final int[][][][] FILL_ORDER = buildFillOrder();

    /** How much a pile can hold when nothing is holding it in, and the most it can ever hold. */
    public static final int MIN_CAPACITY = CUMULATIVE[0][LAYERS - 1];
    public static final int MAX_CAPACITY = CUMULATIVE[MAX_WALLS][LAYERS - 1];

    // -----------------------------------------------------------------------------------------
    // Grid
    // -----------------------------------------------------------------------------------------

    /**
     * @param walls how many of the four sides are walled in
     * @return rows across this layer; every walled side buys one more before the taper starts
     */
    public static int gridOf(int layer, int walls)
    {
        return Math.min(BASE_GRID, GRID[layer] + Mth.clamp(walls, 0, MAX_WALLS));
    }

    public static int capacity(int walls)
    {
        return CUMULATIVE[Mth.clamp(walls, 0, MAX_WALLS)][LAYERS - 1];
    }

    private static int[][] buildCumulative()
    {
        final int[][] cumulative = new int[MAX_WALLS + 1][LAYERS];
        for (int walls = 0; walls <= MAX_WALLS; walls++)
        {
            int total = 0;
            for (int layer = 0; layer < LAYERS; layer++)
            {
                final int grid = Math.min(BASE_GRID, GRID[layer] + walls);
                total += grid * grid;
                cumulative[walls][layer] = total;
            }
        }
        return cumulative;
    }

    /**
     * @param index a zero-based position in the pile, in the order items were added
     * @return which layer that item belongs to
     */
    public static int layerOf(int index, int walls)
    {
        final int[] cumulative = CUMULATIVE[Mth.clamp(walls, 0, MAX_WALLS)];
        for (int layer = 0; layer < LAYERS; layer++)
        {
            if (index < cumulative[layer])
            {
                return layer;
            }
        }
        return LAYERS - 1;
    }

    /**
     * @return the position of {@code index} within its own layer, counting from zero
     */
    public static int indexInLayer(int index, int walls)
    {
        final int layer = layerOf(index, walls);
        return layer == 0 ? index : index - CUMULATIVE[Mth.clamp(walls, 0, MAX_WALLS)][layer - 1];
    }

    // -----------------------------------------------------------------------------------------
    // Where a lump sits
    // -----------------------------------------------------------------------------------------

    private static float spanOf(int grid)
    {
        return (grid - 1) * SPACING;
    }

    /**
     * @param coordinate a row or column within this layer's grid
     * @return its offset from the centre of the block, in blocks
     */
    public static float offsetInLayer(int layer, int coordinate, int walls)
    {
        final int grid = gridOf(layer, walls);
        return grid == 1 ? 0f : ((float) coordinate / (grid - 1) - 0.5f) * spanOf(grid);
    }

    /**
     * @param coordinate a row or column within the doubled grid of a group's layer
     * @return its offset from the centre of the two by two, in blocks
     */
    public static float offsetInMergedLayer(int layer, int coordinate, int walls)
    {
        final int grid = 2 * gridOf(layer, walls);
        return ((float) coordinate / (grid - 1) - 0.5f) * ((grid - 1) * SPACING);
    }

    public static float heightOf(int layer)
    {
        return HEIGHT[layer];
    }

    public static int topOf(int layer)
    {
        return TOP[layer];
    }

    public static int bottomOf(int layer)
    {
        return layer == 0 ? 0 : TOP[layer - 1];
    }

    /** How far a layer is pulled in from the edge of one block, in pixels. */
    public static int insetOf(int layer, int walls)
    {
        final int grid = gridOf(layer, walls);
        return Math.max(0, Math.round(16f * (0.5f - (spanOf(grid) / 2f + ITEM_HALF))));
    }

    /** How far a group's layer is pulled in from the edge of its 32 pixel wide footprint. */
    public static int mergedInsetOf(int layer, int walls)
    {
        final int grid = 2 * gridOf(layer, walls);
        final float halfExtent = (grid - 1) * SPACING / 2f + ITEM_HALF;
        return Math.max(0, Math.round(16f * (1f - halfExtent)));
    }

    // -----------------------------------------------------------------------------------------
    // Fill order
    // -----------------------------------------------------------------------------------------

    /**
     * @return the index used to look up a fill order, for a pile heaping towards {@code (leanX, leanZ)}
     */
    public static int lean(int leanX, int leanZ)
    {
        return (Mth.clamp(leanX, -1, 1) + 1) * 3 + (Mth.clamp(leanZ, -1, 1) + 1);
    }

    /**
     * @param indexInLayer a position within one layer, counting from zero
     * @return the cell it fills, as {@code row * grid + column}
     */
    public static int cellOf(int layer, int indexInLayer, int walls, int lean)
    {
        return FILL_ORDER[Mth.clamp(walls, 0, MAX_WALLS)][layer][lean][indexInLayer];
    }

    /**
     * A layer filled row-major would start in a corner, so a part-filled pile looked swept to one side.
     * Cells are instead ordered outwards from the pile's own middle, or from the side nearest its
     * neighbours when it has any, so piles put down next to each other heap together.
     */
    private static int[][][][] buildFillOrder()
    {
        final int[][][][] order = new int[MAX_WALLS + 1][LAYERS][9][];
        for (int walls = 0; walls <= MAX_WALLS; walls++)
        {
            for (int layer = 0; layer < LAYERS; layer++)
            {
                final int grid = Math.min(BASE_GRID, GRID[layer] + walls);
                final float middle = (grid - 1) / 2f;
                for (int leanX = -1; leanX <= 1; leanX++)
                {
                    for (int leanZ = -1; leanZ <= 1; leanZ++)
                    {
                        // Start from the middle, pushed to the edge in whichever way the pile leans
                        final double fromX = middle + leanX * middle;
                        final double fromZ = middle + leanZ * middle;
                        final Integer[] cells = new Integer[grid * grid];
                        for (int cell = 0; cell < cells.length; cell++)
                        {
                            cells[cell] = cell;
                        }
                        // Stable, so cells equally far out keep a predictable order around the ring
                        Arrays.sort(cells, Comparator.comparingDouble(cell -> {
                            final double x = (cell % grid) - fromX;
                            final double z = (double) (cell / grid) - fromZ;
                            return x * x + z * z;
                        }));
                        final int[] flat = new int[cells.length];
                        for (int cell = 0; cell < cells.length; cell++)
                        {
                            flat[cell] = cells[cell];
                        }
                        order[walls][layer][lean(leanX, leanZ)] = flat;
                    }
                }
            }
        }
        return order;
    }

    private PileLayout() {}
}
