package com.dantaeusb.zetter.block;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.network.packet.painting.SCanvasNamePacket;
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

public class EaselBlock extends ContainerBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;

    public EaselBlock(Properties properties) {
        super(properties);

        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
    }

    /**
     * Create the Tile Entity for this block.
     * Forge has a default but I've included it anyway for clarity
     * @return
     */
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return createNewTileEntity(world);
    }

    @Nullable
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new EaselTileEntity();
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return false;
        }

        return true;
    }

    @Override
    public BlockRenderType getRenderType(BlockState iBlockState) {
        return BlockRenderType.INVISIBLE;
    }

    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (world.isRemote) return ActionResultType.SUCCESS;

        ItemStack heldItem = player.getHeldItem(Hand.MAIN_HAND);

        BlockPos tileEntityPos = pos;
        if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            tileEntityPos = tileEntityPos.down();
        }

        EaselTileEntity easelTileEntity = (EaselTileEntity) world.getTileEntity(tileEntityPos);

        if (easelTileEntity == null) return ActionResultType.FAIL;

        if (!player.isCrouching()) {
            ItemStack easelCanvasStack = easelTileEntity.getEaselStorage().getCanvasStack();

            if (easelCanvasStack.isEmpty()) {
                if (heldItem.getItem() == ModItems.CANVAS && easelTileEntity.putCanvasStack(heldItem)) {
                    player.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                    world.notifyBlockUpdate(tileEntityPos, state, state, 2);

                    return ActionResultType.SUCCESS;
                }
            }

            INamedContainerProvider namedContainerProvider = this.getContainer(state, world, tileEntityPos);

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
                world.notifyBlockUpdate(tileEntityPos, state, state, Constants.BlockFlags.BLOCK_UPDATE);

                // No canvas to grab
                if (canvasStack.isEmpty()) {
                    return ActionResultType.FAIL;
                }

                player.setHeldItem(Hand.MAIN_HAND, canvasStack);

                return ActionResultType.SUCCESS;
            }

            return ActionResultType.FAIL;
        }
    }

    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return true;
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.isIn(newState.getBlock())) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof EaselTileEntity) {
                InventoryHelper.dropInventoryItems(world, pos, ((EaselTileEntity) tileEntity).getEaselStorage());
                world.updateComparatorOutputLevel(pos, this);
            }

            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }

    /*
     * Double-block logic
     * Mostly copied from DoorBlock with adjustments
     */

    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockPos blockpos = context.getPos();
        if (blockpos.getY() < 255 && context.getWorld().getBlockState(blockpos.up()).isReplaceable(context)) {
            return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing()).with(HALF, DoubleBlockHalf.LOWER).with(FACING, context.getPlacementHorizontalFacing());
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
    public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf doubleBlockHalf = state.get(HALF);

        // Sanity check copied from door block: same direction axis and both block connected to each other
        if (facing.getAxis() == Direction.Axis.Y && doubleBlockHalf == DoubleBlockHalf.LOWER == (facing == Direction.UP)) {
            // If second part broken
            if (!facingState.isIn(this)) {
                return Blocks.AIR.getDefaultState();
            }
        }

        // If lower part has nothing to stand on
        return doubleBlockHalf == DoubleBlockHalf.LOWER && !state.isValidPosition(world, currentPos) ? Blocks.AIR.getDefaultState() : super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
    }

    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockPos blockpos = pos.down();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        return state.get(HALF) == DoubleBlockHalf.LOWER ? blockstate.isSolidSide(worldIn, blockpos, Direction.UP) : blockstate.isIn(this);
    }

    protected void writeCanvasIdToNetwork(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, PacketBuffer networkBuffer) {
        TileEntity easelTileEntity = worldIn.getTileEntity(pos);

        if (!(easelTileEntity instanceof EaselTileEntity)) {
            Zetter.LOG.error("Cannot find EaselTileEntity to send canvas data to client");

            return;
        }

        SCanvasNamePacket.writeCanvasName(networkBuffer, ((EaselTileEntity) easelTileEntity).getCanvasName());
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        worldIn.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
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
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    private static final VoxelShape UPPER_SHAPE = Block.makeCuboidShape(2.0D, 0.0D, 5.0D, 14.0D, 11.0D, 10.0D);
    private static final VoxelShape LOWER_SHAPE = Block.makeCuboidShape(2.0D, 0.0D, 3.0D, 14.0D, 15.0D, 12.0D);

    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return rotateShape(Direction.NORTH, state.get(FACING), UPPER_SHAPE);
        } else {
            return rotateShape(Direction.NORTH, state.get(FACING), LOWER_SHAPE);
        }
    }

    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{ shape, VoxelShapes.empty() };

        int times = (to.getHorizontalIndex() - from.getHorizontalIndex() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1], VoxelShapes.create(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        return buffer[0];
    }
}
