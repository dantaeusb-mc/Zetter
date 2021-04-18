package com.dantaeusb.zetter.block;

import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.tileentity.EaselTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class ArtistTableBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
    private static final ITextComponent containerName = new TranslationTextComponent("container.repair");

    public ArtistTableBlock(Properties properties) {
        super(properties);

        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH));
    }

    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isRemote) {
            return ActionResultType.SUCCESS;
        } else {
            player.openContainer(state.getContainer(worldIn, pos));
            return ActionResultType.CONSUME;
        }
    }

    @Nullable
    public INamedContainerProvider getContainer(BlockState state, World worldIn, BlockPos pos) {
        return new SimpleNamedContainerProvider((id, inventory, player) -> new ArtistTableContainer(id, inventory, IWorldPosCallable.of(worldIn, pos)), containerName);
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
