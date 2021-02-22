package com.dantaeusb.zetter.tileentity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModLockItems;
import com.dantaeusb.zetter.core.ModTileEntities;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.tileentity.storage.EaselStorage;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EaselTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private boolean used = true;
    private final EaselStorage easelStorage; // two items: canvas and palette

    private static final String EASEL_STORAGE_TAG = "storage";

    public EaselTileEntity() {
        super(ModTileEntities.EASEL_TILE_ENTITY);

        this.easelStorage = EaselStorage.createForTileEntity(this::canPlayerAccessInventory, this::markDirty);
    }

    public boolean canPlayerAccessInventory(PlayerEntity player) {
        if (this.world.getTileEntity(this.pos) != this) return false;
        final double X_CENTRE_OFFSET = 0.5;
        final double Y_CENTRE_OFFSET = 0.5;
        final double Z_CENTRE_OFFSET = 0.5;
        final double MAXIMUM_DISTANCE_SQ = 8.0 * 8.0;
        return player.getDistanceSq(pos.getX() + X_CENTRE_OFFSET, pos.getY() + Y_CENTRE_OFFSET, pos.getZ() + Z_CENTRE_OFFSET) < MAXIMUM_DISTANCE_SQ;
    }

    public void tick() {
    }

    // specific

    public EaselStorage getEaselStorage() {
        return this.easelStorage;
    }

    public boolean hasCanvas() {
        ItemStack canvasStack = this.getCanvasStack();

        return !canvasStack.isEmpty();
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelStorage.getStackInSlot(EaselStorage.CANVAS_SLOT);
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.getItem() != ModLockItems.CANVAS_ITEM) {
            return false;
        }

        if (this.hasCanvas()) {
            return false;
        }

        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.world);
        this.easelStorage.setCanvasStack(itemStack);

        return true;
    }

    /**
     * Returns current canvas name or empty string if no canvas assigned
     * @return
     */
    public String getCanvasName() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack != null && canvasStack.isEmpty()) {
            return "";
        }

        return CanvasItem.getCanvasName(canvasStack);
    }

    @Nullable
    public CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ModLockItems.CANVAS_ITEM) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.world);
    }

    // render

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return INFINITE_EXTENT_AABB;
        //return new AxisAlignedBB(this.getPos(), this.getPos().add(1, 2, 1));
    }

    // NBT stack

    @Override
    public CompoundNBT write(CompoundNBT parentNBTTagCompound)
    {
        super.write(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        CompoundNBT inventoryNBT = this.easelStorage.serializeNBT();
        parentNBTTagCompound.put(EASEL_STORAGE_TAG, inventoryNBT);

        return parentNBTTagCompound;
    }

    @Override
    public void read(BlockState blockState, CompoundNBT parentNBTTagCompound)
    {
        super.read(blockState, parentNBTTagCompound);

        CompoundNBT inventoryNBT = parentNBTTagCompound.getCompound(EASEL_STORAGE_TAG);
        this.easelStorage.deserializeNBT(inventoryNBT);

        if (this.easelStorage.getSizeInventory() != EaselStorage.STORAGE_SIZE)
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
    }

    // network stack

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);

        Zetter.LOG.info("Going to send update packet");

        int tileEntityType = 42;
        return new SUpdateTileEntityPacket(this.pos, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        BlockState blockState = world.getBlockState(pos);
        read(blockState, packet.getNbtCompound());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag)
    {
        this.read(blockState, tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(World world, BlockPos blockPos) {
        InventoryHelper.dropInventoryItems(world, blockPos, this.easelStorage);
    }

    //

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.immersivemp.easel");
    }

    /**
     * The name is misleading; createMenu has nothing to do with creating a Screen, it is used to create the Container on the server only
     * @param windowID
     * @param playerInventory
     * @param playerEntity
     * @return
     */
    @Nullable
    @Override
    public Container createMenu(int windowID, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return EaselContainer.createContainerServerSide(windowID, playerInventory, this.easelStorage);
    }
}
