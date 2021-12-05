package com.dantaeusb.zetter.block;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import com.dantaeusb.zetter.tileentity.EaselTileEntity;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

import net.minecraft.block.AbstractBlock.Properties;

public class EaselBlock extends ContainerBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    public EaselBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /**
     * Create the Tile Entity for this block.
     * Forge has a default but I've included it anyway for clarity
     * @return
     */
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return newBlockEntity(world);
    }

    @Nullable
    public TileEntity newBlockEntity(IBlockReader world) {
        return new EaselTileEntity();
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return false;
        }

        return true;
    }

    @Override
    public BlockRenderType getRenderShape(BlockState iBlockState) {
        return BlockRenderType.INVISIBLE;
    }

    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (world.isClientSide) return ActionResultType.SUCCESS;

        ItemStack heldItem = player.getItemInHand(Hand.MAIN_HAND);

        BlockPos tileEntityPos = pos;
        if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            tileEntityPos = tileEntityPos.below();
        }

        EaselTileEntity easelTileEntity = (EaselTileEntity) world.getBlockEntity(tileEntityPos);

        if (easelTileEntity == null) return ActionResultType.FAIL;

        if (!player.isCrouching()) {
            ItemStack easelCanvasStack = easelTileEntity.getEaselStorage().getCanvasStack();

            if (easelCanvasStack.isEmpty()) {
                if (heldItem.getItem() == ModItems.CANVAS && easelTileEntity.putCanvasStack(heldItem)) {
                    player.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    world.sendBlockUpdated(tileEntityPos, state, state, 2);

                    return ActionResultType.SUCCESS;
                }
            }

            INamedContainerProvider namedContainerProvider = this.getMenuProvider(state, world, tileEntityPos);

            if (namedContainerProvider != null) {
                if (!(player instanceof ServerPlayerEntity)) return ActionResultType.FAIL;

                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
                BlockPos finalTileEntityPos = tileEntityPos;

                NetworkHooks.openGui(serverPlayerEntity, namedContainerProvider, (packetBuffer) -> this.writeCanvasIdToNetwork(state, world, finalTileEntityPos, player, packetBuffer));
            }

            return ActionResultType.SUCCESS;
        } else {
            if (heldItem.isEmpty()) {
                ItemStack canvasStack = easelTileEntity.getEaselStorage().extractCanvas();
                world.sendBlockUpdated(tileEntityPos, state, state, Constants.BlockFlags.BLOCK_UPDATE);

                // No canvas to grab
                if (canvasStack.isEmpty()) {
                    return ActionResultType.FAIL;
                }

                player.setItemInHand(Hand.MAIN_HAND, canvasStack);

                return ActionResultType.SUCCESS;
            }

            return ActionResultType.FAIL;
        }
    }

    public boolean isPathfindable(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return true;
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof EaselTileEntity) {
                InventoryHelper.dropContents(world, pos, ((EaselTileEntity) tileEntity).getEaselStorage());
                world.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
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

    protected void writeCanvasIdToNetwork(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, PacketBuffer networkBuffer) {
        TileEntity easelTileEntity = worldIn.getBlockEntity(pos);

        if (!(easelTileEntity instanceof EaselTileEntity)) {
            Zetter.LOG.error("Cannot find EaselTileEntity to send canvas data to client");

            return;
        }

        SCanvasNamePacket.writeCanvasName(networkBuffer, ((EaselTileEntity) easelTileEntity).getCanvasName());
    }

    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        worldIn.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    /*
     * Not sure if needed, it'll remove second part anyway
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!worldIn.isRemote && player.isCreative()) {
            DoublePlantBlock.removeBottomHalf(worldIn, pos, state, player);
        }

        super.onBlockHarvested(worldIn, pos, state, player);
    }*/

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
