package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.storage.CanvasData;
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

    /**
     * @see {@link FilledMapItem#getCustomMapData(ItemStack, World)}
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, Level world) {
        CanvasData canvasData = null;
        String canvasCode = getCanvasCode(stack);

        if (canvasCode == null && world instanceof ServerLevel) {
            canvasCode = createNewCanvasData(world);
            setCanvasCode(stack, canvasCode);
        }

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker != null) {
            canvasData = canvasTracker.getCanvasData(canvasCode, CanvasData.class);
        } else {
            Zetter.LOG.error("Unable to find CanvasTracker capability");
        }

        if (canvasData == null && world instanceof ServerLevel) {
            Zetter.LOG.error("Unable to find canvas data after creation");
        }

        return canvasData;
    }

    /**
     * @see {@link FilledMapItem#getMapId(ItemStack)}
     * @return
     */
    public static @Nullable String getCanvasCode(@Nullable ItemStack stack) {
        if (stack == null || !stack.is(ZetterItems.CANVAS)) {
            return null;
        }

        CompoundTag compoundNBT = stack.getTag();

        String canvasCode = null;

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

    private static void createAndStoreCanvasData(ItemStack stack, Level world) {
        String canvasCode = createNewCanvasData(world);
        setCanvasCode(stack, canvasCode);
    }

    /**
     *
     * @see {@link FilledMapItem#createNewSavedData(ItemStack, World, int, int, int, boolean, boolean, RegistryKey)}
     * @param stack
     * @param worldIn
     * @return
     */
    private static String createNewCanvasData(Level world) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);
        final int numericResolution = Helper.getResolution().getNumeric();

        CanvasData canvasData = CanvasData.createFresh(Helper.getResolution(), numericResolution, numericResolution);
        String canvasCode = CanvasData.getCanvasCode(canvasTracker.getNextCanvasId());
        canvasTracker.registerCanvasData(canvasCode, canvasData);

        return canvasCode;
    }
}