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
    public CanvasSplitAction(ArtistTableMenu artistTableMenu, World level) {
        super(artistTableMenu, level);
    }

    /**
     * Check if can put anything in
     * "combined" slot
     *
     * @todo: [HIGH] Don't allow if grid is not empty
     * @param stack
     * @return
     */
    public boolean mayPlaceCombined(ItemStack stack) {
        if (stack.getItem() == ZetterItems.CANVAS.get()) {
            if (!CanvasItem.isCompound(stack)) {
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
     * @param combinedHandler
     */
    @Override
    public void onChangedCombined(ItemStackHandler combinedHandler) {
        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        // No item - clean the contents of split grid
        if (combinedStack.isEmpty() || combinedStack.getItem() != ZetterItems.CANVAS.get() || !CanvasItem.isCompound(combinedStack)) {
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
                    // Just make sure we will not ever remove painted canvas
                    if (
                        this.menu.getSplitHandler().getStackInSlot(slotNumber).getItem() != ZetterItems.CANVAS.get() ||
                        !CanvasItem.isEmpty(this.menu.getSplitHandler().getStackInSlot(slotNumber))
                    ) {
                        this.menu.getSplitHandler().setStackInSlot(slotNumber, ItemStack.EMPTY);
                    }
                }
            }
        }

        this.updateCanvasData(combinedHandler);
    }

    /**
     * When new item placed or it's canvas data
     * updated from server, update the preview
     *
     * @param combinedHandler
     */
    public void updateCanvasData(ItemStackHandler combinedHandler) {
        ItemStack combinedStack = combinedHandler.getStackInSlot(0);

        String combinedStackCanvasCode = CanvasItem.getCanvasCode(combinedStack);

        if (combinedStackCanvasCode == null) {
            int[] size = CanvasItem.getBlockSize(combinedStack);

            assert size != null && size.length == 2;
            CanvasData defaultCanvasData = CanvasData.DEFAULTS.get(CanvasData.getDefaultCanvasCode(size[0], size[1]));

            DummyCanvasData combinedCanvasData = DummyCanvasData.BUILDER.createWrap(
                Helper.COMBINED_CANVAS_CODE,
                defaultCanvasData.getResolution(),
                defaultCanvasData.getWidth(),
                defaultCanvasData.getHeight(),
                defaultCanvasData.getColorData()
            );

            if (this.level.isClientSide()) {
                Helper.getLevelCanvasTracker(this.level).registerCanvasData(Helper.COMBINED_CANVAS_CODE, combinedCanvasData);
            }

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
     * @param player
     * @param takenStack
     */
    @Override
    public void onTakeSplit(PlayerEntity player, ItemStack takenStack) {
        if (this.level.isClientSide()) {
            // Nothing to do on client
            return;
        }

        ItemStack combinedStack = this.menu.getCombinedHandler().getStackInSlot(0);

        if (combinedStack.getItem() != ZetterItems.CANVAS.get() || !CanvasItem.isCompound(combinedStack)) {
            return;
        }

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        final int numericResolution = CanvasItem.getResolution(combinedStack);

        int missingX = 0;
        int missingY = 0;

        // Canvas is empty, so split items are empty, too
        // There's no need to assign data
        if(CanvasItem.isEmpty(combinedStack)) {
            // Remove split canvas item
            this.menu.getCombinedHandler().setStackInSlot(0, ItemStack.EMPTY);

            return;
        }

        // Get data from split canvas
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(player.level);
        final CanvasData combinedCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

        // Don't need that data for client, it'll request if needed
        if (combinedCanvasData == null) {
            Zetter.LOG.error("No canvas data found for item in combined slot");
            return;
        }

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

        // Cleanup canvas ID
        canvasTracker.unregisterCanvasData(CanvasItem.getCanvasCode(combinedStack));

        // Remove split canvas item
        this.menu.getCombinedHandler().setStackInSlot(0, ItemStack.EMPTY);
    }

    /**
     * Call changed container to update output slot
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
            colorBuffer.get(offset + (blockWidth * resolution * y) * 4, destinationColor, y * resolution * 4, resolution * 4);
        }

        return destinationColor;
    }
}
