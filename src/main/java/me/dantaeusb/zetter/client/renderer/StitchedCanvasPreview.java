package me.dantaeusb.zetter.client.renderer;

import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.CanvasStitchingHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Client never runs crafting recipes, it only receives the result stack,
 * so the stitched canvas preview is built here from the parts that stack
 * carries, and registered locally under {@link Helper#COMBINED_CANVAS_CODE}.
 */
public class StitchedCanvasPreview {
    private static @Nullable CanvasStitchingHelper.StitchParts cachedParts;
    private static AbstractCanvasData[] cachedPartData = new AbstractCanvasData[0];
    private static @Nullable DummyCanvasData cachedPreview;

    public static @Nullable AbstractCanvasData getCanvasData(ItemStack stack, Level level) {
        final String canvasCode = CanvasItem.getCanvasCode(stack);

        if (Helper.COMBINED_CANVAS_CODE.equals(canvasCode)) {
            return getPreview(stack, level);
        }

        return Helper.getLevelCanvasTracker(level).getCanvasData(canvasCode);
    }

    private static @Nullable AbstractCanvasData getPreview(ItemStack stack, Level level) {
        final CanvasStitchingHelper.StitchParts stitchParts = CanvasStitchingHelper.StitchParts.readFrom(stack);

        if (stitchParts == null) {
            return null;
        }

        final CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(level);
        final AbstractCanvasData[] partData = new AbstractCanvasData[stitchParts.codes.length];
        boolean partsReady = true;

        for (int i = 0; i < stitchParts.codes.length; i++) {
            final String partCode = stitchParts.codes[i];

            if (partCode == null) {
                continue;
            }

            partData[i] = canvasTracker.getCanvasData(partCode);

            if (partData[i] == null) {
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(partCode);
                partsReady = false;
            }
        }

        if (!partsReady) {
            return null;
        }

        if (
            cachedPreview != null
            && canvasTracker.getCanvasData(Helper.COMBINED_CANVAS_CODE) == cachedPreview
            && stitchParts.equals(cachedParts)
            && sameInstances(partData, cachedPartData)
        ) {
            return cachedPreview;
        }

        final DummyCanvasData preview = CanvasStitchingHelper.createStitchedCanvasData(stitchParts, level, false);

        if (preview == null) {
            return null;
        }

        canvasTracker.registerCanvasData(Helper.COMBINED_CANVAS_CODE, preview);

        cachedParts = stitchParts;
        cachedPartData = partData;
        cachedPreview = preview;

        return preview;
    }

    /**
     * Frees the preview once nothing can show it anymore.
     * @param level
     */
    public static void release(Level level) {
        if (cachedPreview == null) {
            return;
        }

        final CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(level);

        if (canvasTracker.getCanvasData(Helper.COMBINED_CANVAS_CODE) == cachedPreview) {
            canvasTracker.unregisterCanvasData(Helper.COMBINED_CANVAS_CODE);
        }

        cachedParts = null;
        cachedPartData = new AbstractCanvasData[0];
        cachedPreview = null;
    }

    /**
     * Tracker replaces data object on every sync, so identity tells
     * whether a part changed since the preview was built
     */
    private static boolean sameInstances(AbstractCanvasData[] left, AbstractCanvasData[] right) {
        if (left.length != right.length) {
            return false;
        }

        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return false;
            }
        }

        return true;
    }
}
