package com.dantaeusb.immersivemp.locks.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TallBlockItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class LockableDoorItem extends TallBlockItem implements ILockingItem {
    public LockableDoorItem(Block blockIn, Item.Properties builder) {
        super(blockIn, builder);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendKeyDescription(ItemStack stack, java.util.List<ITextComponent> tooltip) {
        UUID keyUUID = ILockingItem.getLockId(stack);
        tooltip.add(new StringTextComponent(keyUUID.toString()));
    }

    /**
     * allows items to add custom lines of information to the mouseover description
     */
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        appendKeyDescription(stack, tooltip);
    }
}
