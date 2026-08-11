package com.minerguy341.morefloorstorage.common.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * Piles standing in a filled rectangle merge into one - see {@link PileGroup} - and from then on behave
 * as a single heap: one outline, one pyramid spanning all of them, items added to or taken from any of
 * them, and one capacity worked out from the rectangle's own perimeter. That is what lets a heap fill the
 * floor of a pit rather than heaping up in one block, and what makes a pit worth digging: a group walled
 * on all four sides is a column.
 * <p>
 * A pyramid will not balance on the point of another one, so piles do not stack. The way up is to fill a
 * group: once every member is full it is broad and flat enough to build the next tier on. Take an item
 * back off any of them and it is no longer full, dropping whatever was resting on it.
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

    /** One shape per layer, each the union of that layer's step and every step below it. */
    private static final VoxelShape[][] SHAPES = buildShapes();

    /**
     * A merged pile's shape, worked out the first time each combination is wanted rather than up front:
     * spans, cells, walls and layers multiply out to tens of thousands of shapes, of which any one world
     * uses a handful. The outline spans the whole group - deliberately reaching outside its own block -
     * so a group reads and targets as one pile. Collision keeps to the block it belongs to.
     */
    private static final Map<Long, VoxelShape> GROUP_SHAPES = new ConcurrentHashMap<>();

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

    /**
     * One cell's view of a merged pile's pyramid, in this block's own coordinates - so it runs negative
     * to the west and north of the cell, and past 16 to the east and south.
     *
     * @param clipToBlock keep the shape inside this block, for collision rather than outline
     */
    private static VoxelShape groupShape(int walls, int layer, PileGroup group, int cellX, int cellZ, boolean clipToBlock)
    {
        final long key = (((((((long) walls * PileLayout.LAYERS + layer)
            * PileGroup.MAX_SPAN + (group.spanX() - 1))
            * PileGroup.MAX_SPAN + (group.spanZ() - 1))
            * PileGroup.MAX_SPAN + cellX)
            * PileGroup.MAX_SPAN + cellZ) << 1) | (clipToBlock ? 1 : 0);
        return GROUP_SHAPES.computeIfAbsent(key, ignored -> {
            VoxelShape shape = Shapes.empty();
            for (int step = 0; step <= layer; step++)
            {
                double minX = PileLayout.mergedInsetOf(step, walls, group.spanX()) - cellX * 16.0;
                double maxX = group.spanX() * 16.0 - PileLayout.mergedInsetOf(step, walls, group.spanX()) - cellX * 16.0;
                double minZ = PileLayout.mergedInsetOf(step, walls, group.spanZ()) - cellZ * 16.0;
                double maxZ = group.spanZ() * 16.0 - PileLayout.mergedInsetOf(step, walls, group.spanZ()) - cellZ * 16.0;
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
                        minX, PileLayout.bottomOf(step), minZ,
                        maxX, PileLayout.topOf(step), maxZ));
                }
            }
            return shape.optimize();
        });
    }

    /**
     * @return {@code true} if every pile in the group is full, which is what lets another pile be built
     * on top of it.
     */
    public static boolean isGroupFull(BlockGetter level, PileGroup group)
    {
        // Capacity is uniform across a group, so work it out once rather than per member
        final int capacity = capacityAt(level, group.origin());
        for (int cellX = 0; cellX < group.spanX(); cellX++)
        {
            for (int cellZ = 0; cellZ < group.spanZ(); cellZ++)
            {
                if (level.getBlockState(group.member(cellX, cellZ)).getValue(COUNT) < capacity)
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
    private static int groupLayer(BlockGetter level, PileGroup group, int walls)
    {
        int layer = 0;
        for (int cellX = 0; cellX < group.spanX(); cellX++)
        {
            for (int cellZ = 0; cellZ < group.spanZ(); cellZ++)
            {
                layer = Math.max(layer, PileLayout.layerOf(
                    level.getBlockState(group.member(cellX, cellZ)).getValue(COUNT) - 1, walls));
            }
        }
        return layer;
    }

    /**
     * Which way a pile heaps.
     * <p>
     * Inside a group, towards the middle of the group, so the shared pyramid builds up from its centre
     * however the members happen to be filled. On its own, towards whichever neighbours of the same kind
     * it has, so piles put down next to each other lean together rather than each sitting squarely in
     * the middle of its own block - which is what they are about to become when the rectangle completes.
     *
     * @return an index for {@link PileLayout#cellOf}
     */
    public static int leanOf(BlockGetter level, BlockPos pos)
    {
        if (!(level.getBlockState(pos).getBlock() instanceof PileBlock pileBlock))
        {
            return PileLayout.LEAN_NONE;
        }
        final PileGroup group = PileGroup.at(level, pos);
        if (group != null)
        {
            return PileLayout.lean(
                Integer.signum(group.spanX() - 1 - 2 * group.cellX(pos)),
                Integer.signum(group.spanZ() - 1 - 2 * group.cellZ(pos)));
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
     * @return the member of the group holding the fewest, which is where the next item goes so that the
     * shared pyramid grows evenly rather than one cell at a time.
     */
    public static BlockPos memberToFill(BlockGetter level, PileGroup group)
    {
        return extremeMember(level, group, true);
    }

    /**
     * @return the member holding the most, which is where the next item comes off.
     */
    public static BlockPos memberToEmpty(BlockGetter level, PileGroup group)
    {
        return extremeMember(level, group, false);
    }

    private static BlockPos extremeMember(BlockGetter level, PileGroup group, boolean fewest)
    {
        BlockPos best = group.origin();
        int bestCount = -1;
        for (int cellX = 0; cellX < group.spanX(); cellX++)
        {
            for (int cellZ = 0; cellZ < group.spanZ(); cellZ++)
            {
                final BlockPos member = group.member(cellX, cellZ);
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

        // A group is one pile, so take from whichever member holds the most and the shared pyramid comes
        // down evenly, no matter which cell of it was clicked
        final PileGroup group = PileGroup.at(level, topPos);
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

        // This pile is no longer full, so anything the merged pyramid was holding up has to be rechecked.
        // Emptying it altogether is a change of shape too, but that goes through onRemove.
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
            // A pyramid does not balance on the point of another one, so heaping higher needs something
            // holding the material in: a wall to pile against or a pit to pile into, or failing that a
            // full group below, which is broad and flat enough to carry the next tier itself.
            if (walledSides(level, pos) >= WALLS_TO_BRACE)
            {
                return true;
            }
            final PileGroup group = PileGroup.at(level, below);
            return group != null && isGroupFull(level, group);
        }
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }

    /**
     * How many sides are holding this pile in, and so how much it can hold.
     * <p>
     * A merged group is one heap, so what holds it in is its own perimeter: a side counts only if it is
     * walled along its whole length. A group filling a pit is walled on all four sides and stands as a
     * column; knock one block out of the pit and that side is open, the group is a side short, and the
     * heap slumps to the amount the remaining walls can hold. Individual members' walls do not come into
     * it - a pile in the middle of a group has none, and a group has to answer with one number or its
     * cells would be different sizes and the shared pyramid would not line up.
     */
    public static int wallsAt(BlockGetter level, BlockPos pos)
    {
        final PileGroup group = PileGroup.at(level, pos);
        if (group == null)
        {
            return walledSides(level, pos);
        }
        int walls = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (isEdgeWalled(level, group, direction))
            {
                walls++;
            }
        }
        return walls;
    }

    /**
     * @return whether every block along one side of the group is backed by a wall. One gap anywhere
     * along it and the heap has somewhere to go, so the whole side counts for nothing.
     */
    private static boolean isEdgeWalled(BlockGetter level, PileGroup group, Direction direction)
    {
        for (BlockPos member : group.edge(direction))
        {
            if (!isWall(level, member, direction))
            {
                return false;
            }
        }
        return true;
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
            // A group's capacity comes from its perimeter, so a wall lost beside any one member is lost
            // for the whole group - including the members that never see a block update
            final PileGroup group = PileGroup.at(level, pos);
            if (group == null)
            {
                level.scheduleTick(pos, this, 1);
            }
            else
            {
                group.forEach(member -> level.scheduleTick(member, this, 1));
            }
        }
        return state;
    }

    /**
     * Rechecks every pile whose footing or capacity depends on this one, none of which finds out any
     * other way.
     * <p>
     * Everything within a group's reach of it, because adding or removing one pile re-cuts the whole run
     * of touching piles into rectangles: piles that were merged can come apart, and their capacity falls
     * with the perimeter they lose. Most of those never get a block update - diagonals never do, and
     * nothing more than one block away does.
     * <p>
     * And the nine above, because a pile up there rests on the group below being full.
     */
    private static void updateDependentPiles(Level level, BlockPos pos)
    {
        final int reach = PileGroup.MAX_SPAN - 1;
        for (int dx = -reach; dx <= reach; dx++)
        {
            for (int dz = -reach; dz <= reach; dz++)
            {
                if (dx != 0 || dz != 0)
                {
                    schedulePile(level, pos.offset(dx, 0, dz));
                }
            }
        }
        updateSupportedPiles(level, pos);
    }

    /**
     * Rechecks the piles resting on this one. A pile above is carried by a whole group, three quarters
     * of which are diagonal neighbours that never get an ordinary block update.
     */
    private static void updateSupportedPiles(Level level, BlockPos pos)
    {
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
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
            // Only what is resting on this: shedding changes how full the group is, not its shape, and
            // this runs once per shed so it must not go re-scanning the neighbourhood each time
            updateSupportedPiles(level, pos);
        }
    }

    /**
     * @return a side that is no longer holding this heap in, so spill goes out through the gap rather
     * than up out of the middle, or {@code null} if it is walled in on all four sides.
     */
    private static @Nullable Direction openSide(BlockGetter level, BlockPos pos)
    {
        // A member in the middle of a group has no walls of its own but is not open either, so it is the
        // group's perimeter that says where the material can actually get out
        final PileGroup group = PileGroup.at(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (group == null ? !isWall(level, pos, direction) : !isEdgeWalled(level, group, direction))
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
        return shapeAt(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        // Collision stays inside the block; only the outline is allowed to span the group
        return shapeAt(state, level, pos, true);
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

    private VoxelShape shapeAt(BlockState state, BlockGetter level, BlockPos pos, boolean clipToBlock)
    {
        final int walls = wallsAt(level, pos);
        final PileGroup group = PileGroup.at(level, pos);
        if (group != null)
        {
            return groupShape(walls, groupLayer(level, group, walls), group,
                group.cellX(pos), group.cellZ(pos), clipToBlock);
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
