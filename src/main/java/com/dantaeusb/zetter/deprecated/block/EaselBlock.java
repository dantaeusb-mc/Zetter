package com.dantaeusb.zetter.deprecated.block;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModBlockEntities;
import com.dantaeusb.zetter.deprecated.block.entity.EaselBlockEntity;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nullable;

public class EaselBlock extends BaseEntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public EaselBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EaselBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState iBlockState) {
        return RenderShape.INVISIBLE;
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (world.isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        BlockPos tileEntityPos = pos;
        if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            tileEntityPos = tileEntityPos.below();
        }

        EaselBlockEntity easelTileEntity = (EaselBlockEntity) world.getBlockEntity(tileEntityPos);

        if (easelTileEntity == null) return InteractionResult.FAIL;

        if (!player.isCrouching()) {
            ItemStack easelCanvasStack = easelTileEntity.getEaselContainer().getCanvasStack();

            if (easelCanvasStack.isEmpty()) {
                if (heldItem.getItem() == ModItems.CANVAS && easelTileEntity.putCanvasStack(heldItem)) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    world.sendBlockUpdated(tileEntityPos, state, state, 2);

                    return InteractionResult.SUCCESS;
                }
            }

            MenuProvider namedContainerProvider = this.getMenuProvider(state, world, tileEntityPos);

            if (namedContainerProvider != null) {
                if (!(player instanceof ServerPlayer)) return InteractionResult.FAIL;

                ServerPlayer serverPlayerEntity = (ServerPlayer)player;
                BlockPos finalTileEntityPos = tileEntityPos;

                NetworkHooks.openGui(serverPlayerEntity, namedContainerProvider, (packetBuffer) -> this.writeCanvasIdToNetwork(state, world, finalTileEntityPos, player, packetBuffer));
            }

            return InteractionResult.SUCCESS;
        } else {
            if (heldItem.isEmpty()) {
                ItemStack canvasStack = easelTileEntity.getEaselContainer().extractCanvasStack();
                world.sendBlockUpdated(tileEntityPos, state, state, Block.UPDATE_CLIENTS);

                // No canvas to grab
                if (canvasStack.isEmpty()) {
                    return InteractionResult.FAIL;
                }

                player.setItemInHand(InteractionHand.MAIN_HAND, canvasStack);

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        }
    }

    public boolean isPathfindable(BlockState state, BlockGetter worldIn, BlockPos pos, PathComputationType type) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof EaselBlockEntity) {
                Containers.dropContents(world, pos, ((EaselBlockEntity) tileEntity).getEaselContainer());
                world.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Nullable
    public BlockEntityTicker<EaselBlockEntity> getTicker(Level world, BlockEntityType<EaselBlockEntity> entityType) {
        return world.isClientSide() ? createTickerHelper(entityType, ModBlockEntities.EASEL_TILE_ENTITY, EaselBlockEntity::tick) : null;
    }

    /*
     * Double-block logic
     * Mostly copied from DoorBlock with adjustments
     */

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
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

    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        BlockPos blockpos = pos.below();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? blockstate.isFaceSturdy(worldIn, blockpos, Direction.UP) : blockstate.is(this);
    }

    protected void writeCanvasIdToNetwork(BlockState state, Level worldIn, BlockPos pos, Player player, FriendlyByteBuf networkBuffer) {
        BlockEntity easelTileEntity = worldIn.getBlockEntity(pos);

        if (!(easelTileEntity instanceof EaselBlockEntity)) {
            Zetter.LOG.error("Cannot find EaselTileEntity to send canvas data to client");

            return;
        }

        SCanvasNamePacket.writeCanvasName(networkBuffer, ((EaselBlockEntity) easelTileEntity).getCanvasName());
    }

    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
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

    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return rotateShape(Direction.NORTH, state.getValue(FACING), UPPER_SHAPE);
        } else {
            return rotateShape(Direction.NORTH, state.getValue(FACING), LOWER_SHAPE);
        }
    }

    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{ shape, Shapes.empty() };

        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }
}
