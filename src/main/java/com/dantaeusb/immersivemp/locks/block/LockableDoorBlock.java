package com.dantaeusb.immersivemp.locks.block;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.item.ILockingItem;
import com.dantaeusb.immersivemp.locks.tileentity.KeyLockableTileEntity;
import com.dantaeusb.immersivemp.state.properties.LockBlockStateProperties;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;

public class LockableDoorBlock extends DoorBlock {
    public static final BooleanProperty LOCKED = LockBlockStateProperties.LOCKED;

    public LockableDoorBlock(AbstractBlock.Properties builder)
    {
        super(builder);
    }

    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos blockPos, PlayerEntity playerEntity, Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isRemote()) {
            return ActionResultType.SUCCESS;
        }

        BlockPos tileEntityPos = blockPos;
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            tileEntityPos = blockPos.offset(Direction.DOWN);
        }

        KeyLockableTileEntity lockableDoorTileEntity = (KeyLockableTileEntity) worldIn.getTileEntity(tileEntityPos);
        if (lockableDoorTileEntity == null) {
            ImmersiveMp.LOG.warn("Unable to find Locking Door TileEntity at pos " + tileEntityPos);
            return ActionResultType.CONSUME;
        }

        if (lockableDoorTileEntity.onActivated(playerEntity, worldIn, playerEntity.isCrouching())) {
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.CONSUME;
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }

        return false;
    }

    @Override
    public void openDoor(World worldIn, BlockState state, BlockPos pos, boolean open) {
        ImmersiveMp.LOG.warn("Lockable door can be opened only through KeyLockableTileEntity");
        return;
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        // Override redstone behavior
        return;
    }

    // Called when the block is placed or loaded client side to get the tile entity for the block
    // Should return a new instance of the tile entity for the block
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {return new KeyLockableTileEntity();}

    // Called just after the player places a block.
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        // Only for bottom part with TE and on server
        if (!this.hasTileEntity(state) || worldIn.isRemote()) {
            return;
        }

        if (!Helper.isLockableItem(stack)) {
            ImmersiveMp.LOG.error("Unable to find DoorItem in Player's hand to copy Door Key");
            return;
        }

        // @todo: check stack
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof KeyLockableTileEntity) {
            KeyLockableTileEntity lockableTileEntity = (KeyLockableTileEntity) tileEntity;
            lockableTileEntity.setKeyId(ILockingItem.getLockId(stack));
        }
    }
}
