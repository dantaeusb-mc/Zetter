package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cutting halves each side of a canvas: ceil, then floor, a side of 1 stays whole.
 * Every part side is 1 or 2, so every part is a valid shape, there are at most 2x2
 * parts, so they fit any crafting grid, and any canvas is down to 1x1 in two cuts.
 * <p>
 * Top-left part is the crafting result, see {@link #finishCutting}; the rest are
 * returned to the grid in place, see CanvasCuttingRecipe#getRemainingItems.
 */
public class CanvasCuttingHelper {
    public static final String NBT_TAG_CUT_SOURCE = "CutSource";
    private static final String NBT_TAG_CODE = "Code";
    private static final String NBT_TAG_PARTS = "Parts";

    /**
     * @param column position in the parts layout, 0 or 1
     * @param row position in the parts layout, 0 or 1
     * @param blockX offset in the source, in blocks
     * @param blockY offset in the source, in blocks
     */
    public record Part(int column, int row, int blockX, int blockY, int blockWidth, int blockHeight) {}

    public static int[] halve(int blocks) {
        if (blocks <= 1) {
            return new int[]{blocks};
        }

        return new int[]{(blocks + 1) / 2, blocks / 2};
    }

    /**
     * @return parts row by row, top-left first
     */
    public static List<Part> cut(int blockWidth, int blockHeight) {
        final int[] columns = halve(blockWidth);
        final int[] rows = halve(blockHeight);
        final List<Part> parts = new ArrayList<>();

        int blockY = 0;

        for (int row = 0; row < rows.length; row++) {
            int blockX = 0;

            for (int column = 0; column < columns.length; column++) {
                parts.add(new Part(column, row, blockX, blockY, columns[column], rows[row]));
                blockX += columns[column];
            }

            blockY += rows[row];
        }

        return parts;
    }

    /**
     * Where the parts layout starts in the grid: on the source slot,
     * moved back from the edges when it doesn't fit
     *
     * @return top-left slot of the layout, x and y
     */
    public static int[] placeInGrid(int sourceX, int sourceY, int columns, int rows, int gridWidth, int gridHeight) {
        return new int[]{
            Math.min(sourceX, gridWidth - columns),
            Math.min(sourceY, gridHeight - rows)
        };
    }

    /**
     * @return slot of the only item in the grid if it's a canvas, -1 otherwise
     */
    public static int findSource(CraftingContainer craftingContainer) {
        int sourceSlot = -1;

        for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
            final ItemStack stack = craftingContainer.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (sourceSlot != -1 || !stack.is(ZetterItems.CANVAS.get())) {
                return -1;
            }

            sourceSlot = i;
        }

        return sourceSlot;
    }

    /**
     * Marks the crafting result until it becomes the top-left part.
     * Code is left out for a blank source.
     */
    public static CompoundTag createCutSourceTag(ItemStack sourceStack, int parts) {
        final CompoundTag tag = new CompoundTag();
        final String sourceCode = CanvasItem.getCanvasCode(sourceStack);

        if (sourceCode != null) {
            tag.putString(NBT_TAG_CODE, sourceCode);
        }

        tag.putInt(NBT_TAG_PARTS, parts);

        return tag;
    }

    public static int getCutParts(ItemStack stack) {
        final CompoundTag tag = stack.getTagElement(NBT_TAG_CUT_SOURCE);

        return tag == null ? 0 : tag.getInt(NBT_TAG_PARTS);
    }

    /**
     * Craft is confirmed and the source is still in the grid: turns the result
     * into the top-left part. The other parts come after, from the recipe.
     *
     * @param craftingContainer
     * @param craftedStack
     * @param player
     */
    public static void finishCutting(CraftingContainer craftingContainer, ItemStack craftedStack, Player player) {
        final int sourceSlot = findSource(craftingContainer);

        if (sourceSlot == -1) {
            Zetter.LOG.warn("Failed to cut canvas: No single canvas found in crafting inventory.");
            return;
        }

        final ItemStack sourceStack = craftingContainer.getItem(sourceSlot);
        final int[] size = CanvasItem.getBlockSize(sourceStack);
        final List<Part> parts = cut(size[0], size[1]);
        final CompoundTag cutSourceTag = createCutSourceTag(sourceStack, parts.size());

        final ItemStack topLeftStack = createPart(sourceStack, parts.get(0), player.level());
        final ItemStack resultStack = CanvasItem.findCraftedStack(
            player,
            craftedStack,
            stack -> cutSourceTag.equals(stack.getTagElement(NBT_TAG_CUT_SOURCE))
        );

        if (resultStack == null) {
            // Source is released after this, the part must reach the player anyway
            Zetter.LOG.error("Cannot find cut canvas after crafting, giving the part directly");
            player.getInventory().placeItemBackInInventory(topLeftStack);
            return;
        }

        resultStack.setTag(topLeftStack.getTag());
    }

    /**
     * Blank part for a blank source, otherwise a new canvas registered
     * with the part of source's pixels; server only for painted source
     *
     * @param sourceStack
     * @param part
     * @param level
     * @return
     */
    public static ItemStack createPart(ItemStack sourceStack, Part part, Level level) {
        final String sourceCode = CanvasItem.getCanvasCode(sourceStack);

        if (sourceCode == null) {
            return CanvasItem.createBlank(part.blockWidth(), part.blockHeight());
        }

        final CanvasData sourceData = CanvasItem.getCanvasData(sourceStack, level);

        if (sourceData == null) {
            Zetter.LOG.error("Cut canvas " + sourceCode + " has no data, part is left blank");
            return CanvasItem.createBlank(part.blockWidth(), part.blockHeight());
        }

        final int resolution = sourceData.getResolution().getNumeric();
        final int pixelWidth = part.blockWidth() * resolution;
        final int pixelHeight = part.blockHeight() * resolution;

        final CanvasData partData = CanvasData.BUILDER.createWrap(
            sourceData.getResolution(),
            pixelWidth,
            pixelHeight,
            sliceColorData(
                sourceData.getColorData(),
                sourceData.getWidth(),
                part.blockX() * resolution,
                part.blockY() * resolution,
                pixelWidth,
                pixelHeight
            )
        );

        final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);
        final String partCode = CanvasData.getCanvasCode(canvasTracker.getFreeCanvasId());

        canvasTracker.registerCanvasData(partCode, partData);

        final ItemStack partStack = new ItemStack(ZetterItems.CANVAS.get());
        CanvasItem.storeCanvasData(partStack, partCode, partData);

        return partStack;
    }

    /**
     * Copies a rectangle of pixels out of color data, 4 bytes per pixel
     *
     * @param colorData
     * @param sourcePixelWidth
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    public static byte[] sliceColorData(byte[] colorData, int sourcePixelWidth, int x, int y, int width, int height) {
        final int COLOR_SIZE = 4;
        final byte[] slice = new byte[width * height * COLOR_SIZE];

        for (int row = 0; row < height; row++) {
            System.arraycopy(
                colorData,
                ((y + row) * sourcePixelWidth + x) * COLOR_SIZE,
                slice,
                row * width * COLOR_SIZE,
                width * COLOR_SIZE
            );
        }

        return slice;
    }
}
