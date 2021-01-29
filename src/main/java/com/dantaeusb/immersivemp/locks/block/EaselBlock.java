package com.dantaeusb.immersivemp.locks.block;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.network.packet.painting.SCanvasNamePacket;
import com.dantaeusb.immersivemp.locks.tileentity.EaselTileEntity;
import com.dantaeusb.immersivemp.locks.tileentity.KeyLockableTileEntity;
import com.dantaeusb.immersivemp.locks.tileentity.storage.EaselStorage;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
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
import net.minecraft.world.World;
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
                if (heldItem.getItem() == ModLockItems.CANVAS_ITEM) {
                    player.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);

                    easelTileEntity.getEaselStorage().setCanvasStack(heldItem);
                    easelTileEntity.markDirty();

                    return ActionResultType.SUCCESS;
                }

                return ActionResultType.FAIL;
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
                easelTileEntity.markDirty();

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

    protected void writeCanvasIdToNetwork(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, PacketBuffer networkBuffer) {
        TileEntity easelTileEntity = worldIn.getTileEntity(pos);

        if (!(easelTileEntity instanceof EaselTileEntity)) {
            ImmersiveMp.LOG.error("Cannot find EaselTileEntity to send canvas data to client");
            return;
        }

        SCanvasNamePacket.writeCanvasName(networkBuffer, ((EaselTileEntity) easelTileEntity).getCanvasName());
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        worldIn.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
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
