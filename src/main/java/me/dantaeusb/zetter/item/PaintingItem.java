package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingItem extends Item
{
    public static final String NBT_TAG_CACHED_PAINTING_NAME = "CachedPaintingName";
    public static final String NBT_TAG_CACHED_AUTHOR_NAME = "CachedAuthorName";
    public static final String NBT_TAG_CACHED_STRING_SIZE = "CachedStringSize";
    public static final String NBT_TAG_CACHED_BLOCK_SIZE = "CachedBlockSize";
    public static final String NBT_TAG_GENERATION = "Generation";

    public static final int GENERATION_ORIGINAL = 0;
    public static final int GENERATION_COPY = 1;
    public static final int GENERATION_COPY_OF_COPY = 2;

    public PaintingItem() {
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }

    public PaintingItem(Item.Properties properties) {
        super(properties);
    }

    public static void setPaintingData(ItemStack stack, String paintingCode, PaintingData paintingData, int generation) {
        setPaintingCode(stack, paintingCode);

        setCachedAuthorName(stack, paintingData.getAuthorName());
        setCachedPaintingName(stack, paintingData.getPaintingName());

        int widthBlocks = paintingData.getWidth() / paintingData.getResolution().getNumeric();
        int heightBlocks = paintingData.getHeight() / paintingData.getResolution().getNumeric();
        TranslatableComponent blockSizeString = (new TranslatableComponent("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        setBlockSize(stack, new int[]{widthBlocks, heightBlocks});
        setCachedStringSize(stack, blockSizeString.getString());
        setGeneration(stack, generation);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.hasTag()) {
            String authorName = getCachedAuthorName(stack);

            if (StringUtil.isNullOrEmpty(authorName)) {
                authorName = new TranslatableComponent("item.zetter.painting.unknown").getString();
            }

            tooltip.add((new TranslatableComponent("book.byAuthor", authorName)).withStyle(ChatFormatting.GRAY));

            String stringSize = getCachedStringSize(stack);

            if (!StringUtil.isNullOrEmpty(stringSize)) {
                tooltip.add((new TextComponent(stringSize)).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtil.isNullOrEmpty(paintingName)) {
                if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                    return super.getName(stack);
                }

                paintingName = new TranslatableComponent("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtil.isNullOrEmpty(paintingName)) {
                return new TextComponent(paintingName);
            }
        }

        return super.getName(stack);
    }

    protected static void setPaintingCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, canvasCode);
    }

    @Nullable
    public static String getPaintingCode(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE);
    }

    /**
     * Names are public for artist table preview
     */

    public static void setCachedAuthorName(ItemStack stack, String authorName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_AUTHOR_NAME, authorName);
    }

    @Nullable
    public static String getCachedAuthorName(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

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
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_NAME);
    }

    protected static void setBlockSize(ItemStack stack, int[] blockSize) {
        stack.getOrCreateTag().putIntArray(NBT_TAG_CACHED_BLOCK_SIZE, blockSize);
    }

    @Nullable
    public static int[] getBlockSize(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        // Migrate for new data format
        if (compoundNBT.contains(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE)) {
            int[] oldBlockSize = compoundNBT.getIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE);
            setBlockSize(stack, oldBlockSize);

            compoundNBT.remove(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE);
        }

        return compoundNBT.getIntArray(NBT_TAG_CACHED_BLOCK_SIZE);
    }

    protected static void setCachedStringSize(ItemStack stack, String stringSize) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_STRING_SIZE, stringSize);
    }

    @Nullable
    public static String getCachedStringSize(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_STRING_SIZE);
    }

    public static void setGeneration(ItemStack stack, int generation) {
        stack.getOrCreateTag().putInt(NBT_TAG_GENERATION, generation);
    }

    public static int getGeneration(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return 0;
        }

        return compoundNBT.getInt(NBT_TAG_GENERATION);
    }
}