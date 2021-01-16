package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public abstract class AbstractLockItem extends Item implements ILockingItem {
    public static final int MAX_STACK_SIZE = 1; // maximum stack size

    public AbstractLockItem(Item.Properties builder) {
        super(builder);
    }

    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        UUID newKeyId = UUID.randomUUID();
        ILockingItem.setLockId(stack, newKeyId);

        ImmersiveMp.LOG.warn("Key Created with UUID" + newKeyId);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendKeyDescription(ItemStack stack, java.util.List<ITextComponent> tooltip) {
        UUID keyUUID = ILockingItem.getLockId(stack);
        tooltip.add(new StringTextComponent(keyUUID.toString()));
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendItemName(ItemStack stack, java.util.List<ITextComponent> tooltip) {
        tooltip.add(stack.getItem().getName());
    }

    /**
     * Allows items to add custom lines of information to the mouseover description
     * Show UUID in debug mode
     * Show type of item if ItemStack is named
     * I.E.
     * Town Hall
     * Key
     * Don't show anything if unnamed
     * I.E.
     * Lock
     */
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (ImmersiveMp.DEBUG_MODE) {
            appendKeyDescription(stack, tooltip);
        } else {
            if (stack.hasDisplayName()) {
                appendItemName(stack, tooltip);
            }
        }
    }
}
