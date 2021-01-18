package com.dantaeusb.immersivemp.locks.block;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.item.ILockingItem;
import com.dantaeusb.immersivemp.locks.tileentity.KeyLockableTileEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class LockableDoorBlock extends DoorBlock {
    public LockableDoorBlock(AbstractBlock.Properties builder)
    {
        super(builder);
    }

    /**
     * Basic block activated event handled in
     * @see com.dantaeusb.immersivemp.locks.core.ModLockGameEvents#onPlayerInteract(PlayerInteractEvent.RightClickBlock)
     * If TileEntity forbids opening, event is market as denied. It seems to be cleaner way,
     * Also patrially we use that way because of Quark design which lead to vunerability with double-doors
     *
     * This one only handles shift+right-click to change lock mode
     */

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isRemote) {
            return ActionResultType.CONSUME;
        }

        if (!player.isCrouching()) {
            ImmersiveMp.LOG.warn("Action should be dispatched by onPlayerInteract handler");
            return ActionResultType.CONSUME;
        }

        KeyLockableTileEntity doorTileEntity = getDoorTileEntity(worldIn, pos);

        if (doorTileEntity == null) {
            return ActionResultType.FAIL;
        }

        boolean isOpen = state.get(DoorBlock.OPEN);

        // Close before locking
        if (isOpen) {
            doorTileEntity.openDoor(false);
        }

        if (doorTileEntity.canUnlock(player)) {
            doorTileEntity.toggleLock();
            ImmersiveMp.LOG.info("Toggling lock");
        }


        return ActionResultType.CONSUME;
    }

    /**
     * @param world
     * @param pos
     * @return
     */
    public static KeyLockableTileEntity getDoorTileEntity(World world, BlockPos pos) {
        BlockPos tileEntityPos = pos;
        BlockState state = world.getBlockState(pos);

        if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            tileEntityPos = tileEntityPos.down();
        }

        TileEntity doorTileEntity = world.getTileEntity(tileEntityPos);

        if (!(doorTileEntity instanceof KeyLockableTileEntity)) {
            return null;
        }

        return (KeyLockableTileEntity) doorTileEntity;
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
