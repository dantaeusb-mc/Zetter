package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import java.nio.ByteBuffer;

public class CanvasSplitAction extends AbstractCanvasAction {
    /**
     * As we display a preview of an action in the
     * "reverse" crafting grid, we need to understand
     * when there are preview canvases and when
     * there are real canvases, which are crafted
     * from either painted or blank canvases.
     * We cannot rely on the fact that canvas is painted,
     * as blank canvases can be combined and split too.
     */
    private final boolean[][] realCanvases = new boolean[][] {
        {
            false, false, false, false
        },
        {
            false, false, false, false
        },
        {
            false, false, false, false
        },
        {
            false, false, false, false
        }
    };

    public CanvasSplitAction(ArtistTableMenu artistTableMenu, World level) {
        super(artistTableMenu, level);
    }

    /**
     * Check if can put anything in
     * "combined" slot
     *
     * @param stack
     * @return
     * @todo: [HIGH] Don't allow if grid is not empty
     */
    public boolean mayPlaceCombined(ItemStack stack) {
        if (stack.getItem() == ZetterItems.CANVAS.get()) {
            if (!CanvasItem.isCompound(stack)) {
                return false;
            }

            return this.noRealCanvases() && this.menu.isSplitGridEmpty();
        }

        return false;
    }

    public boolean noRealCanvases() {
        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                if (this.realCanvases[y][x]) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isReady() {
        return this.state == State.READY;
    }

    /**
     * When compound canvas is placed we
     * should populate the grid with preview
     * of the result, empty non-compound canvases
     * without data
     *
     * @param combinedHandler
     */
    @Override
    public void onChangedCombined(ItemStackHandler combinedHandler) {
        if (this.isInTransaction()) {
            return;
        }

        this.updateCanvasData(combinedHandler);

        if (this.menu.getLevel().isClientSide()) {
            return;
        }

        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        if (this.isReady()) {
            int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
            assert compoundCanvasSize != null;

            final int compoundCanvasWidth = compoundCanvasSize[0];
            final int compoundCanvasHeight = compoundCanvasSize[1];

            // Create canvas items for the slots according to split canvas size
            for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
                for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                    int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;

                    if (this.realCanvases[y][x]) {
                        continue;
                    }

                    if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                        this.menu.getSplitHandler().setStackInSlot(slotNumber, new ItemStack(ZetterItems.CANVAS.get()));
                    } else {
                        // Just make sure we will not ever remove painted canvas
                        if (this.menu.getSplitHandler().getStackInSlot(slotNumber).getItem() != ZetterItems.CANVAS.get()) {
                            Zetter.LOG.error("Found an canvas in split slot that is not supposed to be there when updating combined slot!");
                        } else {
                            this.menu.getSplitHandler().setStackInSlot(slotNumber, ItemStack.EMPTY);
                        }
                    }
                }
            }
        } else {
            for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
                for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                    int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;

                    if (this.realCanvases[y][x]) {
                        continue;
                    }

                    this.menu.getSplitHandler().setStackInSlot(slotNumber, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * When new item placed or it's canvas data
     * updated from server, update the preview
     *
     * @param combinedHandler
     */
    public void updateCanvasData(ItemStackHandler combinedHandler) {
        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        // No item - update preview and state
        if (combinedStack.isEmpty() || combinedStack.getItem() != ZetterItems.CANVAS.get() || !CanvasItem.isCompound(combinedStack)) {
            this.canvasData = null;
            this.state = State.EMPTY;

            return;
        }

        String combinedStackCanvasCode = CanvasItem.getCanvasCode(combinedStack);

        if (combinedStackCanvasCode == null) {
            int[] size = CanvasItem.getBlockSize(combinedStack);

            assert size != null && size.length == 2;

            final int resolutionPixels = Helper.getResolution().getNumeric();
            byte[] color = new byte[
                size[0] * resolutionPixels *
                size[1] * resolutionPixels *
                4
            ];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for (int x = 0; x < size[0] * resolutionPixels * size[1] * resolutionPixels; x++) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            this.canvasData = ZetterCanvasTypes.DUMMY.get().createWrap(
                Helper.COMBINED_CANVAS_CODE,
                Helper.getResolution(),
                size[0] * Helper.getResolution().getNumeric(),
                size[1] * Helper.getResolution().getNumeric(),
                color
            );

            if (this.level.isClientSide()) {
                Helper.getLevelCanvasTracker(this.level).registerCanvasData(Helper.COMBINED_CANVAS_CODE, this.canvasData);
            }

            this.state = State.READY;
            return;
        }

        CanvasData combinedStackCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

        if (combinedStackCanvasData != null) {
            this.canvasData = ZetterCanvasTypes.DUMMY.get().createWrap(
                Helper.COMBINED_CANVAS_CODE,
                combinedStackCanvasData.getResolution(),
                combinedStackCanvasData.getWidth(),
                combinedStackCanvasData.getHeight(),
                combinedStackCanvasData.getColorData()
            );

            if (this.level.isClientSide()) {
                Helper.getLevelCanvasTracker(this.level).registerCanvasData(Helper.COMBINED_CANVAS_CODE, this.canvasData);
            }

            this.state = State.READY;
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(
                CanvasItem.getCanvasCode(combinedStack)
            );

            this.state = State.NOT_LOADED;
        }
    }

    /**
     * Remove the combined canvas, set the data for the
     * partial canvases on the grid
     *
     * This is fucking monstrosity. I should not be that hard,
     * yet I have no idea how to make it better.
     *
     * @param player
     * @param takenStack
     */
    @Override
    public void onTakeSplit(PlayerEntity player, ItemStack takenStack) {
        ItemStack combinedStack = this.menu.getCombinedHandler().getStackInSlot(0);

        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;
                ItemStack splitStack = this.menu.getSplitHandler().getStackInSlot(slotNumber);

                if (this.realCanvases[y][x]) {
                    this.realCanvases[y][x] = !splitStack.isEmpty();
                }
            }
        }

        if (combinedStack.getItem() != ZetterItems.CANVAS.get() || !CanvasItem.isCompound(combinedStack)) {
            return;
        }

        if (!this.noRealCanvases()) {
            Zetter.LOG.error("Cannot take split canvas when there are still real canvases on the grid");
            return;
        }

        this.startTransaction(player);

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        final int numericResolution = CanvasItem.getResolution(combinedStack);

        // Get data from split canvas
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(player.level);

        if (this.level.isClientSide()) {
            for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
                for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                    int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;
                    ItemStack splitStack = this.menu.getSplitHandler().getStackInSlot(slotNumber);

                    if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                        this.realCanvases[y][x] = !splitStack.isEmpty();
                    }
                }
            }
        } else {
            final CanvasData combinedCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

            // Don't need that data for client, it'll request if needed
            if (combinedCanvasData == null) {
                Zetter.LOG.error("No canvas data found for item in combined slot");
                return;
            }

            int missingX = 0;
            int missingY = 0;

            for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
                for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                    int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;
                    ItemStack splitStack = this.menu.getSplitHandler().getStackInSlot(slotNumber);

                    if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                        if (splitStack.isEmpty()) {
                            missingX = x;
                            missingY = y;
                            continue;
                        }

                        this.realCanvases[y][x] = true;

                        String canvasCode = CanvasData.getCanvasCode(((CanvasServerTracker) canvasTracker).getFreeCanvasId());
                        CanvasData itemData = CanvasData.BUILDER.createWrap(
                            canvasCode,
                            combinedCanvasData.getResolution(),
                            numericResolution,
                            numericResolution,
                            getPartialColorData(
                                combinedCanvasData.getColorData(),
                                numericResolution,
                                x,
                                y,
                                compoundCanvasWidth,
                                compoundCanvasHeight
                            )
                        );
                        canvasTracker.registerCanvasData(canvasCode, itemData);

                        CanvasItem.storeCanvasData(splitStack, canvasCode, itemData);
                    }
                }
            }

            // Set data for the picked item
            String canvasCode = CanvasData.getCanvasCode(((CanvasServerTracker) canvasTracker).getFreeCanvasId());
            CanvasData itemData = CanvasData.BUILDER.createWrap(
                canvasCode,
                combinedCanvasData.getResolution(),
                numericResolution,
                numericResolution,
                getPartialColorData(
                    combinedCanvasData.getColorData(),
                    numericResolution,
                    missingX,
                    missingY,
                    compoundCanvasWidth,
                    compoundCanvasHeight
                )
            );

            canvasTracker.registerCanvasData(canvasCode, itemData);

            CanvasItem.storeCanvasData(takenStack, canvasCode, itemData);
        }

        String canvasCode = CanvasItem.getCanvasCode(combinedStack);

        // Cleanup ID
        if (canvasCode != null) {
            canvasTracker.unregisterCanvasData(canvasCode);
        }

        // Remove split canvas item without triggering update
        this.endTransaction(player);
    }

    @Override
    public void endTransaction(PlayerEntity player) {
        this.menu.getCombinedHandler().setStackInSlot(0, ItemStack.EMPTY);
        this.updateCanvasData(this.menu.getCombinedHandler());

        super.endTransaction(player);
    }

    /**
     * Call changed container to update output slot
     *
     * @param canvasCode
     * @param canvasData
     * @param timestamp
     */
    @Override
    public void handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) {
        this.onChangedCombined(this.menu.getCombinedHandler());
    }

    private static byte[] getPartialColorData(byte[] colorData, int resolution, int blockX, int blockY, int blockWidth, int blockHeight) {
        byte[] destinationColor = new byte[resolution * resolution * 4];
        ByteBuffer colorBuffer = ByteBuffer.wrap(colorData);

        final int offset = ((blockWidth * resolution * blockY * resolution) + blockX * resolution) * 4;

        for (int y = 0; y < resolution; y++) {
            final int lastPos = colorBuffer.position();
            colorBuffer.position(offset + (blockWidth * resolution * y) * 4);
            colorBuffer.get(destinationColor, y * resolution * 4, resolution * 4);
            colorBuffer.position(lastPos);
        }

        return destinationColor;
    }
}
