package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.ICanvasTracker;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.entity.player.PlayerEntity;
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
            ImmersiveMp.LOG.error("Unable to find CanvasTracker capability");
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
        canvasData.initData(CANVAS_SIZE, CANVAS_SIZE);
        canvasTracker.registerCanvasData(canvasData);

        stack.getOrCreateTag().putInt(NBT_TAG_NAME_CANVAS_ID, newId);
        return canvasData;
    }
}