package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TallBlockItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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

    @OnlyIn(Dist.CLIENT)
    public static void appendItemName(ItemStack stack, java.util.List<ITextComponent> tooltip) {
        tooltip.add(stack.getItem().getName());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ITextComponent getName() {
        return new TranslationTextComponent(this.getTranslationKey());
    }

    /**
     * Allows items to add custom lines of information to the mouseover description
     * Show UUID in debug mode
     * Show name of item if lock was named
     * I.E.
     * Town Hall
     * Spruce Lockable Door
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
