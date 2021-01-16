package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

public interface ILockingItem {

    /**
     * Returns id of the key
     * @param stack
     * @return keyId
     */
    static UUID getLockId(ItemStack stack)
    {
        return getLockId(stack, false);
    }

    /**
     * Returns id of the key
     * @param stack
     * @return keyId
     */
    static UUID getLockId(ItemStack stack, boolean forceCreate)
    {
        CompoundNBT compoundNBT = stack.getTag();

        UUID keyId = null;
        if (compoundNBT != null && compoundNBT.contains(Helper.NBT_TAG_NAME_KEY_UUID)) {
            keyId = compoundNBT.getUniqueId(Helper.NBT_TAG_NAME_KEY_UUID);
        } else {
            ImmersiveMp.LOG.warn("Cannot find NBT-saved UUID, setting random");
            keyId = UUID.randomUUID();  // default in case of error
            setLockId(stack, keyId);
        }

        return keyId;
    }

    /**
     * Sets key id
     *
     * @param stack the stack
     * @param keyId the new UUID
     */
    static void setLockId(ItemStack stack, UUID keyId)
    {
        CompoundNBT compoundNBT = stack.getOrCreateTag();
        compoundNBT.putUniqueId(Helper.NBT_TAG_NAME_KEY_UUID, keyId);
    }
}
