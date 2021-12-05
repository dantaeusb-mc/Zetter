package com.dantaeusb.zetter.tileentity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModTileEntities;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.tileentity.storage.EaselStorage;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class EaselTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private final EaselStorage easelStorage; // two items: canvas and palette

    private static final String EASEL_STORAGE_TAG = "storage";

    /** The list of players currently using this easel */
    private ArrayList<PlayerEntity> playersUsing = new ArrayList<>();
    private int ticksSinceSync;

    public EaselTileEntity() {
        super(ModTileEntities.EASEL_TILE_ENTITY);

        this.easelStorage = EaselStorage.createForTileEntity(this::canPlayerAccessInventory, this::setChanged);
    }

    public boolean canPlayerAccessInventory(PlayerEntity player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public void tick() {
        // No need to track on client side
        if (this.level.isClientSide()) {
            return;
        }

        if (++this.ticksSinceSync > 200) {
            this.playersUsing = this.calculatePlayersUsing();
        }
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
        return this.easelStorage.getItem(EaselStorage.CANVAS_SLOT);
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.getItem() != ModItems.CANVAS) {
            return false;
        }

        if (this.hasCanvas()) {
            return false;
        }

        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.level);
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

        return CanvasItem.getCanvasCode(canvasStack);
    }

    @Nullable
    public CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ModItems.CANVAS) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.level);
    }

    // track using players to send packets

    public ArrayList<PlayerEntity> calculatePlayersUsing() {
        ArrayList<PlayerEntity> usingPlayers = new ArrayList<>();

        for(PlayerEntity player : this.level.getEntitiesOfClass(PlayerEntity.class, new AxisAlignedBB(this.worldPosition.offset(-5, -5, -5), this.worldPosition.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselContainer) {
                EaselStorage storage = ((EaselContainer)player.containerMenu).getEaselStorage();

                if (storage == this.getEaselStorage()) {
                    usingPlayers.add(player);
                }
            }
        }

        return usingPlayers;
    }

    public ArrayList<PlayerEntity> getPlayersUsing() {
        return this.playersUsing;
    }

    // render

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return new AxisAlignedBB(this.getBlockPos(), this.getBlockPos().offset(1, 2, 1));
    }

    // NBT stack

    @Override
    public CompoundNBT save(CompoundNBT parentNBTTagCompound)
    {
        super.save(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        CompoundNBT inventoryNBT = this.easelStorage.serializeNBT();
        parentNBTTagCompound.put(EASEL_STORAGE_TAG, inventoryNBT);

        return parentNBTTagCompound;
    }

    @Override
    public void load(BlockState blockState, CompoundNBT parentNBTTagCompound)
    {
        super.load(blockState, parentNBTTagCompound);

        CompoundNBT inventoryNBT = parentNBTTagCompound.getCompound(EASEL_STORAGE_TAG);
        this.easelStorage.deserializeNBT(inventoryNBT);

        if (this.easelStorage.getContainerSize() != EaselStorage.STORAGE_SIZE)
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
    }

    // network stack

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);

        int tileEntityType = 42;
        return new SUpdateTileEntityPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        BlockState blockState = level.getBlockState(worldPosition);
        load(blockState, packet.getTag());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag)
    {
        this.load(blockState, tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(World world, BlockPos blockPos) {
        InventoryHelper.dropContents(world, blockPos, this.easelStorage);
    }

    //

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.zetter.easel");
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
        return EaselContainer.createContainerServerSide(windowID, playerInventory, this, this.easelStorage);
    }
}
