package com.minerguy341.tfcmorefloorstorage;

import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Dedicated clay floor pile: 128 capacity (16 per layer × 8 layers), neat corner-aligned packing.
 */
public class ClayPileBlock extends ExtendedBlock implements EntityBlockExtension
{
    public static final int MAX_COUNT = 128;
    public static final int PER_LAYER = 16;
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, MAX_COUNT);

    /** One collision shape per filled layer (16 clay each). */
    private static final VoxelShape[] SHAPES = {
        box(0, 0, 0, 16, 2, 16),
        box(0, 0, 0, 16, 4, 16),
        box(0, 0, 0, 16, 6, 16),
        box(0, 0, 0, 16, 8, 16),
        box(0, 0, 0, 16, 10, 16),
        box(0, 0, 0, 16, 12, 16),
        box(0, 0, 0, 16, 14, 16),
        box(0, 0, 0, 16, 16, 16)
    };

    public ClayPileBlock(ExtendedProperties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(COUNT, 1));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos)
    {
        if (direction == Direction.DOWN && !neighborState.isFaceSturdy(level, neighborPos, direction.getOpposite()) && !neighborState.is(this))
        {
            level.scheduleTick(currentPos, this, 1);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!player.isShiftKeyDown())
        {
            BlockPos topPos = pos;
            while (level.getBlockState(topPos.above()).is(this))
            {
                topPos = topPos.above();
            }

            final BlockState topState = level.getBlockState(topPos);
            final int topCount = topState.getValue(COUNT);

            if (level.getBlockEntity(topPos) instanceof ClayPileBlockEntity pile)
            {
                final ItemStack clay = pile.removeClay();
                if (!player.isCreative())
                {
                    ItemHandlerHelper.giveItemToPlayer(player, clay);
                }
            }

            if (topCount == 1)
            {
                level.removeBlock(topPos, false);
            }
            else
            {
                level.setBlock(topPos, topState.setValue(COUNT, topCount - 1), Block.UPDATE_CLIENTS);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos below = pos.below();
        final BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || belowState.is(this);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        final boolean canActuallyHarvest = state.canHarvestBlock(level, pos, player);
        playerWillDestroy(level, pos, state, player);

        if (player.isCreative() && canActuallyHarvest && level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile)
        {
            pile.removeAll(stack -> {});
        }

        return level.setBlock(pos, fluid.createLegacyBlock(), level.isClientSide ? 11 : 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile && newState.getBlock() != this)
        {
            pile.removeAll(stack -> popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(COUNT));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[(state.getValue(COUNT) - 1) / PER_LAYER];
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        return level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile ? pile.getPickedItemStack() : ItemStack.EMPTY;
    }
}
