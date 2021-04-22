package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class CustomPaintingItem extends Item
{
    public static final String NBT_TAG_NAME_CANVAS_ID = "canvasId";
    public CustomPaintingItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }


    public ITextComponent getDisplayName(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundNBT compoundNBT = stack.getTag();
            String s = compoundNBT.getString("title");
            if (!net.minecraft.util.StringUtils.isNullOrEmpty(s)) {
                return new StringTextComponent(s);
            }
        }

        return super.getDisplayName(stack);
    }

    /**
     *
     * @see {@link FilledMapItem#createMapData(ItemStack, World, int, int, int, boolean, boolean, RegistryKey)}
     * @param stack
     * @param worldIn
     * @return
     */
    private static CanvasData createCanvasData(ItemStack stack, World worldIn) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(worldIn);

        int newId = canvasTracker.getNextId();

        CanvasData canvasData = new CanvasData(newId);
        canvasData.initData(Helper.CANVAS_TEXTURE_RESOLUTION, Helper.CANVAS_TEXTURE_RESOLUTION);
        canvasTracker.registerCanvasData(canvasData);

        stack.getOrCreateTag().putInt(NBT_TAG_NAME_CANVAS_ID, newId);
        return canvasData;
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            CompoundNBT compoundnbt = stack.getTag();
            String s = compoundnbt.getString("author");
            if (!StringUtils.isNullOrEmpty(s)) {
                tooltip.add((new TranslationTextComponent("book.byAuthor", s)).mergeStyle(TextFormatting.GRAY));
            }

            //todo: add canvas size
        }
    }
}