package com.dantaeusb.immersivemp.locks.tileentity;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.core.ModLockSounds;
import com.dantaeusb.immersivemp.locks.core.ModLockTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class KeyLockableTileEntity extends TileEntity implements ITickableTileEntity {
    private UUID keyId;
    private boolean locked = true;
    private int quickOpenTimeout = 0;

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

    public boolean getLocked() {
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
                this.playOpenSound(false);
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

    public boolean onActivated(PlayerEntity playerEntity, World worldIn, boolean isCrouching) {
        if (worldIn.isRemote()) {
            return false;
        } else {
            if (!canOpen(playerEntity, worldIn)) {
                return false;
            }

            if (!isCrouching) {
                // Process open/close action
                if (this.locked) {
                    ImmersiveMp.LOG.debug("Quick open locked");

                    this.quickOpenTimeout = Helper.DOOR_QUICK_ACTION_TIMEOUT;
                    this.openDoor(true);
                    this.playOpenSound(true);
                } else {
                    ImmersiveMp.LOG.debug("Quick open unlocked");
                    this.toggleDoorBlock();
                }
            } else {
                // Process lock/unlock action

                if (this.getBlockState().get(DoorBlock.OPEN)) {
                    this.openDoor(false);
                    this.playOpenSound(false);
                }

                ImmersiveMp.LOG.info("Toggling lock state");
                this.toggleLock();
            }
        }

        return true;
    }

    private void toggleDoorBlock() {
        BlockState doorBlockState = this.getBlockState();
        boolean newOpenState = !doorBlockState.get(DoorBlock.OPEN).booleanValue();

        this.setOpenProperty(doorBlockState, newOpenState);
        this.playOpenSound(newOpenState);
    }

    private void openDoor(boolean open) {
        BlockState doorBlockState = this.getBlockState();

        this.setOpenProperty(doorBlockState, open);
    }

    private void setOpenProperty(BlockState state, boolean open) {
        ImmersiveMp.LOG.debug("Set state open: " + open);
        this.world.setBlockState(this.getPos(), state.with(DoorBlock.OPEN, Boolean.valueOf(open)), 10);
    }

    public boolean canOpen(PlayerEntity playerEntity, World worldIn) {
        ImmersiveMp.LOG.info("Check can open door");

        if (worldIn.isRemote()) {
            return false;
        }

        if (playerEntity.isSpectator()) {
            return false;
        }

        if (!this.getLocked()) {
            return true;
        }

        return canUnlock(playerEntity);
    }

    public boolean canUnlock(PlayerEntity playerEntity) {
        ImmersiveMp.LOG.info("Check can unlock door");

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
