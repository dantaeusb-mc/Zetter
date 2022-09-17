package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class CanvasSplitAction extends AbstractCanvasAction {
    public CanvasSplitAction(ArtistTableMenu artistTableMenu, Level level) {
        super(artistTableMenu, level);
    }

    public boolean mayPlaceCombined(ItemStack stack) {
        if (stack.is(ZetterItems.CANVAS.get())) {
            return !CanvasItem.isEmpty(stack, this.level) && CanvasItem.isCompound(stack);
        }

        return false;
    }

    /**
     * When compound canvas is placed we
     * should populate the grid with preview
     * of the result, empty non-compound canvases
     * without data
     * <p>
     * When canvas removed, if we see canvases
     * without data (empty), that means we had
     * preview here in place before, so it's safe
     * to remove all canvases from the grid
     *
     * @param combinedContainer
     */
    @Override
    public void onChangedCombined(ItemStackHandler combinedContainer) {
        ItemStack combinedStack = combinedContainer.getStackInSlot(0);

        if (combinedStack.isEmpty() || !combinedStack.is(ZetterItems.CANVAS.get()) || !CanvasItem.isCompound(combinedStack)) {
            return;
        }

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        // Set data of split canvas parts to grid items
        this.forEveryGridSlot((ItemStackHandler gridContainer, int x, int y, ItemStack gridStack, int slotNumber) -> {
            if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                gridContainer.setStackInSlot(slotNumber, new ItemStack(ZetterItems.CANVAS.get()));
            } else {
                if (
                        !gridContainer.getStackInSlot(slotNumber).is(ZetterItems.CANVAS.get()) ||
                                !CanvasItem.isEmpty(gridContainer.getStackInSlot(slotNumber), this.level)
                ) {
                    gridContainer.setStackInSlot(slotNumber, ItemStack.EMPTY);
                }
            }
        });
    }

    /**
     * Remove the combined canvas, set the data for the
     * partial canvases on the grid
     *
     * @param player
     * @param stack
     */
    @Override
    public void onTakeGrid(Player player, ItemStack stack) {
        if (this.level.isClientSide()) {
            return;
        }

        ItemStack combinedStack = this.menu.getCombinedContainer().getStackInSlot(0);

        if (combinedStack.isEmpty() || !combinedStack.is(ZetterItems.CANVAS.get()) || !CanvasItem.isCompound(combinedStack)) {
            return;
        }

        // Remove split canvas item
        this.menu.getCombinedContainer().setStackInSlot(0, ItemStack.EMPTY);

        // Get data from split canvas
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getWorldCanvasTracker(player.getLevel());
        final CanvasData combinedCanvasData = CanvasItem.getCanvasData(combinedStack, this.level);

        if (combinedCanvasData == null) {
            Zetter.LOG.error("No canvas data found for item in combined slot");
            return;
        }

        int[] compoundCanvasSize = CanvasItem.getBlockSize(combinedStack);
        assert compoundCanvasSize != null;

        final int compoundCanvasWidth = compoundCanvasSize[0];
        final int compoundCanvasHeight = compoundCanvasSize[1];

        final int numericResolution = combinedCanvasData.getResolution().getNumeric();

        AtomicInteger missingX = new AtomicInteger();
        AtomicInteger missingY = new AtomicInteger();

        // Set data of split canvas parts to grid items
        this.forEveryGridSlot((ItemStackHandler gridContainer, int x, int y, ItemStack gridStack, int slotNumber) -> {
            if (x < compoundCanvasWidth && y < compoundCanvasHeight) {
                if (gridStack.isEmpty()/* || !CanvasItem.isEmpty(gridStack, this.level)*/) {
                    missingX.set(x);
                    missingY.set(y);
                    return;
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

                String canvasCode = CanvasData.getCanvasCode(canvasTracker.getFreeCanvasId());
                CanvasItem.storeCanvasData(gridStack, canvasCode, itemData);
            }
        });

        // Set data for the picked item
        CanvasData itemData = CanvasData.createWrap(
                combinedCanvasData.getResolution(),
                numericResolution,
                numericResolution,
                getPartialColorData(
                        combinedCanvasData.getColorData(),
                        numericResolution,
                        missingX.get(),
                        missingY.get(),
                        compoundCanvasWidth,
                        compoundCanvasHeight
                )
        );

        String canvasCode = CanvasData.getCanvasCode(canvasTracker.getFreeCanvasId());
        CanvasItem.storeCanvasData(stack, canvasCode, itemData);
    }

    private static byte[] getPartialColorData(byte[] colorData, int resolution, int blockX, int blockY, int blockWidth, int blockHeight) {
        byte[] destinationColor = new byte[resolution * resolution * 4];
        ByteBuffer colorBuffer = ByteBuffer.wrap(colorData);

        final int offset = (blockY * resolution * blockWidth * resolution) + blockX * resolution;

        for (int y = 0; y < resolution; y++) {
            colorBuffer.get(offset * 4 + (blockWidth * resolution * y) * 4, destinationColor, y * resolution * 4, resolution * 4);
        }

        return destinationColor;
    }
}
