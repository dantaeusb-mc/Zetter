package me.dantaeusb.zetter.block.entity;

import me.dantaeusb.zetter.block.entity.container.ArtistTableGridContainer;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterBlockEntities;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class ArtistTableBlockEntity extends BlockEntity implements ItemStackHandlerListener, MenuProvider {
    // @todo: [LOW] Remove before release: transition 0.16 - 0.17
    private static final String NBT_TAG_DEPRECATED_ARTIST_TABLE_CANVAS_STORAGE = "canvas_storage";
    private static final String NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE = "CanvasStorage";
    private static final String NBT_TAG_ARTIST_TABLE_MODE = "Mode";

    public static final int DATA_MODE = 0;

    private final ContainerData dataAccess = new ContainerData() {
        public int get(int slot) {
            if (slot == DATA_MODE) {
                return ArtistTableBlockEntity.this.mode.getId();
            }

            return 0;
        }

        public void set(int slot, int value) {
            if (slot == DATA_MODE) {
                ArtistTableBlockEntity.this.mode = ArtistTableMenu.Mode.getById((byte) value);
            }
        }

        public int getCount() {
            return 1;
        }
    };

    private ArtistTableGridContainer artistTableGridContainer;

    private ArtistTableMenu.Mode mode = ArtistTableMenu.Mode.COMBINE;

    public ArtistTableBlockEntity(BlockPos pos, BlockState state) {
        super(ZetterBlockEntities.ARTIST_TABLE_BLOCK_ENTITY.get(), pos, state);
        this.createInventory();
    }

    public ArtistTableMenu.Mode getMode() {
        return this.mode;
    }

    public void setMode(ArtistTableMenu.Mode mode) {
        this.mode = mode;
    }

    protected void createInventory() {
        ArtistTableGridContainer currentArtistTableContainer = this.artistTableGridContainer;
        this.artistTableGridContainer = new ArtistTableGridContainer(this);

        if (currentArtistTableContainer != null) {
            int i = Math.min(currentArtistTableContainer.getSlots(), this.artistTableGridContainer.getSlots());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = currentArtistTableContainer.getStackInSlot(j);
                if (!itemstack.isEmpty()) {
                    this.artistTableGridContainer.setStackInSlot(j, itemstack.copy());
                }
            }
        }

        this.artistTableGridContainer.addListener(this);
    }

    public ArtistTableGridContainer getArtistTableGridContainer() {
        return this.artistTableGridContainer;
    }

    public boolean canPlayerAccessInventory(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    /**
     * @todo: [LOW] Do we need that?
     * @param container
     * @param slot
     */
    public void containerChanged(ItemStackHandler container, int slot) {
        this.setChanged();
    }



    // NBT stack

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries)
    {
        super.saveAdditional(compoundTag, registries);
        CompoundTag gridContainer = this.artistTableGridContainer.serializeNBT(registries);
        compoundTag.put(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE, gridContainer);
        compoundTag.putByte(NBT_TAG_ARTIST_TABLE_MODE, this.mode.getId());
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);

        CompoundTag canvasStorageTag;

        if (compoundTag.contains(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE)) {
            canvasStorageTag = compoundTag.getCompound(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE);
        } else {
            canvasStorageTag = compoundTag.getCompound(NBT_TAG_DEPRECATED_ARTIST_TABLE_CANVAS_STORAGE);
        }

        this.artistTableGridContainer.deserializeNBT(registries, canvasStorageTag);

        if (this.artistTableGridContainer.getSlots() != ArtistTableGridContainer.STORAGE_SIZE) {
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
        }

        if (compoundTag.contains(NBT_TAG_ARTIST_TABLE_MODE)) {
            byte modeId = compoundTag.getByte(NBT_TAG_ARTIST_TABLE_MODE);
            this.mode = ArtistTableMenu.Mode.getById(modeId);
        } else {
            this.mode = ArtistTableMenu.Mode.COMBINE;
        }
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        this.loadAdditional(packet.getTag(), registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        this.saveAdditional(nbtTagCompound, registries);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        this.loadAdditional(tag, registries);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(Level world, BlockPos blockPos) {
        for (int i = 0; i < this.artistTableGridContainer.getSlots(); i++) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.artistTableGridContainer.getStackInSlot(i));
        }
    }
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.zetter.artistTable");
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
        return ArtistTableMenu.createMenuServerSide(
                windowID, playerInventory, this.artistTableGridContainer,
                this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos())
        );
    }
}
