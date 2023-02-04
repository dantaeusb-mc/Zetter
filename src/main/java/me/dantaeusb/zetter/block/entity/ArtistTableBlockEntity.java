package me.dantaeusb.zetter.block.entity;

import me.dantaeusb.zetter.block.entity.container.ArtistTableGridContainer;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.ZetterBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntArray;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class ArtistTableBlockEntity extends TileEntity implements ItemStackHandlerListener, INamedContainerProvider {
    // @todo: [LOW] Remove before release: transition 0.16 - 0.17
    private static final String NBT_TAG_DEPRECATED_ARTIST_TABLE_CANVAS_STORAGE = "canvas_storage";
    private static final String NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE = "CanvasStorage";
    private static final String NBT_TAG_ARTIST_TABLE_MODE = "Mode";

    public static final int DATA_MODE = 0;

    private final IIntArray dataAccess = new IntArray(1) {
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
    private final LazyOptional<ItemStackHandler> artistTableContainerOptional = LazyOptional.of(() -> this.artistTableGridContainer);

    private ArtistTableMenu.Mode mode = ArtistTableMenu.Mode.COMBINE;

    public ArtistTableBlockEntity(BlockPos pos, BlockState state) {
        super(ZetterBlockEntities.ARTIST_TABLE_BLOCK_ENTITY, pos, state);
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

    public boolean canPlayerEntityAccessInventory(PlayerEntity player) {
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

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return new AxisAlignedBB(this.getBlockPos(), this.getBlockPos().offset(1, 1, 1));
    }

    // NBT stack

    @Override
    public void saveAdditional(CompoundNBT compoundTag)
    {
        CompoundNBT gridContainer = this.artistTableGridContainer.serializeNBT();
        compoundTag.put(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE, gridContainer);
        compoundTag.putByte(NBT_TAG_ARTIST_TABLE_MODE, this.mode.getId());
    }

    @Override
    public void load(BlockState blockState, CompoundNBT compoundTag) {
        super.load(blockState, compoundTag);

        CompoundNBT canvasStorageTag;

        if (compoundTag.contains(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE)) {
            canvasStorageTag = compoundTag.getCompound(NBT_TAG_ARTIST_TABLE_CANVAS_STORAGE);
        } else {
            canvasStorageTag = compoundTag.getCompound(NBT_TAG_DEPRECATED_ARTIST_TABLE_CANVAS_STORAGE);
        }

        this.artistTableGridContainer.deserializeNBT(canvasStorageTag);

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
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);

        int tileEntityType = 43;
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
        this.saveAdditional(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundNBT tag)
    {
        this.load(tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(World world, BlockPos blockPos) {
        for (int i = 0; i < this.artistTableGridContainer.getSlots(); i++) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.artistTableGridContainer.getStackInSlot(i));
        }
    }
    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.zetter.artistTable");
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == ForgeCapabilities.ITEM_HANDLER
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
    public Container createMenu(int windowID, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return ArtistTableMenu.createMenuServerSide(
                windowID, playerInventory, this.artistTableGridContainer,
                this.dataAccess, IWorldPosCallable.create(this.level, this.getBlockPos())
        );
    }
}
