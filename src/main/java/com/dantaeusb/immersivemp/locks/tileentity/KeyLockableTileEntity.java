package com.dantaeusb.immersivemp.locks.tileentity;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.core.ModLockSounds;
import com.dantaeusb.immersivemp.locks.core.ModLockTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.DoorHingeSide;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

public class KeyLockableTileEntity extends TileEntity implements ITickableTileEntity {
    private UUID keyId;
    private boolean locked = true;
    private int quickOpenTimeout = -1;

    public KeyLockableTileEntity() {
        super(ModLockTileEntities.LOCKING_TILE_ENTITY);
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
        this.markDirty();
    }

    public UUID getKeyId() {
        return this.keyId;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        this.markDirty();
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean toggleLock() {
        this.setLocked(!this.locked);
        this.playLockSound(this.locked);

        return this.locked;
    }

    public void tick() {
        if (quickOpenTimeout >= 0) {
            if (quickOpenTimeout == 0) {
                this.openDoor(false);
            }

            quickOpenTimeout--;
        }
    }

    public void read(BlockState state, CompoundNBT compoundNBT) {
        super.read(state, compoundNBT);

        if (compoundNBT.contains(Helper.NBT_TAG_NAME_KEY_UUID)) {
            this.keyId = compoundNBT.getUniqueId(Helper.NBT_TAG_NAME_KEY_UUID);
        } else {
            ImmersiveMp.LOG.warn("Unable to load door key ID from NBT");
            this.keyId = UUID.randomUUID();
        }

        if (compoundNBT.contains("locked")) {
            this.locked = compoundNBT.getBoolean("locked");
        } else {
            this.locked =  true;
        }

        if (compoundNBT.contains("quickActionTimeout")) {
            this.quickOpenTimeout = compoundNBT.getInt("quickActionTimeout");
        }
    }

    public CompoundNBT write(CompoundNBT parentNBTTagCompound) {
        super.write(parentNBTTagCompound);

        if (this.keyId != null) {
            parentNBTTagCompound.putUniqueId(Helper.NBT_TAG_NAME_KEY_UUID, this.keyId);
        }

        parentNBTTagCompound.putBoolean("locked", this.locked);
        parentNBTTagCompound.putInt("quickActionTimeout", this.quickOpenTimeout);

        return parentNBTTagCompound;
    }

    public void activateLockTimeout() {
        this.quickOpenTimeout = Helper.DOOR_QUICK_ACTION_TIMEOUT;
    }

    /**
     * Toggle state by default
     */
    public void openDoor() {
        BlockState doorBlockState = this.getBlockState();

        this.openDoor(!doorBlockState.get(DoorBlock.OPEN));
    }

    /**
     * Change block state
     * @param open
     */
    public void openDoor(boolean open) {

        BlockState doorBlockState = this.getBlockState();

        this.playOpenSound(open);
        this.setOpenProperty(doorBlockState, open);

        if (ImmersiveMp.quarkEnabled) {
            this.openDoubleDoor(open);
        }

        if (open) {
            if (this.isLocked()) {
                this.activateLockTimeout();
            }
        } else {
            if (this.isLocked()) {
                this.quickOpenTimeout = -1;
            }
        }
    }

    /**
     * Imitate Quark double door behavior
     * If Quark is enabled, only close door for quick actions
     */
    private void openDoubleDoor(boolean open) {
        ImmersiveMp.LOG.debug("Quark enabled, trying to toggle next door");

        BlockState currentDoorState = this.getBlockState();
        Direction direction = currentDoorState.get(DoorBlock.FACING);
        boolean isOpen = currentDoorState.get(DoorBlock.OPEN);
        DoorHingeSide isMirrored = currentDoorState.get(DoorBlock.HINGE);

        BlockPos mirrorPos = pos.offset(isMirrored == DoorHingeSide.RIGHT ? direction.rotateYCCW() : direction.rotateY());
        BlockPos doorPos = currentDoorState.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? mirrorPos : mirrorPos.down();
        BlockState secondDoorState = this.getWorld().getBlockState(doorPos);

        // @todo: add second door check

        if(secondDoorState.getBlock() == currentDoorState.getBlock() && secondDoorState.get(DoorBlock.FACING) == direction && secondDoorState.get(DoorBlock.OPEN) != isOpen && secondDoorState.get(DoorBlock.HINGE) != isMirrored) {
            BlockState newState = secondDoorState.with(DoorBlock.OPEN, open);
            this.getWorld().setBlockState(doorPos, newState);
        }
    }

    private void setOpenProperty(BlockState state, boolean open) {
        ImmersiveMp.LOG.debug("Set state open: " + open);
        this.world.setBlockState(this.getPos(), state.with(DoorBlock.OPEN, Boolean.valueOf(open)), 10);
    }

    public boolean canOpen(PlayerEntity playerEntity, World worldIn) {
        ImmersiveMp.LOG.debug("Check can open door");

        if (worldIn.isRemote()) {
            return false;
        }

        if (playerEntity.isSpectator()) {
            return false;
        }

        if (!this.isLocked()) {
            return true;
        }

        return canUnlock(playerEntity);
    }

    public boolean canUnlock(PlayerEntity playerEntity) {
        ImmersiveMp.LOG.debug("Check can unlock door");

        if (playerEntity.isSpectator()) {
            return false;
        }

        return Helper.playerHasKey(playerEntity, this.getKeyId());
    }

    /**
     *
     * @todo fix closing sound on placement
     * @param isOpening
     */
    private void playOpenSound(boolean isOpening) {
        this.world.playEvent(null, isOpening ? this.getOpenSound() : this.getCloseSound(), this.getPos(), 0);
    }

    private void playLockSound(boolean isLocking) {
        this.world.playSound(null, this.getPos(), isLocking ? this.getLockSound() : this.getUnlockSound(), SoundCategory.BLOCKS, 0.5F, 1F);
    }

    /**
     * Copy of original block's behavior
     * @see DoorBlock#getCloseSound()
     * @return
     */
    private int getCloseSound() {
        return 1012;
    }

    /**
     * Copy of original block's behavior
     * @see DoorBlock#getOpenSound()
     * @return
     */
    private int getOpenSound() {
        return 1006;
    }

    private SoundEvent getLockSound() {
        return ModLockSounds.BLOCK_DOOR_LOCK;
    }

    private SoundEvent getUnlockSound() {
        return ModLockSounds.BLOCK_DOOR_UNLOCK;
    }

    @OnlyIn(Dist.CLIENT)
    private static void playLockedEvent(PlayerEntity playerEntity) {
        playerEntity.sendStatusMessage(new TranslationTextComponent("container.isLocked", "The door"), true);
    }
}
