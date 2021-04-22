package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class CanvasItem extends Item
{
    public static final String NBT_TAG_NAME_CANVAS_ID = "canvasId";

    public CanvasItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }

    /**
     *
     * @see {@link FilledMapItem#getMapData(ItemStack, World)}
     * @param stack
     * @param worldIn
     * @return
     */
    @Nullable
    public static CanvasData getCanvasData(ItemStack stack, World worldIn) {
        Item canvas = stack.getItem();
        if (canvas instanceof CanvasItem) {
            return ((CanvasItem)canvas).getCustomCanvasData(stack, worldIn);
        }

        return null;
    }

    /**
     *
     * @see {@link FilledMapItem#getCustomMapData(ItemStack, World)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, World world) {
        CanvasData canvasData = null;
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker != null) {
            canvasData = canvasTracker.getCanvasData(getCanvasName(stack));
        } else {
            Zetter.LOG.error("Unable to find CanvasTracker capability");
        }

        if ((canvasData == null || canvasData.getName().equals(CanvasData.getCanvasName(0))) && world instanceof ServerWorld) {
            canvasData = createCanvasData(stack, world);
        }

        return canvasData;
    }

    /**
     *
     * @see {@link FilledMapItem#getMapName(ItemStack)}
     * @param stack
     * @return
     */
    public static String getCanvasName(ItemStack stack) {
        return CanvasData.getCanvasName(getCanvasId(stack));
    }


    /**
     *
     * @see {@link FilledMapItem#getMapName(ItemStack)}
     * @param stack
     * @return
     */
    public static void setCanvasName(ItemStack stack, String canvasName) {
        String number = StringUtils.substring(canvasName, CanvasData.NAME_PREFIX.length());
        int id = Integer.parseInt(number);
        stack.getOrCreateTag().putInt(NBT_TAG_NAME_CANVAS_ID, id);
    }

    /**
     *
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     * @param stack
     * @return
     */
    public static int getCanvasId(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        int canvasId = 0;
        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_NAME_CANVAS_ID)) {
            canvasId = compoundNBT.getInt(NBT_TAG_NAME_CANVAS_ID);
        }

        return canvasId;
    }

    /**
     * Called when item is crafted/smelted. Used only by maps so far.
     */
    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        if (worldIn.isRemote) return;

        createCanvasData(stack, worldIn);
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

            CustomPaintingEntity paintingEntity = new CustomPaintingEntity(world, facePos, direction, getCanvasName(stack));

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

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }

}