package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingItem extends Item
{
    public static final String NBT_TAG_CACHED_PAINTING_NAME = "CachedPaintingName";
    public static final String NBT_TAG_CACHED_AUTHOR_NAME = "CachedAuthorName";
    public static final String NBT_TAG_CACHED_STRING_SIZE = "CachedStringSize";

    public PaintingItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }

    public PaintingItem(Item.Properties properties) {
        super(properties);
    }

    public static void setPaintingData(ItemStack stack, PaintingData paintingData) {
        setPaintingCode(stack, paintingData.getName());

        setCachedAuthorName(stack, paintingData.getAuthorName());
        setCachedPaintingName(stack, paintingData.getPaintingName());

        String widthBlocks = Integer.toString((paintingData.getWidth() / paintingData.getResolution().getNumeric()));
        String heightBlocks = Integer.toString((paintingData.getHeight() / paintingData.getResolution().getNumeric()));
        TranslationTextComponent blockSizeString = (new TranslationTextComponent("item.zetter.painting.size", widthBlocks, heightBlocks));

        setCachedStringSize(stack, blockSizeString.getString());
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            String authorName = getCachedAuthorName(stack);

            if (StringUtils.isNullOrEmpty(authorName)) {
                authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
            }

            tooltip.add((new TranslationTextComponent("book.byAuthor", authorName)).mergeStyle(TextFormatting.GRAY));

            String stringSize = getCachedStringSize(stack);

            if (!StringUtils.isNullOrEmpty(stringSize)) {
                tooltip.add((new StringTextComponent(stringSize)).mergeStyle(TextFormatting.GRAY));
            }
        }
    }

    public ITextComponent getDisplayName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtils.isNullOrEmpty(paintingName)) {
                if (StringUtils.isNullOrEmpty(getPaintingCode(stack))) {
                    return super.getDisplayName(stack);
                }

                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtils.isNullOrEmpty(paintingName)) {
                return new StringTextComponent(paintingName);
            }
        }

        return super.getDisplayName(stack);
    }

    /**
     * It's there but we probably should avoid this
     * @param stack
     * @return
     */
    @Deprecated
    @Nullable
    public static PaintingData getPaintingData(ItemStack stack) {
        return Helper.getWorldCanvasTracker().getCanvasData(getPaintingCode(stack), PaintingData.class);
    }

    public static void setPaintingCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, canvasCode);
    }

    @Nullable
    public static String getPaintingCode(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE);
    }

    public static void setCachedAuthorName(ItemStack stack, String authorName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_AUTHOR_NAME, authorName);
    }

    @Nullable
    public static String getCachedAuthorName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_AUTHOR_NAME);
    }

    public static void setCachedPaintingName(ItemStack stack, String paintingName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_PAINTING_NAME, paintingName);
    }

    @Nullable
    public static String getCachedPaintingName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_NAME);
    }

    public static void setCachedStringSize(ItemStack stack, String stringSize) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_STRING_SIZE, stringSize);
    }

    @Nullable
    public static String getCachedStringSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_STRING_SIZE);
    }
}