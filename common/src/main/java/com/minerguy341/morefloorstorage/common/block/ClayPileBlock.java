package com.minerguy341.morefloorstorage.common.block;

import com.minerguy341.morefloorstorage.common.blockentity.ClayPileBlockEntity;
import com.minerguy341.morefloorstorage.common.blockentity.MFSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
 * TerraFirmaCraft's ingot pile, applied to clay. Clay-type items sneak-placed on the ground stack into
 * a pile of up to {@link #MAX_ITEMS} instead of dropping as loose items, and clicking the pile takes the
 * top one back off. The pile grows as it fills, in the stepped pyramid described by
 * {@link ClayPileLayout}. Piles stack vertically, and the pile that a click resolves to is always the
 * topmost one in the column, so a tall stack empties from the top down.
 */
public class ClayPileBlock extends Block implements EntityBlock
{
    public static final int MAX_ITEMS = ClayPileLayout.MAX_ITEMS;

    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, MAX_ITEMS);

    /** One shape per layer, each the union of that layer's step and every step below it. */
    private static final VoxelShape[] SHAPES = buildShapes();

    private static VoxelShape[] buildShapes()
    {
        final VoxelShape[] shapes = new VoxelShape[ClayPileLayout.LAYERS];
        VoxelShape shape = Shapes.empty();
        for (int layer = 0; layer < ClayPileLayout.LAYERS; layer++)
        {
            final int inset = ClayPileLayout.insetOf(layer);
            shape = Shapes.or(shape, box(
                inset, ClayPileLayout.bottomOf(layer), inset,
                16 - inset, ClayPileLayout.topOf(layer), 16 - inset));
            shapes[layer] = shape.optimize();
        }
        return shapes;
    }

    public ClayPileBlock(Properties properties)
    {
        super(properties);
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
        if (!topState.is(this) || !(level.getBlockEntity(topPos) instanceof ClayPileBlockEntity pile))
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
        return belowState.isFaceSturdy(level, below, Direction.UP) || belowState.is(this);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (direction == Direction.DOWN && !neighborState.isFaceSturdy(level, neighborPos, Direction.UP) && !neighborState.is(this))
        {
            level.scheduleTick(pos, this, 1);
        }
        return state;
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
        if (player.isCreative() && level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile)
        {
            pile.removeAll(stack -> {});
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile)
        {
            pile.removeAll(stack -> popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[ClayPileLayout.layerOf(state.getValue(COUNT) - 1)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(COUNT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        // Contents are drawn by ClayPileRenderer; the block itself has no baked geometry
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
    {
        return level.getBlockEntity(pos) instanceof ClayPileBlockEntity pile ? pile.getPickedItemStack() : ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return MFSBlockEntities.CLAY_PILE.get().create(pos, state);
    }
}
