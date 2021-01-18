package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.item.ILockingItem;
import com.dantaeusb.immersivemp.locks.item.KeyItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class Helper {
    public static final int DOOR_QUICK_ACTION_TIMEOUT = 30;

    public static final String NBT_TAG_NAME_KEY_UUID = "keyUuid";

    /**
     * @todo use tag
     * @param stack
     * @return
     */
    public static boolean isLockableItem(ItemStack stack) {
        ResourceLocation lockableDoorsTagId = new ResourceLocation(ImmersiveMp.MOD_ID, ModLockItems.LOCKABLE_DOOR_TAG_ID);
        ITag<Item> lockableDoorsTag = ItemTags.getCollection().get(lockableDoorsTagId);

        if (lockableDoorsTag == null) {
            ImmersiveMp.LOG.error("Unable to find tag: " + lockableDoorsTagId);
            return false;
        } else {
            ImmersiveMp.LOG.info(lockableDoorsTag.getAllElements());
        }

        return lockableDoorsTag.contains(stack.getItem());
    }

    /**
     * @param stack
     * @return
     */
    public static boolean isKey(ItemStack stack) {
        return stack.getItem() == ModLockItems.KEY_ITEM;
    }

    /**
     * @param stack
     * @return
     */
    public static boolean isLock(ItemStack stack) {
        return stack.getItem() == ModLockItems.LOCK_ITEM;
    }

    public static boolean playerHasKey(PlayerEntity playerEntity, UUID requiredKeyId) {
        ImmersiveMp.LOG.debug("Looking for required key " + requiredKeyId + " in " + playerEntity + " inventory");

        for (ItemStack stackInSlot: playerEntity.inventory.mainInventory) {
            if (!Helper.isKey(stackInSlot)) {
                continue;
            }

            UUID keyId = ILockingItem.getLockId(stackInSlot);

            if (requiredKeyId.equals(keyId)) {
                ImmersiveMp.LOG.debug("Found required key " + keyId + " in " + playerEntity + " inventory");
                return true;
            } else {
                ImmersiveMp.LOG.debug("Found key " + keyId + " in " + playerEntity + " inventory");
            }
        }

        ImmersiveMp.LOG.debug("Unable to find required key " + requiredKeyId + " in " + playerEntity + " inventory");

        return false;
    }
}
