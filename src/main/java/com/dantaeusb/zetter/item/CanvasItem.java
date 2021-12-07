package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CanvasItem extends Item
{
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    public CanvasItem() {
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }

    /**
     *
     * @see {@link FilledMapItem#getMapData(ItemStack, World)}
     * @param stack
     * @param worldIn
     * @return
     */
    @Nullable
    public static CanvasData getCanvasData(ItemStack stack, Level world) {
        Item canvas = stack.getItem();

        if (canvas instanceof CanvasItem) {
            return ((CanvasItem)canvas).getCustomCanvasData(stack, world);
        }

        return null;
    }

    @Nullable
    public static CanvasData getCanvasData(String canvasCode, Level world) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        return canvasTracker.getCanvasData(canvasCode, CanvasData.class);
    }

    /**
     * @see {@link FilledMapItem#getCustomMapData(ItemStack, World)}
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, Level world) {
        CanvasData canvasData = null;
        String canvasCode = getCanvasCode(stack);
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker != null) {
            canvasData = canvasTracker.getCanvasData(canvasCode, CanvasData.class);
        } else {
            Zetter.LOG.error("Unable to find CanvasTracker capability");
        }

        // @todo: Maybe throw an exception if it's happening on client-side?
        if ((canvasData == null || canvasCode.equals(Helper.FALLBACK_CANVAS_CODE)) && world instanceof ServerLevel) {
            canvasData = createNewCanvasData(stack, world);
        }

        return canvasData;
    }

    /**
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     * @return
     */
    public static String getCanvasCode(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        String canvasCode = Helper.FALLBACK_CANVAS_CODE;

        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_CANVAS_CODE)) {
            canvasCode = compoundNBT.getString(NBT_TAG_CANVAS_CODE);
        }

        return canvasCode;
    }

    /**
     *
     * @see {@link FilledMapItem#getMapName(ItemStack)}
     * @param stack
     * @return
     */
    public static void setCanvasCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(NBT_TAG_CANVAS_CODE, canvasCode);
    }

    /**
     * Called when item is crafted/smelted. Used only by maps so far.
     */
    public void onCraftedBy(ItemStack stack, Level worldIn, Player playerIn) {
        if (worldIn.isClientSide) return;

        createNewCanvasData(stack, worldIn);
    }

    /**
     *
     * @see {@link FilledMapItem#createMapData(ItemStack, World, int, int, int, boolean, boolean, RegistryKey)}
     * @param stack
     * @param worldIn
     * @return
     */
    private static CanvasData createNewCanvasData(ItemStack stack, Level world) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);
        final int numericResolution = Helper.getResolution().getNumeric();

        CanvasData canvasData = CanvasData.createFresh(Helper.getResolution(), numericResolution, numericResolution);
        String canvasCode = CanvasData.getCanvasCode(canvasTracker.getNextCanvasId());
        canvasTracker.registerCanvasData(canvasCode, canvasData);

        CanvasItem.setCanvasCode(stack, canvasCode);

        return canvasData;
    }
}