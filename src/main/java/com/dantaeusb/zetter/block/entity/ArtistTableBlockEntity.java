package com.dantaeusb.zetter.block.entity;

import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModBlockEntities;
import com.dantaeusb.zetter.tileentity.container.ArtistTableCanvasStorage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ArtistTableBlockEntity extends BlockEntity implements MenuProvider {
    private static final String ARTIST_TABLE_CANVAS_STORAGE_TAG = "canvas_storage";

    private final ArtistTableCanvasStorage canvasStorage;

    public ArtistTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARTIST_TABLE_TILE_ENTITY, pos, state);

        this.canvasStorage = ArtistTableCanvasStorage.createForTileEntity(this::canPlayerAccessInventory, this::setChanged);
    }

    public boolean canPlayerAccessInventory(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 1, 1));
    }

    // NBT stack

    @Override
    public CompoundTag save(CompoundTag parentNBTTagCompound)
    {
        super.save(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        CompoundTag canvasNbt = this.canvasStorage.serializeNBT();
        parentNBTTagCompound.put(ARTIST_TABLE_CANVAS_STORAGE_TAG, canvasNbt);

        return parentNBTTagCompound;
    }

    @Override
    public void load(CompoundTag parentNBTTagCompound)
    {
        super.load(parentNBTTagCompound);

        CompoundTag canvasNbt = parentNBTTagCompound.getCompound(ARTIST_TABLE_CANVAS_STORAGE_TAG);
        this.canvasStorage.deserializeNBT(canvasNbt);

        if (this.canvasStorage.getContainerSize() != ArtistTableCanvasStorage.STORAGE_SIZE)
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);

        int tileEntityType = 43;
        return new ClientboundBlockEntityDataPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        this.load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        this.load(tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(Level world, BlockPos blockPos) {
        Containers.dropContents(world, blockPos, this.canvasStorage);
    }

    //

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.zetter.artistTable");
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
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player playerEntity) {
        return ArtistTableMenu.createContainerServerSide(windowID, playerInventory, this.canvasStorage, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    static public boolean isItemValidForCanvasArea(ItemStack itemStack)
    {
        return itemStack.getItem() == ModItems.CANVAS;
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