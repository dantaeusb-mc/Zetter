package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasClientTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
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

    private boolean hasColorData = false;

    public CanvasCombinationAction(ArtistTableMenu menu, Level level) {
        super(menu, level);

        this.updateCanvasData(menu.getCombinationContainer());
    }

    /**
     * When new item placed or it's canvas data
     * updated from server, update the preview
     *
     * @param combinationContainer
     */
    public void updateCanvasData(ItemStackHandler combinationContainer) {
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

        if (min == null) {
            this.state = State.EMPTY;
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

                    if (
                        this.level.isClientSide()
                        && CanvasItem.getCanvasCode(currentStack) != null
                        && CanvasItem.getCanvasData(currentStack, this.level) == null
                    ) {
                        CanvasRenderer.getInstance().queueCanvasTextureUpdate(
                            CanvasItem.getCanvasCode(currentStack)
                        );

                        canvasesReady = false;
                    }
                }
            }
        }

        Rectangle rectangle = CanvasCombinationAction.getRect(min, max);

        if (rectangle.height == 1 && rectangle.width == 1) {
            this.state = State.EMPTY;
            this.rectangle = CanvasCombinationAction.getZeroRect();
            this.canvasData = null;
            return;
        }

        if (!canvasesReady) {
            this.state = State.NOT_LOADED;
            this.rectangle = CanvasCombinationAction.getZeroRect();
            this.canvasData = null;
            return;
        }

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
        this.canvasData = this.createCanvasData(combinationContainer, rectangle, this.level);
    }

    private DummyCanvasData createCanvasData(ItemStackHandler artistTableContainer, Rectangle rectangle, Level world) {
        final int pixelWidth = rectangle.width * Helper.getResolution().getNumeric();
        final int pixelHeight = rectangle.height * Helper.getResolution().getNumeric();
        this.hasColorData = false;

        for (int i = 0; i < artistTableContainer.getSlots(); i++) {
            if (CanvasItem.getCanvasData(artistTableContainer.getStackInSlot(i), world) != null) {
                this.hasColorData = true;
                break;
            }
        }

        // Return default canvas instead
        if (!this.hasColorData) {
            CanvasData defaultCanvasData = CanvasData.DEFAULTS.get(CanvasData.getDefaultCanvasCode(rectangle.width, rectangle.height));

            DummyCanvasData combinedCanvasData = DummyCanvasData.BUILDER.createWrap(
                defaultCanvasData.getResolution(),
                defaultCanvasData.getWidth(),
                defaultCanvasData.getHeight(),
                defaultCanvasData.getColorData()
            );

            if (world.isClientSide()) {
                Helper.getLevelCanvasTracker(world).registerCanvasData(Helper.COMBINED_CANVAS_CODE, combinedCanvasData);
            }

            return combinedCanvasData;
        }

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

        if (world.isClientSide()) {
            Helper.getLevelCanvasTracker(world).registerCanvasData(Helper.COMBINED_CANVAS_CODE, combinedCanvasData);
        }

        return combinedCanvasData;
    }

    @Override
    public void onChangedCombination(ItemStackHandler container) {
        this.updateCanvasData(container);

        ItemStack combinedStack = this.menu.getCombinedHandler().getStackInSlot(0);

        // @todo: [MED] Can combine?
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
        if (this.canvasData == null || !this.isReady()) {
            Zetter.LOG.error("Cannot find combined canvas data");
            return;
        }

        if (!player.getLevel().isClientSide()) {
            CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(player.getLevel());

            if (this.hasColorData) {
                CanvasData combinedCanvasData = CanvasData.BUILDER.createWrap(
                    this.canvasData.getResolution(),
                    this.canvasData.getWidth(),
                    this.canvasData.getHeight(),
                    this.canvasData.getColorData()
                );

                final int newId = canvasTracker.getFreeCanvasId();
                final String newCode = CanvasData.getCanvasCode(newId);

                canvasTracker.registerCanvasData(newCode, combinedCanvasData);
                CanvasItem.storeCanvasData(stack, newCode, combinedCanvasData);
            } else {
                CanvasItem.setBlockSize(stack, this.rectangle.width, this.rectangle.height);
            }

            for (int i = 0; i < this.menu.getCombinationContainer().getSlots(); i++) {
                ItemStack combinationStack = this.menu.getCombinationContainer().getStackInSlot(i);

                if (combinationStack.isEmpty()) {
                    continue;
                }

                String canvasCode = CanvasItem.getCanvasCode(combinationStack);

                // Cleanup IDs and grid
                if (canvasCode != null) {
                    canvasTracker.unregisterCanvasData(canvasCode);
                }

                this.menu.getCombinationContainer().setStackInSlot(i, ItemStack.EMPTY);
            }
        } else {
            CanvasClientTracker canvasTracker = (CanvasClientTracker) Helper.getLevelCanvasTracker(player.getLevel());

            for (int i = 0; i < this.menu.getCombinationContainer().getSlots(); i++) {
                // First we are removing item to avoid loading it's canvas on update
                // otherwise, when server will push container update events with
                // CanvasRenderer.getInstance().queueCanvasTextureUpdate(...)
                // One by one by every removed canvas and cause client to load
                // textures for removed canvases again
                ItemStack combinationStack = this.menu.getCombinationContainer().extractItem(i, 64, false);

                if (combinationStack.isEmpty()) {
                    continue;
                }

                String canvasCode = CanvasItem.getCanvasCode(combinationStack);

                // Cleanup IDs and grid
                if (canvasCode != null) {
                    canvasTracker.unregisterCanvasData(canvasCode);
                }
            }
        }
    }

    /**
     * Call changed container to update output slot
     * @param canvasCode
     * @param canvasData
     * @param timestamp
     */
    @Override
    public void handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) {
        this.onChangedCombination(this.menu.getCombinationContainer());
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
