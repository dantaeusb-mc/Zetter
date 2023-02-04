package me.dantaeusb.zetter.deprecated.block;

import me.dantaeusb.zetter.core.ZetterBlockEntities;
import me.dantaeusb.zetter.deprecated.block.entity.EaselBlockEntity;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

import javax.annotation.Nullable;

public class EaselBlock extends ContainerBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    public EaselBlock(AbstractBlock.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public TileEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EaselBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState iBlockState) {
        return BlockRenderType.INVISIBLE;
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, BlockState blockState, TileEntityType<T> entityType) {
        return world.isClientSide() ? null : createTickerHelper(entityType, ZetterBlockEntities.EASEL_BLOCK_ENTITY.get(), EaselBlockEntity::serverTick);
    }

    /*
     * Double-block logic
     * Mostly copied from DoorBlock with adjustments
     */

    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockPos blockpos = context.getClickedPos();
        if (blockpos.getY() < 255 && context.getLevel().getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, context.getHorizontalDirection());
        } else {
            return null;
        }
    }

    /**
     * Remove easel if one part of the double-blocks was removed
     * @param state
     * @param facing
     * @param facingState
     * @param world
     * @param currentPos
     * @param facingPos
     * @return
     */
    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);

        // Sanity check copied from door block: same direction axis and both block connected to each other
        if (facing.getAxis() == Direction.Axis.Y && doubleBlockHalf == DoubleBlockHalf.LOWER == (facing == Direction.UP)) {
            // If second part broken
            if (!facingState.is(this)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        // If lower part has nothing to stand on
        return doubleBlockHalf == DoubleBlockHalf.LOWER && !state.canSurvive(world, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, world, currentPos, facingPos);
    }

    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockPos blockpos = pos.below();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? blockstate.isFaceSturdy(worldIn, blockpos, Direction.UP) : blockstate.is(this);
    }

    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        worldIn.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    /**
     * Shape
     */

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    private static final VoxelShape UPPER_SHAPE = Block.box(2.0D, 0.0D, 5.0D, 14.0D, 11.0D, 10.0D);
    private static final VoxelShape LOWER_SHAPE = Block.box(2.0D, 0.0D, 3.0D, 14.0D, 15.0D, 12.0D);

    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return rotateShape(Direction.NORTH, state.getValue(FACING), UPPER_SHAPE);
        } else {
            return rotateShape(Direction.NORTH, state.getValue(FACING), LOWER_SHAPE);
        }
    }

    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{ shape, VoxelShapes.empty() };

        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1], VoxelShapes.box(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        return buffer[0];
    }
}
