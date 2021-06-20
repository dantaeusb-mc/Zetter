package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class CanvasItem extends Item
{
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

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
     * @see {@link FilledMapItem#getCustomMapData(ItemStack, World)}
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, World world) {
        CanvasData canvasData = null;
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker != null) {
            canvasData = canvasTracker.getCanvasData(getCanvasCode(stack), CanvasData.class);
        } else {
            Zetter.LOG.error("Unable to find CanvasTracker capability");
        }

        // @todo: Maybe throw an exception if it's happening on client-side?
        if ((canvasData == null || canvasData.getName().equals(Helper.FALLBACK_CANVAS_CODE)) && world instanceof ServerWorld) {
            canvasData = createCanvasData(stack, world);
        }

        return canvasData;
    }

    /**
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     * @return
     */
    public static String getCanvasCode(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

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
        CanvasData canvasData = Helper.createNewCanvas(worldIn);
        canvasData.initData(Helper.getResolution().getNumeric(), Helper.getResolution().getNumeric());

        CanvasItem.setCanvasCode(stack, canvasData.getName());

        return canvasData;
    }
}