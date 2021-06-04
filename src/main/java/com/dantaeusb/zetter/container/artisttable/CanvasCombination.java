package com.dantaeusb.zetter.container.artisttable;

import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableCanvasStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class CanvasCombination {
    public static final int[][] paintingShapes = new int[][]{
            {1, 1},
            {1, 2},
            {1, 3},
            {2, 1},
            {2, 2},
            {2, 3},
            {3, 1},
            {3, 2},
            {3, 3},
            {4, 2},
            {4, 3}
    };

    public final State valid;
    public final Rectangle rectangle;

    @Nullable
    public final DummyCanvasData canvasData;

    public CanvasCombination(ArtistTableCanvasStorage canvasStorage, World world) {
        Tuple<Integer, Integer> min = null;
        Tuple<Integer, Integer> max = null;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                if (canvasStorage.getStackInSlot(y * 4 + x) != ItemStack.EMPTY) {
                    if (min == null) {
                        min = new Tuple<>(x ,y);
                    }

                    if (max == null) {
                        max = new Tuple<>(x ,y);
                        continue;
                    }

                    if (max.getA() < x) {
                        max = new Tuple<>(x, max.getB());
                    } if (max.getB() < y) {
                        max = new Tuple<>(max.getA(), y);
                    }
                }
            }
        }

        if (min == null || max == null) {
            this.valid = State.INVALID_SHAPE;
            this.rectangle = CanvasCombination.getZeroRect();
            this.canvasData = null;
            return;
        }

        boolean canvasesReady = true;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                ItemStack currentStack = canvasStorage.getStackInSlot(y * 4 + x);

                if (currentStack == ItemStack.EMPTY) {
                    if (x >= min.getA() && x <= max.getA()) {
                        if (y >= min.getB() && (y <= max.getB())) {
                            this.valid = State.INVALID_SHAPE;
                            this.rectangle = CanvasCombination.getZeroRect();
                            this.canvasData = null;
                            return;
                        }
                    }
                } else if (currentStack.getItem() == ModItems.CANVAS_ITEM) {
                    if ((x < min.getA() || x > max.getA()) || (y < min.getB() || y > max.getB())) {
                        this.valid = State.INVALID_SHAPE;
                        this.rectangle = CanvasCombination.getZeroRect();
                        this.canvasData = null;
                        return;
                    }

                    if (CanvasItem.getCanvasData(currentStack, world) == null) {
                        /**
                         * @todo: move request out of here, request with data load attempts but avoid loading unavailable canvases
                         */
                        CanvasRenderer.getInstance().queueCanvasTextureUpdate(
                                AbstractCanvasData.Type.CANVAS,
                                CanvasItem.getCanvasCode(currentStack)
                        );

                        canvasesReady = false;
                    }
                }
            }
        }

        if (!canvasesReady) {
            this.valid = State.NOT_LOADED;
            this.rectangle = CanvasCombination.getZeroRect();
            this.canvasData = null;
            return;
        }

        Rectangle rectangle = CanvasCombination.getRect(min, max);

        boolean shapeAvailable = false;
        for (int[] shape: CanvasCombination.paintingShapes) {
            if (rectangle.width == shape[0] && rectangle.height == shape[1]) {
                shapeAvailable = true;
            }
        }

        if (!shapeAvailable) {
            this.valid = State.INVALID_SHAPE;
            this.rectangle = CanvasCombination.getZeroRect();
            this.canvasData = null;
            return;
        }

        this.valid = State.READY;
        this.rectangle = rectangle;
        this.canvasData = CanvasCombination.createCanvasData(canvasStorage, rectangle, world);
    }

    public static DummyCanvasData createCanvasData(ArtistTableCanvasStorage canvasStorage, Rectangle rectangle, World world) {
        final int pixelWidth = rectangle.width * Helper.CANVAS_TEXTURE_RESOLUTION;
        final int pixelHeight = rectangle.height * Helper.CANVAS_TEXTURE_RESOLUTION;

        ByteBuffer color = ByteBuffer.allocate(pixelWidth * pixelHeight * 4);

        for (int slotY = rectangle.y; slotY < rectangle.y + rectangle.height; slotY++) {
            for (int slotX = rectangle.x; slotX < rectangle.x + rectangle.width; slotX++) {
                ItemStack canvasStack = canvasStorage.getStackInSlot(slotY * 4 + slotX);

                CanvasData smallCanvasData = CanvasItem.getCanvasData(canvasStack, world);

                int relativeX = slotX - rectangle.x;
                int relativeY = slotY - rectangle.y;

                for (int smallY = 0; smallY < smallCanvasData.getHeight(); smallY++) {
                    for (int smallX = 0; smallX < smallCanvasData.getWidth(); smallX++) {
                        final int bigX = relativeX * Helper.CANVAS_TEXTURE_RESOLUTION + smallX;
                        final int bigY = relativeY * Helper.CANVAS_TEXTURE_RESOLUTION + smallY;

                        final int colorIndex = (bigY * pixelWidth + bigX) * 4;

                        color.putInt(colorIndex, smallCanvasData.getColorAt(smallX, smallY));
                    }
                }
            }
        }

        DummyCanvasData combinedCanvasData = Helper.getCombinedCanvas();

        combinedCanvasData.initData(
            pixelWidth,
            pixelHeight,
            color.array()
        );

        if (world.isRemote()) {
            Helper.getWorldCanvasTracker(world).registerCanvasData(combinedCanvasData);
        }

        return combinedCanvasData;
    }

    public static Rectangle getRect(Tuple<Integer, Integer> min, Tuple<Integer, Integer> max) {
        int width = max.getA() + 1 - min.getA();
        int height = max.getB() + 1 - min.getB();

        return new Rectangle(min.getA(), min.getB(), width, height);
    }

    public static Rectangle getZeroRect() {
        return new Rectangle(0, 0, 0, 0);
    }

    public enum State {
        INVALID_SHAPE,
        NOT_LOADED,
        READY
    }

    private static class Rectangle {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
