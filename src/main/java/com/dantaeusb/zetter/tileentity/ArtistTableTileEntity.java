package com.dantaeusb.zetter.tileentity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModTileEntities;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableCanvasStorage;
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

public class ArtistTableTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private static final String ARTIST_CANVAS_STORAGE_TAG = "canvas_storage";

    private final ArtistTableCanvasStorage canvasStorage;

    public ArtistTableTileEntity() {
        super(ModTileEntities.ARTIST_TABLE_TILE_ENTITY);

        this.canvasStorage = ArtistTableCanvasStorage.createForTileEntity(this::canPlayerAccessInventory, this::markDirty);
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

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        // @todo: change this
        return INFINITE_EXTENT_AABB;
        //return new AxisAlignedBB(this.getPos(), this.getPos().add(1, 2, 1));
    }

    // NBT stack

    @Override
    public CompoundNBT write(CompoundNBT parentNBTTagCompound)
    {
        super.write(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        CompoundNBT canvasNbt = this.canvasStorage.serializeNBT();
        parentNBTTagCompound.put(ARTIST_CANVAS_STORAGE_TAG, canvasNbt);

        return parentNBTTagCompound;
    }

    @Override
    public void read(BlockState blockState, CompoundNBT parentNBTTagCompound)
    {
        super.read(blockState, parentNBTTagCompound);

        CompoundNBT canvasNbt = parentNBTTagCompound.getCompound(ARTIST_CANVAS_STORAGE_TAG);
        this.canvasStorage.deserializeNBT(canvasNbt);

        if (this.canvasStorage.getSizeInventory() != ArtistTableCanvasStorage.STORAGE_SIZE)
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

        int tileEntityType = 43;
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
        InventoryHelper.dropInventoryItems(world, blockPos, this.canvasStorage);
    }

    //

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.zetter.artistTable");
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
        return ArtistTableContainer.createContainerServerSide(windowID, playerInventory, this.canvasStorage);
    }

    static public boolean isItemValidForCanvasArea(ItemStack itemStack)
    {
        return itemStack.getItem() == ModItems.CANVAS_ITEM;
    }

    static public boolean isItemValidForFrameMainMaterial(ItemStack itemStack)
    {
        return true;
    }

    static public boolean isItemValidForFrameDetailMaterial(ItemStack itemStack)
    {
        return true;
    }
}
