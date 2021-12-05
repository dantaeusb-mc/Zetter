package com.dantaeusb.zetter.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

import net.minecraft.item.Item.Properties;

public class PaintsItem extends Item
{
    public PaintsItem() {
        super(new Properties().stacksTo(16).tab(ItemGroup.TAB_TOOLS));
    }
}