package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class FrameItem extends Item {
    private CustomPaintingEntity.Materials material;
    private boolean hasPlate;

    public static final String NBT_TAG_CACHED_PAINTING_NAME = "CachedPaintingName";
    public static final String NBT_TAG_CACHED_AUTHOR_NAME = "CachedAuthorName";

    public FrameItem(CustomPaintingEntity.Materials material, boolean plated) {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));

        this.material = material;
        this.hasPlate = plated;
    }

    public ITextComponent getDisplayName(ItemStack stack) {
        if (stack.hasTag()) {
            if (StringUtils.isNullOrEmpty(getCanvasCode(stack))) {
                return super.getDisplayName(stack);
            }

            String paintingName = getCachedPaintingName(stack);

            if (StringUtils.isNullOrEmpty(paintingName)) {
                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtils.isNullOrEmpty(paintingName)) {
                return new StringTextComponent(paintingName);
            }
        }

        return super.getDisplayName(stack);
    }

    public CustomPaintingEntity.Materials getMaterial() {
        return this.material;
    }

    public boolean hasPlate() {
        return this.hasPlate;
    }

    public static void setPaintingData(ItemStack stack, PaintingData paintingData) {
        setCanvasCode(stack, paintingData.getName());

        setCachedAuthorName(stack, paintingData.getAuthorName());
        setCachedPaintingName(stack, paintingData.getPaintingName());
    }

    @Nullable
    public static PaintingData getPaintingData(ItemStack stack) {
        return Helper.getWorldCanvasTracker().getCanvasData(getCanvasCode(stack), PaintingData.class);
    }

    public static void setCanvasCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, canvasCode);
    }

    public static void setBlockSize(ItemStack stack, int[] blockSize) {
        stack.getOrCreateTag().putIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE, blockSize);
    }

    public static int[] getBlockSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE);
    }

    public static void setCachedAuthorName(ItemStack stack, String authorName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_AUTHOR_NAME, authorName);
    }

    public static String getCachedAuthorName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(NBT_TAG_CACHED_AUTHOR_NAME);
    }

    public static void setCachedPaintingName(ItemStack stack, String paintingName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_PAINTING_NAME, paintingName);
    }

    public static String getCachedPaintingName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_NAME);
    }

    // @todo: Restore caching of painting name and author name

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            PaintingData paintingData = getPaintingData(stack);

            if (paintingData == null) {
                return;
            }

            String authorName = getCachedAuthorName(stack);

            if (StringUtils.isNullOrEmpty(authorName)) {
                authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
            }

            tooltip.add((new TranslationTextComponent("book.byAuthor", authorName)).mergeStyle(TextFormatting.GRAY));

            String widthBlocks = Integer.toString((paintingData.getWidth() / paintingData.getResolution().getNumeric()));
            String heightBlocks = Integer.toString((paintingData.getHeight() / paintingData.getResolution().getNumeric()));
            TranslationTextComponent blockSizeString = (new TranslationTextComponent("item.zetter.painting.size", widthBlocks, heightBlocks));

            tooltip.add(blockSizeString.mergeStyle(TextFormatting.GRAY));
        }
    }

    /**
     * Hanging painting
     */

    public ActionResultType onItemUse(ItemUseContext context) {
        BlockPos blockPos = context.getPos();
        Direction direction = context.getFace();
        BlockPos facePos = blockPos.offset(direction);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getItem();

        if (player != null && !this.canPlace(player, direction, stack, facePos)) {
            return ActionResultType.FAIL;
        } else {
            if (FrameItem.getCanvasCode(stack).equals(Helper.FALLBACK_CANVAS_CODE)) {
                return ActionResultType.FAIL;
            }

            World world = context.getWorld();

            CustomPaintingEntity paintingEntity = new CustomPaintingEntity(
                    world, facePos, direction, this.material, this.hasPlate, getCanvasCode(stack), getBlockSize(stack)
            );

            if (paintingEntity.onValidSurface()) {
                if (!world.isRemote) {
                    paintingEntity.playPlaceSound();
                    world.addEntity(paintingEntity);
                }

                player.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                return ActionResultType.func_233537_a_(world.isRemote);
            } else {
                return ActionResultType.CONSUME;
            }
        }
    }

    /**
     * @param stack
     * @return
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     */
    public static String getCanvasCode(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        String canvasCode = Helper.FALLBACK_CANVAS_CODE;
        if (compoundNBT != null && compoundNBT.contains(CustomPaintingEntity.NBT_TAG_PAINTING_CODE)) {
            canvasCode = compoundNBT.getString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE);
        }

        return canvasCode;
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }
}