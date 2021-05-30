package com.dantaeusb.zetter.item;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class CustomPaintingItem extends Item {
    public static final String NBT_TAG_CANVAS_NAME = "CanvasName";
    public static final String NBT_TAG_TITLE = "Title";
    public static final String NBT_TAG_AUTHOR = "Author";

    public CustomPaintingItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
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

    public static void setCanvasName(ItemStack stack, String canvasName) {
        stack.getOrCreateTag().putString(NBT_TAG_CANVAS_NAME, canvasName);
    }

    public static void setTitle(ItemStack stack, String title) {
        stack.getOrCreateTag().putString(NBT_TAG_TITLE, title);
    }

    public static String getTitle(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(NBT_TAG_TITLE);
    }

    public static void setAuthor(ItemStack stack, String author) {
        stack.getOrCreateTag().putString(NBT_TAG_AUTHOR, author);
    }

    public static String getAuthor(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        return compoundNBT.getString(NBT_TAG_AUTHOR);
    }

    /**
     * @param stack
     * @param worldIn
     * @return
     * @see {@link FilledMapItem#createMapData(ItemStack, World, int, int, int, boolean, boolean, RegistryKey)}
     */
    public static CanvasData copyCanvasData(ItemStack stack, CanvasData originalCanvasData, World worldIn) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(worldIn);

        int newId = canvasTracker.getNextId();

        CanvasData paintingCanvasData = new CanvasData(newId);
        paintingCanvasData.copyFrom(originalCanvasData);
        canvasTracker.registerCanvasData(paintingCanvasData);

        stack.getOrCreateTag().putString(NBT_TAG_CANVAS_NAME, paintingCanvasData.getName());
        return paintingCanvasData;
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            CompoundNBT nbt = stack.getTag();
            String s = nbt.getString(NBT_TAG_AUTHOR);
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
            World world = context.getWorld();

            String canvasName = getCanvasName(stack);
            CustomPaintingEntity paintingEntity = new CustomPaintingEntity(
                    world, facePos, direction, canvasName, getTitle(stack), getAuthor(stack)
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
    public static String getCanvasName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        String canvasName = CanvasData.getCanvasName(0);
        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_CANVAS_NAME)) {
            canvasName = compoundNBT.getString(NBT_TAG_CANVAS_NAME);
        }

        return canvasName;
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }
}