package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.ICanvasTracker;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public class CanvasItem extends AbstractLockItem
{
    public static final String NBT_TAG_NAME_CANVAS_ID = "canvasId";
    public static int CANVAS_SIZE = 16;
    public static int CANVAS_SQUARE = CANVAS_SIZE * CANVAS_SIZE;
    public static int CANVAS_BYTE_SIZE = CANVAS_SQUARE * 4;

    public CanvasItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }

    public static ItemStack setupNewCanvas(World worldIn) {
        ItemStack canvasStack = new ItemStack(ModLockItems.CANVAS_ITEM);
        createCanvasData(canvasStack, worldIn);

        return canvasStack;
    }

    /**
     *
     * @see {@link FilledMapItem#getData(ItemStack, World)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    public static CanvasData getData(ItemStack stack, World world) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(getCanvasName(stack));
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
        // FORGE: Add instance method for mods to override
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
     * @param worldIn
     * @return
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, World worldIn) {
        CanvasData canvasData = getData(stack, worldIn);

        if ((canvasData == null || canvasData.getName() == CanvasData.getCanvasName(0)) && worldIn instanceof ServerWorld) {
            canvasData = createCanvasData(stack, worldIn);
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
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     * @param stack
     * @return
     */
    public static int getCanvasId(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        int canvasId = 0;
        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_NAME_CANVAS_ID)) {
            canvasId = compoundNBT.getInt(NBT_TAG_NAME_CANVAS_ID);
        } else {
            ImmersiveMp.LOG.warn("Cannot find NBT-saved canvas id");
        }

        return canvasId;
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
        canvasData.initData(CANVAS_SIZE, CANVAS_SIZE);
        canvasTracker.registerCanvasData(canvasData);

        stack.getOrCreateTag().putInt(NBT_TAG_NAME_CANVAS_ID, newId);
        return canvasData;
    }
}