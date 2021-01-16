package com.dantaeusb.immersivemp.locks.item;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.UUID;

public class KeyItem extends AbstractLockItem
{
    public KeyItem() {
        super(new Item.Properties().maxStackSize(AbstractLockItem.MAX_STACK_SIZE).group(ItemGroup.TOOLS));
    }
}