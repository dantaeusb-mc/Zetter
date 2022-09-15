package me.dantaeusb.zetter.block.entity;

import me.dantaeusb.zetter.block.entity.container.ArtistTableGridContainer;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.ZetterBlockEntities;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class ArtistTableBlockEntity extends BlockEntity implements ItemStackHandlerListener, MenuProvider {
    private static final String ARTIST_TABLE_CANVAS_STORAGE_TAG = "canvas_storage";

    protected ArtistTableGridContainer artistTableContainer;
    protected final LazyOptional<ItemStackHandler> artistTableContainerOptional = LazyOptional.of(() -> this.artistTableContainer);

    public ArtistTableBlockEntity(BlockPos pos, BlockState state) {
        super(ZetterBlockEntities.ARTIST_TABLE_BLOCK_ENTITY.get(), pos, state);
        this.createInventory();
    }

    protected void createInventory() {
        ArtistTableGridContainer currentArtistTableContainer = this.artistTableContainer;
        this.artistTableContainer = new ArtistTableGridContainer(this);

        if (currentArtistTableContainer != null) {
            int i = Math.min(currentArtistTableContainer.getSlots(), this.artistTableContainer.getSlots());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = currentArtistTableContainer.getStackInSlot(j);
                if (!itemstack.isEmpty()) {
                    this.artistTableContainer.setStackInSlot(j, itemstack.copy());
                }
            }
        }
    }

    public boolean canPlayerAccessInventory(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public void containerChanged(ItemStackHandler container) {
        this.setChanged();
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 1, 1));
    }

    // NBT stack

    @Override
    public void saveAdditional(CompoundTag compoundTag)
    {
        CompoundTag canvasNbt = this.artistTableContainer.serializeNBT();
        compoundTag.put(ARTIST_TABLE_CANVAS_STORAGE_TAG, canvasNbt);
    }

    @Override
    public void load(CompoundTag parentNBTTagCompound)
    {
        super.load(parentNBTTagCompound);

        CompoundTag canvasNbt = parentNBTTagCompound.getCompound(ARTIST_TABLE_CANVAS_STORAGE_TAG);
        this.artistTableContainer.deserializeNBT(canvasNbt);

        if (this.artistTableContainer.getSlots() != ArtistTableGridContainer.STORAGE_SIZE)
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        this.load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        this.saveAdditional(nbtTagCompound);
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
        for (int i = 0; i < this.artistTableContainer.getSlots(); i++) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.artistTableContainer.getStackInSlot(i));
        }
    }
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.zetter.artistTable");
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                && (direction == null || direction == Direction.UP || direction == Direction.DOWN)) {
            return this.artistTableContainerOptional.cast();
        }

        return super.getCapability(capability, direction);
    }

    /**
     * @param windowID
     * @param playerInventory
     * @param playerEntity
     * @return
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player playerEntity) {
        return ArtistTableMenu.createMenuServerSide(windowID, playerInventory, this.artistTableContainer);
    }
}
