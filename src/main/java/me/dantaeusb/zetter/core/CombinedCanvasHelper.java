package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Helper to handle events and combination of the combined canvas when stitching.
 */
public class CombinedCanvasHelper {
    public static @Nullable DummyCanvasData createCanvasData(CraftingContainer craftingInventory, Level world) {
        final CanvasGridRectangle canvasGridRectangle = getCraftingContainerCanvasRectangle(craftingInventory);

        if (canvasGridRectangle == null) {
            Zetter.LOG.warn("Failed to create combined canvas data: No valid canvas rectangle found in crafting inventory.");
            Zetter.LOG.debug("At this point, the crafting inventory should contain a valid rectangle of canvases.");
            return null;
        }

        final int pixelWidth = canvasGridRectangle.width * canvasGridRectangle.canvasBlockSize[0] * Helper.getResolution().getNumeric();
        final int pixelHeight = canvasGridRectangle.height * canvasGridRectangle.canvasBlockSize[1] * Helper.getResolution().getNumeric();
        boolean hasColorData = false;

        for (int i = 0; i < craftingInventory.getContainerSize(); i++) {
            if (CanvasItem.getCanvasData(craftingInventory.getItem(i), world) != null) {
                hasColorData = true;
                break;
            }
        }

        // Return default canvas instead
        if (!hasColorData) {
            final int resolutionPixels = Helper.getResolution().getNumeric();
            byte[] color = new byte[
                canvasGridRectangle.width * resolutionPixels *
                    canvasGridRectangle.height * resolutionPixels *
                    4
                ];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for (int x = 0; x < canvasGridRectangle.width * resolutionPixels * canvasGridRectangle.height * resolutionPixels; x++) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            return ZetterCanvasTypes.DUMMY.get().createWrap(
                Helper.getResolution(),
                canvasGridRectangle.width * resolutionPixels,
                canvasGridRectangle.height * resolutionPixels,
                color
            );
        }

        ByteBuffer color = ByteBuffer.allocate(pixelWidth * pixelHeight * 4);

        for (int slotY = canvasGridRectangle.y; slotY < canvasGridRectangle.y + canvasGridRectangle.height; slotY++) {
            for (int slotX = canvasGridRectangle.x; slotX < canvasGridRectangle.x + canvasGridRectangle.width; slotX++) {
                ItemStack canvasStack = craftingInventory.getItem(slotY * 4 + slotX);

                CanvasData smallCanvasData = CanvasItem.getCanvasData(canvasStack, world);

                int relativeX = slotX - canvasGridRectangle.x;
                int relativeY = slotY - canvasGridRectangle.y;

                if (smallCanvasData != null) {
                    for (int smallY = 0; smallY < smallCanvasData.getHeight(); smallY++) {
                        for (int smallX = 0; smallX < smallCanvasData.getWidth(); smallX++) {
                            final int bigX = relativeX * Helper.getResolution().getNumeric() + smallX;
                            final int bigY = relativeY * Helper.getResolution().getNumeric() + smallY;

                            final int colorIndex = (bigY * pixelWidth + bigX) * 4;

                            color.putInt(colorIndex, smallCanvasData.getColorAt(smallX, smallY));
                        }
                    }
                } else {
                    for (int smallY = 0; smallY < Helper.getResolution().getNumeric(); smallY++) {
                        for (int smallX = 0; smallX < Helper.getResolution().getNumeric(); smallX++) {
                            final int bigX = relativeX * Helper.getResolution().getNumeric() + smallX;
                            final int bigY = relativeY * Helper.getResolution().getNumeric() + smallY;

                            final int colorIndex = (bigY * pixelWidth + bigX) * 4;

                            color.putInt(colorIndex, Helper.CANVAS_COLOR);
                        }
                    }
                }
            }
        }

        DummyCanvasData combinedCanvasData = ZetterCanvasTypes.DUMMY.get().createWrap(
            Helper.getResolution(),
            pixelWidth,
            pixelHeight,
            color.array()
        );

        return combinedCanvasData;
    }

    public static @Nullable CanvasGridRectangle getCraftingContainerCanvasRectangle(CraftingContainer craftingInventory) {
        Tuple<Integer, Integer> min = null;
        Tuple<Integer, Integer> max = null;

        int[] canvasBlockSize = null;

        for (int y = 0; y < craftingInventory.getHeight(); y++) {
            for (int x = 0; x < craftingInventory.getWidth(); x++) {
                ItemStack itemStack = craftingInventory.getItem(y * 4 + x);

                if (itemStack != ItemStack.EMPTY) {
                    if (!craftingInventory.getItem(y * 4 + x).is(ZetterItems.CANVAS.get())) {
                        // We only expect canvases
                        return null;
                    }

                    if (canvasBlockSize == null) {
                        canvasBlockSize = CanvasItem.getBlockSize(itemStack);
                    } else if (Arrays.equals(canvasBlockSize, CanvasItem.getBlockSize(itemStack))) {
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
        for (int y = 0; y < craftingInventory.getHeight(); y++) {
            for (int x = 0; x < craftingInventory.getWidth(); x++) {
                ItemStack currentStack = craftingInventory.getItem(y * 4 + x);

                if (currentStack == ItemStack.EMPTY) {
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

        return  new CanvasGridRectangle(min.getA(), min.getB(), width, height, canvasBlockSize);
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
