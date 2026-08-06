package com.minerguy341.morefloorstorage.common.block;

/**
 * The shape of a clay pile, shared by the block's voxel shape and the renderer so the outline always
 * matches what you can see.
 * <p>
 * A pile holds 64 items, filled bottom up in six square layers that get smaller as they go, so a full
 * pile is a stepped pyramid and a partial one is however much of that pyramid has been built so far.
 * <pre>
 *   layer 5              1     1x1     14px
 *   layer 4            2 2 2   2x2     12px
 *   layer 3          3 3 3 3   3x3     10px
 *   layer 2        3 3 3 3 3   3x3      7px
 *   layer 1      4 4 4 4 4 4   4x4      5px
 *   layer 0    5 5 5 5 5 5 5   5x5      3px
 * </pre>
 */
public final class ClayPileLayout
{
    public static final int LAYERS = 6;

    /** Items per row in each layer; the layer holds the square of this. */
    public static final int[] GRID = {5, 4, 3, 3, 2, 1};

    /** Running total of items up to and including each layer. The last entry is the pile's capacity. */
    public static final int[] CUMULATIVE = {25, 41, 50, 59, 63, 64};

    public static final int MAX_ITEMS = CUMULATIVE[LAYERS - 1];

    /** Distance between the centres of the outermost items in a layer, in blocks. */
    private static final float[] SPAN = {0.75f, 0.56f, 0.38f, 0.38f, 0.20f, 0f};

    /** Height at which each layer's items sit, in blocks. */
    private static final float[] HEIGHT = {0.03f, 0.17f, 0.31f, 0.45f, 0.59f, 0.73f};

    /** Top of each layer's collision box, in pixels. Layer {@code n} starts where layer {@code n - 1} ends. */
    private static final int[] TOP = {3, 5, 7, 10, 12, 14};

    /** How far each layer's collision box is pulled in from the block edge, in pixels. */
    private static final int[] INSET = {0, 2, 3, 3, 4, 6};

    /**
     * @param index a zero-based position in the pile, in the order items were added
     * @return which layer that item belongs to
     */
    public static int layerOf(int index)
    {
        for (int layer = 0; layer < LAYERS; layer++)
        {
            if (index < CUMULATIVE[layer])
            {
                return layer;
            }
        }
        return LAYERS - 1;
    }

    /**
     * @return the position of {@code index} within its own layer, counting from zero
     */
    public static int indexInLayer(int index)
    {
        final int layer = layerOf(index);
        return layer == 0 ? index : index - CUMULATIVE[layer - 1];
    }

    /**
     * @param coordinate a row or column within the layer's grid
     * @return the offset of that row or column from the centre of the block, in blocks
     */
    public static float offsetInLayer(int layer, int coordinate)
    {
        final int grid = GRID[layer];
        return grid == 1 ? 0f : ((float) coordinate / (grid - 1) - 0.5f) * SPAN[layer];
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

    public static int insetOf(int layer)
    {
        return INSET[layer];
    }

    private ClayPileLayout() {}
}
