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
 * TerraFirmaCraft's ingot pile, generalised. Items sneak-placed on the ground stack into a pile instead
 * of being placed as a single loose item, and clicking the pile takes the top one back off. The pile
 * grows as it fills, in the stepped pyramid described by {@link PileLayout}.
 * <p>
 * How much fits depends on where it stands. A heap in the open slumps into a pyramid and holds 64, but
 * anything holding it in lets it stay wider for longer: 95 against a wall, 125 in a corner, 150 in a
 * pit, where it is a straight column. Widen the pit while it is in use and the heap slumps to suit:
 * whatever no longer fits trickles out through the gap, a few items a tick, until it does.
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
    /** The most any pile can hold, which is what the count property has to be able to express. */
    public static final int MAX_ITEMS = PileLayout.MAX_CAPACITY;

    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, MAX_ITEMS);

    /** Sides that must be walled for a pile to heap higher without a full group beneath it. */
    private static final int WALLS_TO_BRACE = 1;

    /**
     * How many items a slumping heap sheds per tick. Opening up a full pit can put nearly a hundred of
     * them over the line at once, and a heap that empties itself over a couple of seconds looks like a
     * heap slumping; dropping the lot in one tick looks like a bug and lands as one heap of entities.
     */
    private static final int SPILL_PER_TICK = 4;

    /** Ticks between one shed and the next while a heap is still slumping. */
    private static final int SPILL_INTERVAL = 2;

    /**
     * The four two by twos that could contain a given pile, by their minimum corner, in a fixed scan
     * order. The order only has to be stable - {@link #groupOrigin} makes every member of a group agree
     * on which group they are in.
     */
    private static final int[][] GROUP_CANDIDATES = {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}};

    /** One shape per layer, each the union of that layer's step and every step below it. */
    private static final VoxelShape[][] SHAPES = buildShapes();

    /**
     * A grouped pile's shape, indexed {@code [layer][quadrantX][quadrantZ]}. The outline spans the whole
     * two by two - deliberately reaching outside its own block - so a group reads and targets as one
     * pile. Collision keeps to the block it belongs to.
     */
    private static final VoxelShape[][][][] GROUP_OUTLINES = buildGroupShapes(false);
    private static final VoxelShape[][][][] GROUP_COLLISION = buildGroupShapes(true);

    private static VoxelShape[][] buildShapes()
    {
        final VoxelShape[][] shapes = new VoxelShape[PileLayout.MAX_WALLS + 1][PileLayout.LAYERS];
        for (int walls = 0; walls <= PileLayout.MAX_WALLS; walls++)
        {
            VoxelShape shape = Shapes.empty();
            for (int layer = 0; layer < PileLayout.LAYERS; layer++)
            {
                final int inset = PileLayout.insetOf(layer, walls);
                shape = Shapes.or(shape, box(
                    inset, PileLayout.bottomOf(layer), inset,
                    16 - inset, PileLayout.topOf(layer), 16 - inset));
                shapes[walls][layer] = shape.optimize();
            }
        }
        return shapes;
    }

    private static VoxelShape[][][][] buildGroupShapes(boolean clipToBlock)
    {
        final VoxelShape[][][][] shapes =
            new VoxelShape[PileLayout.MAX_WALLS + 1][PileLayout.LAYERS][2][2];
        for (int walls = 0; walls <= PileLayout.MAX_WALLS; walls++)
        {
        for (int quadrantX = 0; quadrantX < 2; quadrantX++)
        {
            for (int quadrantZ = 0; quadrantZ < 2; quadrantZ++)
            {
                VoxelShape shape = Shapes.empty();
                for (int layer = 0; layer < PileLayout.LAYERS; layer++)
                {
                    // The group's pyramid is 32 pixels across, offset so this block sits at the origin
                    final int inset = PileLayout.mergedInsetOf(layer, walls);
                    double minX = inset - quadrantX * 16;
                    double maxX = 32 - inset - quadrantX * 16;
                    double minZ = inset - quadrantZ * 16;
                    double maxZ = 32 - inset - quadrantZ * 16;
                    if (clipToBlock)
                    {
                        minX = Mth.clamp(minX, 0, 16);
                        maxX = Mth.clamp(maxX, 0, 16);
                        minZ = Mth.clamp(minZ, 0, 16);
                        maxZ = Mth.clamp(maxZ, 0, 16);
                    }
                    if (maxX > minX && maxZ > minZ)
                    {
                        shape = Shapes.or(shape, box(
                            minX, PileLayout.bottomOf(layer), minZ,
                            maxX, PileLayout.topOf(layer), maxZ));
                    }
                    shapes[walls][layer][quadrantX][quadrantZ] = shape.optimize();
                }
            }
        }
        }
        return shapes;
    }

    /**
     * Works out whether this pile is part of a two by two of piles of the same kind.
     * <p>
     * A group forms as soon as all four blocks exist, whatever they hold, because that is the point at
     * which the player has said what they are building. From then on the four behave as one pile: one
     * outline, one pyramid, and items added or taken from any of the four.
     * <p>
     * A pile can sit in up to four different two by twos, so the first valid one in a fixed scan order
     * wins - and every member of that group has to independently pick the same group, otherwise a run of
     * piles longer than two would produce overlapping pyramids that disagree about where the apex is.
     * Piles left over from a larger arrangement simply stay as individual piles.
     *
     * @return the minimum corner of the group, or {@code null} if this pile is on its own.
     */
    public static @Nullable BlockPos groupOrigin(BlockGetter level, BlockPos pos)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof PileBlock pileBlock))
        {
            return null;
        }
        final BlockPos origin = firstGroup(level, pos, pileBlock);
        if (origin == null)
        {
            return null;
        }
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                if (!origin.equals(firstGroup(level, origin.offset(dx, 0, dz), pileBlock)))
                {
                    return null;
                }
            }
        }
        return origin;
    }

    private static @Nullable BlockPos firstGroup(BlockGetter level, BlockPos pos, PileBlock pileBlock)
    {
        for (int[] candidate : GROUP_CANDIDATES)
        {
            final BlockPos origin = pos.offset(candidate[0], 0, candidate[1]);
            if (isGroup(level, origin, pileBlock))
            {
                return origin;
            }
        }
        return null;
    }

    private static boolean isGroup(BlockGetter level, BlockPos origin, PileBlock pileBlock)
    {
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                // Same kind of pile throughout: clay and ore sitting side by side are two piles
                if (!level.getBlockState(origin.offset(dx, 0, dz)).is(pileBlock))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return {@code true} if every pile in the group is full, which is what lets another pile be built
     * on top of it.
     */
    public static boolean isGroupFull(BlockGetter level, BlockPos origin)
    {
        // Capacity is uniform across a group, so work it out once rather than per member
        final int capacity = capacityAt(level, origin);
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                if (level.getBlockState(origin.offset(dx, 0, dz)).getValue(COUNT) < capacity)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The tallest layer any member of the group has reached, which is how tall the group's shared
     * pyramid is drawn and outlined.
     */
    private static int groupLayer(BlockGetter level, BlockPos origin, int walls)
    {
        int layer = 0;
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                final BlockPos member = origin.offset(dx, 0, dz);
                layer = Math.max(layer, PileLayout.layerOf(
                    level.getBlockState(member).getValue(COUNT) - 1, walls));
            }
        }
        return layer;
    }

    /**
     * Which way a pile heaps: towards its neighbours of the same kind, so piles put down next to each
     * other lean together instead of each sitting squarely in the middle of its own block.
     *
     * @return an index for {@link PileLayout#cellOf}
     */
    public static int leanOf(BlockGetter level, BlockPos pos)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof PileBlock pileBlock))
        {
            return PileLayout.LEAN_NONE;
        }
        int leanX = 0;
        int leanZ = 0;
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                if ((dx != 0 || dz != 0) && level.getBlockState(pos.offset(dx, 0, dz)).is(pileBlock))
                {
                    leanX += dx;
                    leanZ += dz;
                }
            }
        }
        return PileLayout.lean(leanX, leanZ);
    }

    /**
     * @return the member of the group that should take the next item, so the shared pyramid grows evenly
     * rather than one quarter at a time.
     */
    public static BlockPos memberToFill(BlockGetter level, BlockPos origin)
    {
        return extremeMember(level, origin, true);
    }

    /**
     * @return the member holding the most, which is where the next item comes off.
     */
    public static BlockPos memberToEmpty(BlockGetter level, BlockPos origin)
    {
        return extremeMember(level, origin, false);
    }

    private static BlockPos extremeMember(BlockGetter level, BlockPos origin, boolean fewest)
    {
        BlockPos best = origin;
        int bestCount = -1;
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                final BlockPos member = origin.offset(dx, 0, dz);
                final int count = level.getBlockState(member).getValue(COUNT);
                // Strictly better only, so ties fall to the first in a fixed order and stay stable
                if (bestCount == -1 || (fewest ? count < bestCount : count > bestCount))
                {
                    best = member;
                    bestCount = count;
                }
            }
        }
        return best;
    }

    /** Supplied rather than passed, because the block and its block entity type register each other. */
    private final Supplier<BlockEntityType<PileBlockEntity>> blockEntityType;

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

        // Within a group the four blocks are one pile, so take from whichever holds the most and the
        // shared pyramid comes down evenly, no matter which quarter was clicked
        final BlockPos group = groupOrigin(level, topPos);
        if (group != null)
        {
            topPos = memberToEmpty(level, group);
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
        updateDependentPiles(level, topPos);

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
            // A pyramid does not balance on the point of another one, so heaping higher needs something
            // holding the material in: a wall to pile against or a pit to pile into, or failing that a
            // full two by two below, which is broad and flat enough to carry the next tier itself.
            if (walledSides(level, pos) >= WALLS_TO_BRACE)
            {
                return true;
            }
            final BlockPos group = groupOrigin(level, below);
            return group != null && isGroupFull(level, group);
        }
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }

    /**
     * How many sides are holding this pile in, and so how much it can hold.
     * <p>
     * Within a group the answer has to be the same for all four, or their quadrants would be different
     * sizes and the shared pyramid would not line up. It is the best-braced member that decides, never
     * the worst: a pile's capacity must never fall when a group forms around it, or whatever it is
     * already holding would have nowhere to go.
     */
    public static int wallsAt(BlockGetter level, BlockPos pos)
    {
        final BlockPos origin = groupOrigin(level, pos);
        if (origin == null)
        {
            return walledSides(level, pos);
        }
        int walls = 0;
        for (int dx = 0; dx < 2; dx++)
        {
            for (int dz = 0; dz < 2; dz++)
            {
                walls = Math.max(walls, walledSides(level, origin.offset(dx, 0, dz)));
            }
        }
        return walls;
    }

    /**
     * @return how much this pile can hold where it stands.
     */
    public static int capacityAt(BlockGetter level, BlockPos pos)
    {
        return PileLayout.capacity(wallsAt(level, pos));
    }

    /**
     * @return how many of the four sides of {@code pos} are solid enough to hold a heap of material in.
     */
    private static int walledSides(BlockGetter level, BlockPos pos)
    {
        int walls = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (isWall(level, pos, direction))
            {
                walls++;
            }
        }
        return walls;
    }

    /**
     * @return whether whatever is on this side of {@code pos} would hold a heap of material in.
     */
    private static boolean isWall(BlockGetter level, BlockPos pos, Direction direction)
    {
        final BlockPos side = pos.relative(direction);
        final BlockState sideState = level.getBlockState(side);
        // Another heap is no wall - it slumps just the same. Skipping it up front also keeps this from
        // asking a neighbouring pile how sturdy it is, which asks this one straight back.
        return !(sideState.getBlock() instanceof PileBlock)
            && sideState.isFaceSturdy(level, side, direction.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        // Down for the floor, horizontals because a wall this pile was leaning on may have gone
        if (direction == Direction.DOWN || direction.getAxis().isHorizontal())
        {
            // A group's capacity comes from its best-braced member, so a wall lost beside any one of
            // the four is lost for all four - including the three that never see a block update
            final BlockPos origin = groupOrigin(level, pos);
            if (origin == null)
            {
                level.scheduleTick(pos, this, 1);
            }
            else
            {
                for (int dx = 0; dx < 2; dx++)
                {
                    for (int dz = 0; dz < 2; dz++)
                    {
                        level.scheduleTick(origin.offset(dx, 0, dz), this, 1);
                    }
                }
            }
        }
        return state;
    }

    /**
     * Rechecks every pile whose footing or capacity depends on this one, none of which finds out any
     * other way.
     * <p>
     * The eight around it, because any of them may share a two by two with it, and a group's capacity is
     * its best-braced member's: break the group up and the three left behind can be holding more than
     * they can now shape. Only two of those three are neighbours in the sense the game means, and
     * diagonals never get a block update.
     * <p>
     * The nine above, because a pile up there rests on a whole two by two below, and again three
     * quarters of it is diagonal.
     */
    private static void updateDependentPiles(Level level, BlockPos pos)
    {
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                if (dx != 0 || dz != 0)
                {
                    schedulePile(level, pos.offset(dx, 0, dz));
                }
                schedulePile(level, pos.above().offset(dx, 0, dz));
            }
        }
    }

    /** Any kind of pile may rest on a merged group, and a scheduled tick only fires if it names the
     * block actually standing there, so the block has to be read back rather than assumed. */
    private static void schedulePile(Level level, BlockPos target)
    {
        if (level.getBlockState(target).getBlock() instanceof PileBlock pile)
        {
            level.scheduleTick(target, pile, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!canSurvive(state, level, pos))
        {
            level.destroyBlock(pos, true);
            return;
        }

        // Take a wall away and the heap it was holding in slumps: anything over what this spot can now
        // hold spills out rather than sitting in a pile that is no longer shaped to contain it. It goes
        // a few at a time, out through the gap, and keeps going until the heap fits again.
        final int capacity = capacityAt(level, pos);
        final int count = state.getValue(COUNT);
        if (count > capacity && level.getBlockEntity(pos) instanceof PileBlockEntity pile)
        {
            final int remaining = Math.max(capacity, count - SPILL_PER_TICK);
            final Direction gap = openSide(level, pos);
            for (int i = count; i > remaining; i--)
            {
                final ItemStack spilled = pile.removeTop();
                if (gap == null)
                {
                    popResource(level, pos, spilled);
                }
                else
                {
                    popResourceFromFace(level, pos, gap, spilled);
                }
            }
            level.setBlock(pos, state.setValue(COUNT, remaining), Block.UPDATE_CLIENTS);
            if (remaining > capacity)
            {
                level.scheduleTick(pos, this, SPILL_INTERVAL);
            }
            updateDependentPiles(level, pos);
        }
    }

    /**
     * @return a side that is no longer holding this heap in, so spill goes out through the gap rather
     * than up out of the middle, or {@code null} if the pile is walled in on all four sides.
     */
    private static @Nullable Direction openSide(BlockGetter level, BlockPos pos)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (!isWall(level, pos, direction))
            {
                return direction;
            }
        }
        return null;
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
            updateDependentPiles(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return shapeAt(state, level, pos, GROUP_OUTLINES);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        // Collision stays inside the block; only the outline is allowed to span the group
        return shapeAt(state, level, pos, GROUP_COLLISION);
    }

    /**
     * The shape another block asks about when it wants to know whether it can lean on this one.
     * <p>
     * Deliberately blind to its surroundings, which is what makes it safe to answer. A pile's real shape
     * depends on the walls around it, so working it out means asking each neighbour how sturdy it is -
     * and because piles declare a dynamic shape, that question is answered live rather than from the
     * table Minecraft precomputes for ordinary blocks. It comes back here, and a pile beside a pile
     * asks each other until the stack runs out.
     * <p>
     * The unwalled shape is the honest answer anyway: what a pile can be leaned on for should not
     * depend on what is leaning on it.
     */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos)
    {
        return SHAPES[0][PileLayout.layerOf(state.getValue(COUNT) - 1, 0)];
    }

    private VoxelShape shapeAt(BlockState state, BlockGetter level, BlockPos pos, VoxelShape[][][][] grouped)
    {
        final int walls = wallsAt(level, pos);
        final BlockPos origin = groupOrigin(level, pos);
        if (origin != null)
        {
            return grouped[walls][groupLayer(level, origin, walls)]
                [pos.getX() - origin.getX()][pos.getZ() - origin.getZ()];
        }
        return SHAPES[walls][PileLayout.layerOf(state.getValue(COUNT) - 1, walls)];
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
