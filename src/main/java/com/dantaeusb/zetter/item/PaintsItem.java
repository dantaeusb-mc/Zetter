package com.dantaeusb.zetter.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class PaintsItem extends Item
{
    public PaintsItem() {
        super(new Properties().maxStackSize(16).group(ItemGroup.TOOLS));
    }
}