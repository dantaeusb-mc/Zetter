package me.dantaeusb.zetter.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class PaintsItem extends Item
{
    public PaintsItem() {
        super(new Properties().tab(ItemGroup.TAB_TOOLS).stacksTo(16));
    }
}