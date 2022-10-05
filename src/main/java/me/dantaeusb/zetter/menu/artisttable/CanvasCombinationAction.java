package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.nio.ByteBuffer;

/**
 * Canvas combination is a helper structure that
 * checks and verifies data from multiple
 * canvases located on the artist table, used
 * to validate the shape of the future painting
 * and prepare preview for display if the canvas
 * combination is valid.
 *
 * It has a behavior of recipe as well, but as
 * we're having multi-slot results in split mode,
 * we're not using recipe model
 */
public class CanvasCombinationAction extends AbstractCanvasAction {
    public static final int[][] paintingShapes = new int[][]{
            {1, 1},
            {1, 2},
            {1, 3},
            {2, 1},
            {2, 2},
            {2, 3},
            {2, 4},
            {3, 1},
            {3, 2},
            {3, 3},
            {3, 4},
            {4, 2},
            {4, 3},
            {4, 4}
    };

    public Rectangle rectangle;

    public CanvasCombinationAction(ArtistTableMenu menu, Level level) {
        super(menu, level);

        this.update();
    }

    public void update() {
        ItemStackHandler combinationContainer = menu.getCombinationContainer();

        Tuple<Integer, Integer> min = null;
        Tuple<Integer, Integer> max = null;

        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                if (combinationContainer.getStackInSlot(y * 4 + x) != ItemStack.EMPTY) {
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
            this.state = State.INVALID;
            this.rectangle = CanvasCombinationAction.getZeroRect();
            this.canvasData = null;
            return;
        }

        boolean canvasesReady = true;

        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                ItemStack currentStack = combinationContainer.getStackInSlot(y * 4 + x);

                if (currentStack == ItemStack.EMPTY) {
                    if (x >= min.getA() && x <= max.getA()) {
                        if (y >= min.getB() && (y <= max.getB())) {
                            this.state = State.INVALID;
                            this.rectangle = CanvasCombinationAction.getZeroRect();
                            this.canvasData = null;
                            return;
                        }
                    }
                } else if (currentStack.getItem() == ZetterItems.CANVAS.get()) {
                    if ((x < min.getA() || x > max.getA()) || (y < min.getB() || y > max.getB())) {
                        this.state = State.INVALID;
                        this.rectangle = CanvasCombinationAction.getZeroRect();
                        this.canvasData = null;
                        return;
                    }

                    if (CanvasItem.getCanvasData(currentStack, this.level) == null) {
                        /**
                         * @todo: [HIGH] Move request out of here, request with data load attempts but avoid loading unavailable canvases
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
            this.state = State.NOT_LOADED;
            this.rectangle = CanvasCombinationAction.getZeroRect();
            this.canvasData = null;
            return;
        }

        Rectangle rectangle = CanvasCombinationAction.getRect(min, max);

        boolean shapeAvailable = false;
        for (int[] shape: CanvasCombinationAction.paintingShapes) {
            if (rectangle.width == shape[0] && rectangle.height == shape[1]) {
                shapeAvailable = true;
                break;
            }
        }

        if (!shapeAvailable) {
            this.state = State.INVALID;
            this.rectangle = CanvasCombinationAction.getZeroRect();
            this.canvasData = null;
            return;
        }

        this.state = State.READY;
        this.rectangle = rectangle;
        this.canvasData = CanvasCombinationAction.createCanvasData(combinationContainer, rectangle, this.level);
    }

    public static DummyCanvasData createCanvasData(ItemStackHandler artistTableContainer, Rectangle rectangle, Level world) {
        final int pixelWidth = rectangle.width * Helper.getResolution().getNumeric();
        final int pixelHeight = rectangle.height * Helper.getResolution().getNumeric();

        ByteBuffer color = ByteBuffer.allocate(pixelWidth * pixelHeight * 4);

        for (int slotY = rectangle.y; slotY < rectangle.y + rectangle.height; slotY++) {
            for (int slotX = rectangle.x; slotX < rectangle.x + rectangle.width; slotX++) {
                ItemStack canvasStack = artistTableContainer.getStackInSlot(slotY * 4 + slotX);

                CanvasData smallCanvasData = CanvasItem.getCanvasData(canvasStack, world);

                int relativeX = slotX - rectangle.x;
                int relativeY = slotY - rectangle.y;


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
                    // Use canvas color as background if canvas data is not initialized
                    // @todo: [LOW] Maybe there's a better way to do that
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

        DummyCanvasData combinedCanvasData = DummyCanvasData.createWrap(
            Helper.getResolution(),
            pixelWidth,
            pixelHeight,
            color.array()
        );

        if (world.isClientSide()) {
            Helper.getWorldCanvasTracker(world).registerCanvasData(Helper.COMBINED_CANVAS_CODE, combinedCanvasData);
        } else {
            // @todo: drop texture

        }

        return combinedCanvasData;
    }

    @Override
    public void onChangedCombination(ItemStackHandler container) {
        this.update();

        ItemStack combinedStack = this.menu.getCombinedHandler().getStackInSlot(0);

        // @todo: Can combine?
        if (this.isReady()) {
            if (combinedStack.isEmpty()) {
                combinedStack = new ItemStack(ZetterItems.CANVAS.get());
            }
        } else {
            combinedStack = ItemStack.EMPTY;
        }

        this.menu.getCombinedHandler().setStackInSlot(0, combinedStack);
    }

    @Override
    public void onTakeCombined(Player player, ItemStack stack) {
        if (this.canvasData == null) {
            Zetter.LOG.error("Cannot find combined canvas data");
            return;
        }

        if (player.getLevel().isClientSide()) {
            // @todo: remove texture from client too
            return;
        }

        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getWorldCanvasTracker(player.getLevel());
        CanvasData combinedCanvasData = CanvasData.createWrap(
                this.canvasData.getResolution(),
                this.canvasData.getWidth(),
                this.canvasData.getHeight(),
                this.canvasData.getColorData()
        );

        final int newId = canvasTracker.getFreeCanvasId();
        final String newCode = CanvasData.getCanvasCode(newId);

        canvasTracker.registerCanvasData(newCode, combinedCanvasData);
        CanvasItem.storeCanvasData(stack, newCode, combinedCanvasData);

        for (int i = 0; i < this.menu.getCombinationContainer().getSlots(); i++) {
            ItemStack combinationStack = this.menu.getCombinationContainer().getStackInSlot(i);

            if (combinationStack.isEmpty()) {
                continue;
            }

            // Cleanup IDs and grid
            int canvasId = CanvasItem.getCanvasId(combinationStack);
            // @todo: remove texture from client too
            canvasTracker.clearCanvasId(canvasId);
            this.menu.getCombinationContainer().setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public boolean isReady() {
        return this.state == State.READY;
    }

    public static Rectangle getRect(Tuple<Integer, Integer> min, Tuple<Integer, Integer> max) {
        int width = max.getA() + 1 - min.getA();
        int height = max.getB() + 1 - min.getB();

        return new Rectangle(min.getA(), min.getB(), width, height);
    }

    public static Rectangle getZeroRect() {
        return new Rectangle(0, 0, 0, 0);
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
