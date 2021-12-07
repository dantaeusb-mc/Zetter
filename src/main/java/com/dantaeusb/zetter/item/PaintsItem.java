package com.dantaeusb.zetter.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;

import net.minecraft.world.item.Item.Properties;

public class PaintsItem extends Item
{
    public PaintsItem() {
        super(new Properties().stacksTo(16).tab(CreativeModeTab.TAB_TOOLS));
    }
}