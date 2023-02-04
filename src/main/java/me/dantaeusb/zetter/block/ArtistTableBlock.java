package me.dantaeusb.zetter.block;

import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.network.packet.SArtistTableMenuCreatePacket;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class ArtistTableBlock extends ContainerBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    public ArtistTableBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    public TileEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArtistTableBlockEntity(pos, state);
    }

    @Nullable
    public TileEntity newBlockEntity(IBlockReader reader) {
        return new ArtistTableBlockEntity();
    }

    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isClientSide) {
            return ActionResultType.SUCCESS;
        } else {
            this.interactWith(worldIn, pos, player);
            return ActionResultType.CONSUME;
        }
    }

    /**
     * Interface for handling interaction with blocks that impliment AbstractFurnaceBlock. Called in onBlockActivated
     * inside AbstractFurnaceBlock.
     */
    protected void interactWith(World level, BlockPos pos, PlayerEntity player) {
        TileEntity currentTileEntity = level.getBlockEntity(pos);

        if (currentTileEntity instanceof ArtistTableBlockEntity) {
            if (!level.isClientSide()) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (ArtistTableBlockEntity) currentTileEntity, (packetBuffer) -> {
                    SArtistTableMenuCreatePacket packet = new SArtistTableMenuCreatePacket(currentTileEntity.getBlockPos(), ((ArtistTableBlockEntity) currentTileEntity).getMode());
                    packet.writePacketData(packetBuffer);
                });
            }
        }
    }

    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof ArtistTableBlockEntity) {
                ((ArtistTableBlockEntity) tileEntity).dropAllContents(world, pos);
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
