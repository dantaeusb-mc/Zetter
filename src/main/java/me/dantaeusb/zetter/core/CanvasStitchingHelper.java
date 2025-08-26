package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * Helper to handle events and combination of the combined canvas when stitching.
 */
public class CanvasStitchingHelper {
    public static void createStitchedCanvasAndWriteNewCanvasData(CraftingContainer craftingContainer, ItemStack canvasItemStack, Player player) {
        final CanvasStitchingHelper.CanvasGridRectangle canvasGridRectangle = CanvasStitchingHelper.getCraftingContainerCanvasRectangle(craftingContainer);

        if (canvasGridRectangle == null) {
            Zetter.LOG.warn("Failed to create combined canvas data: No valid canvas rectangle found in crafting inventory.");
            Zetter.LOG.debug("At this point, the crafting inventory should contain a valid rectangle of canvases.");
            return;
        }

        DummyCanvasData combinedCanvasData = CanvasStitchingHelper.createStitchedCanvasData(
            craftingContainer,
            canvasGridRectangle,
            player.level()
        );

        CanvasStitchingHelper.writeNewCanvasData(canvasItemStack, canvasGridRectangle, combinedCanvasData, player);
    }

    /**
     * Will create stitched canvas from container grid.
     * Will not request missing canvas data on client,
     * any item without data will be filled with default color!
     * This might be expected when stitching with empty canvases.
     * <p>
     * On client, make sure you loaded all parts before stitching.
     *
     * @param craftingInventory
     * @param canvasGridRectangle
     * @param level
     * @return
     */
    public static @Nullable DummyCanvasData createStitchedCanvasData(CraftingContainer craftingInventory, CanvasGridRectangle canvasGridRectangle, Level level) {
        final int COLOR_SIZE = 4;
        final int pixelWidth = canvasGridRectangle.width * canvasGridRectangle.canvasBlockSize[0] * Helper.getResolution().getNumeric();
        final int pixelHeight = canvasGridRectangle.height * canvasGridRectangle.canvasBlockSize[1] * Helper.getResolution().getNumeric();
        boolean hasColorData = false;
        AbstractCanvasData.Resolution resolution = Helper.getResolution();

        for (int i = 0; i < craftingInventory.getContainerSize(); i++) {
            final ItemStack itemStack = craftingInventory.getItem(i);

            if (CanvasItem.getCanvasData(itemStack, level) != null) {
                hasColorData = true;
                break;
            } else if (CanvasItem.getCanvasCode(itemStack) != null) {
                throw new IllegalStateException("Unable to stitch canvases: canvas has a code " + CanvasItem.getCanvasCode(itemStack) + ", but no data");
            }
        }

        if (!hasColorData) {
            return null;
        }

        ByteBuffer color = ByteBuffer.allocate(pixelWidth * pixelHeight * COLOR_SIZE);

        for (int slotY = canvasGridRectangle.y; slotY < canvasGridRectangle.y + canvasGridRectangle.height; slotY++) {
            for (int slotX = canvasGridRectangle.x; slotX < canvasGridRectangle.x + canvasGridRectangle.width; slotX++) {
                ItemStack canvasStack = craftingInventory.getItem(slotY * craftingInventory.getWidth() + slotX);

                CanvasData smallCanvasData = CanvasItem.getCanvasData(canvasStack, level);

                int relativeX = slotX - canvasGridRectangle.x;
                int relativeY = slotY - canvasGridRectangle.y;

                // @todo: add resolution!
                for (int smallY = 0; smallY < resolution.getNumeric() * canvasGridRectangle.y; smallY++) {
                    for (int smallX = 0; smallX < resolution.getNumeric() * canvasGridRectangle.x; smallX++) {
                        final int bigX = relativeX * Helper.getResolution().getNumeric() + smallX;
                        final int bigY = relativeY * Helper.getResolution().getNumeric() + smallY;

                        final int colorIndex = (bigY * pixelWidth + bigX) * COLOR_SIZE;

                        if (smallCanvasData != null) {
                            color.putInt(colorIndex, smallCanvasData.getColorAt(smallX, smallY));
                        } else {
                            color.putInt(colorIndex, Helper.CANVAS_COLOR);
                        }
                    }
                }
            }
        }

        return ZetterCanvasTypes.DUMMY.get().createWrap(
            Helper.getResolution(),
            pixelWidth,
            pixelHeight,
            color.array()
        );
    }

    /**
     * Writes and registers canvas after stitching with dummy canvas data.
     * If data is empty, an empty canvas of a new size is created.
     *
     * @param canvasItemStack
     * @param dummyCanvasData
     */
    public static void writeNewCanvasData(ItemStack canvasItemStack, CanvasGridRectangle canvasGridRectangle, @Nullable DummyCanvasData dummyCanvasData, Player player) {
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(player.level());

        if (dummyCanvasData != null) {
            CanvasData combinedCanvasData = CanvasData.BUILDER.createWrap(
                dummyCanvasData.getResolution(),
                dummyCanvasData.getWidth(),
                dummyCanvasData.getHeight(),
                dummyCanvasData.getColorData()
            );

            final int newId = canvasTracker.getFreeCanvasId();
            final String newCode = CanvasData.getCanvasCode(newId);

            canvasTracker.registerCanvasData(newCode, combinedCanvasData);
            CanvasItem.storeCanvasData(canvasItemStack, newCode, combinedCanvasData);
        } else {
            CanvasItem.setBlockSize(canvasItemStack, canvasGridRectangle.width, canvasGridRectangle.height);
        }
    }

    public static @Nullable CanvasGridRectangle getCraftingContainerCanvasRectangle(CraftingContainer craftingContainer) {
        Tuple<Integer, Integer> min = null;
        Tuple<Integer, Integer> max = null;

        int[] canvasBlockSize = null;

        for (int y = 0; y < craftingContainer.getHeight(); y++) {
            for (int x = 0; x < craftingContainer.getWidth(); x++) {
                ItemStack itemStack = craftingContainer.getItem(y * craftingContainer.getWidth() + x);

                if (itemStack != ItemStack.EMPTY) {
                    if (!craftingContainer.getItem(y * craftingContainer.getWidth() + x).is(ZetterItems.CANVAS.get())) {
                        // We only expect canvases
                        return null;
                    }

                    if (canvasBlockSize == null) {
                        canvasBlockSize = CanvasItem.getBlockSize(itemStack);
                    } else if (!Arrays.equals(canvasBlockSize, CanvasItem.getBlockSize(itemStack))) {
                        // We expect canvases to have the same resolution
                        return null;
                    }

                    if (min == null) {
                        min = new Tuple<>(x, y);
                    }

                    if (max == null) {
                        max = new Tuple<>(x, y);
                        continue;
                    }

                    if (max.getA() < x) {
                        max = new Tuple<>(x, max.getB());
                    }
                    if (max.getB() < y) {
                        max = new Tuple<>(max.getA(), y);
                    }
                }
            }
        }

        if (canvasBlockSize == null) {
            return null;
        }

        // Verify there are no empty slots in the rectangle
        for (int y = 0; y < craftingContainer.getHeight(); y++) {
            for (int x = 0; x < craftingContainer.getWidth(); x++) {
                ItemStack currentStack = craftingContainer.getItem(y * craftingContainer.getWidth() + x);

                if (currentStack.isEmpty()) {
                    if (x >= min.getA() && x <= max.getA()) {
                        if (y >= min.getB() && (y <= max.getB())) {
                            return null;
                        }
                    }
                } else if (currentStack.getItem() == ZetterItems.CANVAS.get()) {
                    if ((x < min.getA() || x > max.getA()) || (y < min.getB() || y > max.getB())) {
                        return null;
                    }
                }
            }
        }

        int width = max.getA() + 1 - min.getA();
        int height = max.getB() + 1 - min.getB();

        return new CanvasGridRectangle(min.getA(), min.getB(), width, height, canvasBlockSize);
    }

    public static String generateCombinedCanvasCode(CanvasGridRectangle canvasGridRectangle) {
        return "combined_" + UUID.randomUUID();
    }

    public static class CanvasGridRectangle {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final int[] canvasBlockSize;

        CanvasGridRectangle(int x, int y, int width, int height, int[] canvasBlockSize) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.canvasBlockSize = canvasBlockSize;
        }
    }
}
