package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class FrameItem extends Item {
    private CustomPaintingEntity.Materials material;
    private boolean hasPlate;

    public FrameItem(CustomPaintingEntity.Materials material, boolean plated) {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));

        this.material = material;
        this.hasPlate = plated;
    }

    public ITextComponent getDisplayName(ItemStack stack) {
        if (stack.hasTag()) {
            String s = getTitle(stack);

            if (!net.minecraft.util.StringUtils.isNullOrEmpty(s)) {
                return new StringTextComponent(s);
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

    public static void setCanvasCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, canvasCode);
    }

    public static void setTitle(ItemStack stack, String title) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_TITLE, title);
    }

    public static String getTitle(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(CustomPaintingEntity.NBT_TAG_TITLE);
    }

    public static void setAuthor(ItemStack stack, String author) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_AUTHOR_NAME, author);
    }

    public static String getAuthor(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(CustomPaintingEntity.NBT_TAG_AUTHOR_NAME);
    }

    public static void setBlockSize(ItemStack stack, int[] blockSize) {
        stack.getOrCreateTag().putIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE, blockSize);
    }

    public static int[] getBlockSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE);
    }

    /**
     * @param stack
     * @param worldIn
     * @return
     * @see {@link FilledMapItem#createMapData(ItemStack, World, int, int, int, boolean, boolean, RegistryKey)}
     */
    public static PaintingData copyCanvasData(ItemStack stack, AbstractCanvasData originalCanvasData, World worldIn) {
        PaintingData paintingData = Helper.createNewPainting(worldIn);
        paintingData.copyFrom(originalCanvasData);

        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, paintingData.getName());
        return paintingData;
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            CompoundNBT nbt = stack.getTag();
            String s = nbt.getString(CustomPaintingEntity.NBT_TAG_AUTHOR_NAME);
            if (!StringUtils.isNullOrEmpty(s)) {
                tooltip.add((new TranslationTextComponent("book.byAuthor", s)).mergeStyle(TextFormatting.GRAY));
            }

            //todo: add canvas size
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
                    world, facePos, direction, this.material, this.hasPlate, getCanvasCode(stack), getTitle(stack), getAuthor(stack), getBlockSize(stack)
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