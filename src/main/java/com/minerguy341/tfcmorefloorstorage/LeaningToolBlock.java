package com.minerguy341.tfcmorefloorstorage;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Tools propped up against the side of a block, as in Vintage Story.
 * Occupies the air in front of a wall, holds up to {@link #SLOTS} tools, no collision.
 * {@link #FACING} points from the block toward the wall the tools rest against.
 * <p>
 * Ported from Claude's {@code morefloorstorage} leaning-tool implementation.
 */
public class LeaningToolBlock extends Block implements EntityBlock
{
    public static final int SLOTS = 4;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
        Direction.NORTH, box(0, 0, 0, 16, 15, 6),
        Direction.SOUTH, box(0, 0, 10, 16, 15, 16),
        Direction.WEST, box(0, 0, 0, 6, 15, 16),
        Direction.EAST, box(10, 0, 0, 16, 15, 16)
    ));

    /**
     * Maps a hit on this block to one of the {@link #SLOTS} tool positions, numbered left to right
     * as seen from the room side. Kept in sync with the renderer offsets.
     */
    public static int slotFromHit(BlockState state, BlockPos pos, Vec3 location)
    {
        final Direction facing = state.getValue(FACING);
        double lateral = facing.getAxis() == Direction.Axis.Z
            ? location.x() - pos.getX()
            : location.z() - pos.getZ();
        if (facing == Direction.SOUTH || facing == Direction.WEST)
        {
            lateral = 1.0 - lateral;
        }
        return Mth.clamp((int) (lateral * SLOTS), 0, SLOTS - 1);
    }

    public LeaningToolBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    /**
     * @return {@code true} if a tool was taken.
     */
    public boolean takeTool(Level level, BlockPos pos, BlockState state, Player player, BlockHitResult hitResult)
    {
        if (!(level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning) || leaning.isEmpty())
        {
            return false;
        }
        if (level.isClientSide)
        {
            return true;
        }

        final ItemStack stack = leaning.removeTool(slotFromHit(state, pos, hitResult.getLocation()));
        if (stack.isEmpty())
        {
            return false;
        }

        ItemHandlerHelper.giveItemToPlayer(player, stack);
        if (leaning.isEmpty())
        {
            level.removeBlock(pos, false);
        }

        final SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 4f, sound.getPitch() * 0.8f);
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }
        return takeTool(level, pos, state, player, hit)
            ? InteractionResult.sidedSuccess(level.isClientSide)
            : InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))
        {
            return false;
        }
        final Direction facing = state.getValue(FACING);
        final BlockPos wall = pos.relative(facing);
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing.getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (direction == Direction.DOWN || direction == state.getValue(FACING))
        {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!canSurvive(state, level, pos))
        {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        if (player.isCreative() && level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning)
        {
            leaning.removeAll(stack -> {});
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning)
        {
            leaning.removeAll(stack -> popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        if (target instanceof BlockHitResult hit && level.getBlockEntity(pos) instanceof LeaningToolBlockEntity leaning)
        {
            return leaning.getTool(slotFromHit(state, pos, hit.getLocation())).copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return ModBlockEntities.LEANING_TOOL.get().create(pos, state);
    }
}
