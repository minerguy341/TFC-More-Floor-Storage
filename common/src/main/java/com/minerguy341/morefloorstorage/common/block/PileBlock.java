package com.minerguy341.morefloorstorage.common.block;

import java.util.function.Supplier;

import com.minerguy341.morefloorstorage.common.blockentity.PileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * TerraFirmaCraft's ingot pile, generalised. Items sneak-placed on the ground stack into a pile of up to
 * {@link #MAX_ITEMS} instead of being placed as a single loose item, and clicking the pile takes the top
 * one back off. The pile grows as it fills, in the stepped pyramid described by {@link PileLayout}.
 * <p>
 * A pyramid will not balance on the point of another one, so piles do not stack. The way up is to fill a
 * two by two: four full piles merge into a single pyramid spanning all four blocks, which is broad enough
 * to build the next tier on. Take an item back off any of the four and the merge breaks, dropping
 * whatever was resting on it.
 * <p>
 * One instance is registered per kind of pile - see {@link MFSBlocks} - each with its own block entity
 * type and its own tag of items it accepts. Only piles of the same kind merge with each other, though a
 * pile of any kind may be built on top of any merged group.
 */
public class PileBlock extends Block implements EntityBlock
{
    public static final int MAX_ITEMS = PileLayout.MAX_ITEMS;

    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, MAX_ITEMS);

    /**
     * The four two by twos that could contain a given pile, by their minimum corner, in a fixed scan
     * order. The order only has to be stable - {@link #mergeOrigin} makes every member of a group agree
     * on which group they are in.
     */
    private static final int[][] GROUP_CANDIDATES = {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}};

    /** One shape per layer, each the union of that layer's step and every step below it. */
    private static final VoxelShape[] SHAPES = buildShapes();

    /** The full pyramid's shape as seen by each quadrant of a merged two by two, indexed {@code [x][z]}. */
    private static final VoxelShape[][] MERGED_SHAPES = buildMergedShapes();

    private static VoxelShape[] buildShapes()
    {
        final VoxelShape[] shapes = new VoxelShape[PileLayout.LAYERS];
        VoxelShape shape = Shapes.empty();
        for (int layer = 0; layer < PileLayout.LAYERS; layer++)
        {
            final int inset = PileLayout.insetOf(layer);
            shape = Shapes.or(shape, box(
                inset, PileLayout.bottomOf(layer), inset,
                16 - inset, PileLayout.topOf(layer), 16 - inset));
            shapes[layer] = shape.optimize();
        }
        return shapes;
    }

    private static VoxelShape[][] buildMergedShapes()
    {
        final VoxelShape[][] shapes = new VoxelShape[2][2];
        for (int quadrantX = 0; quadrantX < 2; quadrantX++)
        {
            for (int quadrantZ = 0; quadrantZ < 2; quadrantZ++)
            {
                VoxelShape shape = Shapes.empty();
                for (int layer = 0; layer < PileLayout.LAYERS; layer++)
                {
                    // The merged pyramid is 32 pixels across; clip each layer to this block's half of it
                    final int inset = PileLayout.mergedInsetOf(layer);
                    final double minX = Mth.clamp(inset - quadrantX * 16, 0, 16);
                    final double maxX = Mth.clamp(32 - inset - quadrantX * 16, 0, 16);
                    final double minZ = Mth.clamp(inset - quadrantZ * 16, 0, 16);
                    final double maxZ = Mth.clamp(32 - inset - quadrantZ * 16, 0, 16);
                    if (maxX > minX && maxZ > minZ)
                    {
                        shape = Shapes.or(shape, box(
                            minX, PileLayout.bottomOf(layer), minZ,
                            maxX, PileLayout.topOf(layer), maxZ));
                    }
                }
                shapes[quadrantX][quadrantZ] = shape.optimize();
            }
        }
        return shapes;
    }

    /**
     * Works out whether this pile is part of a two by two of full piles that has merged into one larger
     * pyramid.
     * <p>
     * A pile can sit in up to four different two by twos, so the first valid one in a fixed scan order
     * wins - and every member of that group has to independently pick the same group, otherwise a run of
     * piles longer than two would produce overlapping pyramids that disagree about where the apex is.
     * Piles left over from a larger arrangement simply stay as individual pyramids.
     *
     * @return the minimum corner of the merged group, or {@code null} if this pile is on its own.
     */
    public static @Nullable BlockPos mergeOrigin(BlockGetter level, BlockPos pos)
    {
        // Cheap rejection first: this runs from getShape, so most calls should cost a single state read
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PileBlock pileBlock) || state.getValue(COUNT) < MAX_ITEMS)
        {
            return null;
        }

        final BlockPos origin = firstFullGroup(level, pos, pileBlock);
        if (origin == null)
        {
            return null;
        }
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                if (!origin.equals(firstFullGroup(level, origin.offset(dx, 0, dz), pileBlock)))
                {
                    return null;
                }
            }
        }
        return origin;
    }

    private static @Nullable BlockPos firstFullGroup(BlockGetter level, BlockPos pos, PileBlock pileBlock)
    {
        for (int[] candidate : GROUP_CANDIDATES)
        {
            final BlockPos origin = pos.offset(candidate[0], 0, candidate[1]);
            if (isFullGroup(level, origin, pileBlock))
            {
                return origin;
            }
        }
        return null;
    }

    private static boolean isFullGroup(BlockGetter level, BlockPos origin, PileBlock pileBlock)
    {
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                // Same kind of pile throughout: clay and ore sitting side by side are two pyramids
                final BlockState state = level.getBlockState(origin.offset(dx, 0, dz));
                if (!state.is(pileBlock) || state.getValue(COUNT) < MAX_ITEMS)
                {
                    return false;
                }
            }
        }
        return true;
    }

    private final Supplier<BlockEntityType<PileBlockEntity>> blockEntityType;

    /**
     * @param blockEntityType this kind of pile's own block entity type, supplied lazily because the type
     *                        is registered against this very block.
     */
    public PileBlock(Properties properties, Supplier<BlockEntityType<PileBlockEntity>> blockEntityType)
    {
        super(properties);
        this.blockEntityType = blockEntityType;
        registerDefaultState(getStateDefinition().any().setValue(COUNT, 1));
    }

    /**
     * Takes the top item off the topmost pile in this column and hands it to {@code player}.
     *
     * @param wholeColumn if the entire pile should be emptied rather than a single item.
     * @return {@code true} if anything was removed.
     */
    public boolean removeFromTop(Level level, BlockPos pos, Player player, boolean wholeColumn)
    {
        // Climb to the top of the column first - you always take off the top of the stack
        BlockPos topPos = pos;
        while (level.getBlockState(topPos.above()).is(this))
        {
            topPos = topPos.above();
        }

        final BlockState topState = level.getBlockState(topPos);
        if (!topState.is(this) || !(level.getBlockEntity(topPos) instanceof PileBlockEntity pile))
        {
            return false;
        }

        if (level.isClientSide)
        {
            return true; // Client only predicts the interaction; the server does the work
        }

        final int count = topState.getValue(COUNT);
        final int taken = wholeColumn ? count : 1;
        for (int i = 0; i < taken; i++)
        {
            final ItemStack stack = pile.removeTop();
            if (!stack.isEmpty() && !player.isCreative())
            {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }

        if (count - taken <= 0)
        {
            level.removeBlock(topPos, false);
        }
        else
        {
            level.setBlock(topPos, topState.setValue(COUNT, count - taken), Block.UPDATE_CLIENTS);
        }

        // This pile is no longer full, so anything the merged pyramid was holding up has to be rechecked
        updateSupportedPiles(level, topPos);

        final SoundType sound = topState.getSoundType();
        level.playSound(null, topPos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 4f, sound.getPitch() * 0.8f);
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        // Sneaking is how items go *into* the pile, and is handled by MFSInteractions
        if (player.isShiftKeyDown())
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return removeFromTop(level, pos, player, false)
            ? ItemInteractionResult.sidedSuccess(level.isClientSide)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        return removeFromTop(level, pos, player, false)
            ? InteractionResult.sidedSuccess(level.isClientSide)
            : InteractionResult.PASS;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos below = pos.below();
        final BlockState belowState = level.getBlockState(below);
        if (belowState.getBlock() instanceof PileBlock)
        {
            // A pyramid does not balance on the point of another one. The only way up is a two by two of
            // full piles that has merged into a wide pyramid, which is flat enough on top to build on.
            return mergeOrigin(level, below) != null;
        }
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (direction == Direction.DOWN)
        {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    /**
     * Rechecks every pile that might have been resting on this one. A pile above is supported by a whole
     * two by two, three quarters of which are diagonal neighbours that never get an ordinary block update,
     * so emptying one pile has to reach up and poke the piles above by hand.
     */
    private void updateSupportedPiles(Level level, BlockPos pos)
    {
        final BlockPos above = pos.above();
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                final BlockPos target = above.offset(dx, 0, dz);
                // Any kind of pile may rest on a merged group, and a scheduled tick only fires if it
                // names the block actually standing there
                if (level.getBlockState(target).getBlock() instanceof PileBlock pileAbove)
                {
                    level.scheduleTick(target, pileAbove, 1);
                }
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!canSurvive(state, level, pos))
        {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        // Creative breaks void the pile rather than showering the player in clay
        if (player.isCreative() && level.getBlockEntity(pos) instanceof PileBlockEntity pile)
        {
            pile.removeAll(stack -> {});
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof PileBlockEntity pile)
            {
                pile.removeAll(stack -> popResource(level, pos, stack));
            }
            updateSupportedPiles(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        final BlockPos origin = mergeOrigin(level, pos);
        if (origin != null)
        {
            return MERGED_SHAPES[pos.getX() - origin.getX()][pos.getZ() - origin.getZ()];
        }
        return SHAPES[PileLayout.layerOf(state.getValue(COUNT) - 1)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(COUNT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        // Contents are drawn by PileRenderer; the block itself has no baked geometry
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
    {
        return level.getBlockEntity(pos) instanceof PileBlockEntity pile ? pile.getPickedItemStack() : ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return blockEntityType.get().create(pos, state);
    }
}
