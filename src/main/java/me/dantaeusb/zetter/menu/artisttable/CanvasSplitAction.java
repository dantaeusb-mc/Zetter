package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasDefaultTracker;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.nio.ByteBuffer;

public class CanvasSplitAction extends AbstractCanvasAction {
    public CanvasSplitAction(ArtistTableMenu artistTableMenu, Level level) {
        super(artistTableMenu, level);
    }

    /**
     * Check if can put anything in
     * "combined" slot
     *
     * @todo: Don't allow if grid is not empty
     * @param stack
     * @return
     */
    public boolean mayPlaceCombined(ItemStack stack) {
        if (stack.is(ZetterItems.CANVAS.get())) {
            if (CanvasItem.isEmpty(stack) || !CanvasItem.isCompound(stack)) {
                return false;
            }

            return this.menu.isSplitGridEmpty();
        }

        return false;
    }

    /**
     * When compound canvas is placed we
     * should populate the grid with preview
     * of the result, empty non-compound canvases
     * without data
     *
     * When canvas removed, if we see canvases
     * without data (empty), that means we had
     * preview here in place before, so it's safe
     * to remove all canvases from the grid
     *
     * @param combinedHandler
     */
    @Override
    public void onChangedCombined(ItemStackHandler combinedHandler) {
        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        // No item - clean the contents of split grid
        if (combinedStack.isEmpty() || !combinedStack.is(ZetterItems.CANVAS.get()) || !CanvasItem.isCompound(combinedStack)) {
            for (int i = 0; i < this.menu.getSplitHandler().getSlots(); i++) {
                ItemStack stackInSlot = this.menu.getSplitHandler().getStackInSlot(i);

                if (!stackInSlot.isEmpty() && CanvasItem.isEmpty(stackInSlot)) {
                    this.menu.getSplitHandler().setStackInSlot(i, ItemStack.EMPTY);
                }
            }

            this.canvasData = null;

            return;
        }

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        // Create canvas items for the slots according to split canvas size
        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;

                if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                    this.menu.getSplitHandler().setStackInSlot(slotNumber, new ItemStack(ZetterItems.CANVAS.get()));
                } else {
                    if (
                            !this.menu.getSplitHandler().getStackInSlot(slotNumber).is(ZetterItems.CANVAS.get()) ||
                                    !CanvasItem.isEmpty(this.menu.getSplitHandler().getStackInSlot(slotNumber))
                    ) {
                        this.menu.getSplitHandler().setStackInSlot(slotNumber, ItemStack.EMPTY);
                    }
                }
            }
        }

        this.updateCanvasData(combinedHandler);
    }

    public void updateCanvasData(ItemStackHandler combinedHandler) {
        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        CanvasData combinedStackCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

        if (combinedStackCanvasData != null) {
            this.canvasData = ZetterCanvasTypes.DUMMY.get().createWrap(
                    combinedStackCanvasData.getResolution(),
                    combinedStackCanvasData.getWidth(),
                    combinedStackCanvasData.getHeight(),
                    combinedStackCanvasData.getColorData()
            );

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
     * @param player
     * @param takenStack
     */
    @Override
    public void onTakeSplit(Player player, ItemStack takenStack) {
        if (this.level.isClientSide()) {
            // Nothing to do on client
            return;
        }

        ItemStack combinedStack = this.menu.getCombinedHandler().getStackInSlot(0);

        if (combinedStack.isEmpty() || !combinedStack.is(ZetterItems.CANVAS.get()) || !CanvasItem.isCompound(combinedStack)) {
            return;
        }

        // Get data from split canvas
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(player.getLevel());
        final CanvasData combinedCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

        // Don't need that data for client, it'll request if needed
        if (combinedCanvasData == null) {
            Zetter.LOG.error("No canvas data found for item in combined slot");
            return;
        }

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        final int numericResolution = combinedCanvasData.getResolution().getNumeric();

        int missingX = 0;
        int missingY = 0;

        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;
                ItemStack splitStack = this.menu.getSplitHandler().getStackInSlot(slotNumber);

                if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                    if (splitStack.isEmpty()/* || !CanvasItem.isEmpty(gridStack, this.level)*/) {
                        missingX = x;
                        missingY = y;
                        continue;
                    }

                    CanvasData itemData = CanvasData.createWrap(
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

                    String canvasCode = CanvasData.getCanvasCode(((CanvasServerTracker) canvasTracker).getFreeCanvasId());
                    canvasTracker.registerCanvasData(canvasCode, itemData);

                    CanvasItem.storeCanvasData(splitStack, canvasCode, itemData);
                }
            }
        }

        // Set data for the picked item
        CanvasData itemData = CanvasData.createWrap(
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

        String canvasCode = CanvasData.getCanvasCode(((CanvasServerTracker) canvasTracker).getFreeCanvasId());
        canvasTracker.registerCanvasData(canvasCode, itemData);

        CanvasItem.storeCanvasData(takenStack, canvasCode, itemData);

        // Remove split canvas item
        this.menu.getCombinedHandler().setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public void handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) {
        this.updateCanvasData(this.menu.getSplitHandler());
    }

    private static byte[] getPartialColorData(byte[] colorData, int resolution, int blockX, int blockY, int blockWidth, int blockHeight) {
        byte[] destinationColor = new byte[resolution * resolution * 4];
        ByteBuffer colorBuffer = ByteBuffer.wrap(colorData);

        final int offset = ((blockWidth * resolution * blockY * resolution) + blockX * resolution) * 4;

        for (int y = 0; y < resolution; y++) {
            colorBuffer.get(offset + (blockWidth * resolution * y) * 4, destinationColor, y * resolution * 4, resolution * 4);
        }

        return destinationColor;
    }
}
