package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Helper to handle events and combination of the combined canvas when stitching.
 */
public class CanvasStitchingHelper {
    /**
     * Craft is confirmed and parts are about to be consumed: creates
     * the stitched canvas on the result, then releases the parts.
     *
     * @param craftingContainer
     * @param craftedStack
     * @param player
     */
    public static void finishStitching(CraftingContainer craftingContainer, ItemStack craftedStack, Player player) {
        final CanvasGridRectangle canvasGridRectangle = getCraftingContainerCanvasRectangle(craftingContainer);

        if (canvasGridRectangle == null) {
            Zetter.LOG.warn("Failed to create combined canvas data: No valid canvas rectangle found in crafting inventory.");
            return;
        }

        final StitchParts stitchParts = StitchParts.fromContainer(craftingContainer, canvasGridRectangle);

        // Blank parts only: result is a blank canvas of the new size already
        if (!stitchParts.hasAnyCode()) {
            return;
        }

        final ItemStack resultStack = CanvasItem.findCraftedStack(
            player,
            craftedStack,
            stack -> stitchParts.equals(StitchParts.readFrom(stack))
        );

        if (resultStack == null) {
            // Keep parts: they are the only data this painting has
            Zetter.LOG.error("Cannot find stitched canvas after crafting, parts are kept");
            return;
        }

        writeStitchedCanvasData(resultStack, stitchParts, player.level());
        releaseParts(stitchParts, player.level());
    }

    /**
     * Registers stitched canvas under a new code and writes it to the stack
     *
     * @param canvasItemStack
     * @param stitchParts
     * @param level
     */
    public static void writeStitchedCanvasData(ItemStack canvasItemStack, StitchParts stitchParts, Level level) {
        final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);
        final DummyCanvasData stitchedCanvasData = createStitchedCanvasData(stitchParts, level, true);

        // Only null when missing parts are not allowed
        assert stitchedCanvasData != null;

        final CanvasData combinedCanvasData = CanvasData.BUILDER.createWrap(
            stitchedCanvasData.getResolution(),
            stitchedCanvasData.getWidth(),
            stitchedCanvasData.getHeight(),
            stitchedCanvasData.getColorData()
        );

        final int newId = canvasTracker.getFreeCanvasId();
        final String newCode = CanvasData.getCanvasCode(newId);

        canvasTracker.registerCanvasData(newCode, combinedCanvasData);
        CanvasItem.storeCanvasData(canvasItemStack, newCode, combinedCanvasData);
        StitchParts.removeFrom(canvasItemStack);
    }

    /**
     * Parts are consumed by the craft, free their ids and data,
     * players tracking them are notified by the tracker
     *
     * @param stitchParts
     * @param level
     */
    public static void releaseParts(StitchParts stitchParts, Level level) {
        final CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(level);

        for (String partCode : stitchParts.codes) {
            if (partCode != null && canvasTracker.getCanvasData(partCode) != null) {
                canvasTracker.unregisterCanvasData(partCode);
            }
        }
    }

    /**
     * Copies every part into one canvas, blank parts are filled with default color.
     *
     * @param stitchParts
     * @param level
     * @param fillMissingParts
     * @return null if any part with a code has no data and missing parts are not filled
     */
    public static @Nullable DummyCanvasData createStitchedCanvasData(StitchParts stitchParts, Level level, boolean fillMissingParts) {
        final int COLOR_SIZE = 4;
        final int resolution = Helper.getResolution().getNumeric();
        final CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(level);

        final int partPixelWidth = stitchParts.partBlockSize[0] * resolution;
        final int partPixelHeight = stitchParts.partBlockSize[1] * resolution;
        final int pixelWidth = stitchParts.width * partPixelWidth;
        final int pixelHeight = stitchParts.height * partPixelHeight;

        ByteBuffer color = ByteBuffer.allocate(pixelWidth * pixelHeight * COLOR_SIZE);

        for (int partY = 0; partY < stitchParts.height; partY++) {
            for (int partX = 0; partX < stitchParts.width; partX++) {
                final String partCode = stitchParts.codes[partY * stitchParts.width + partX];
                AbstractCanvasData partCanvasData = null;

                if (partCode != null) {
                    partCanvasData = canvasTracker.getCanvasData(partCode);

                    if (partCanvasData == null) {
                        if (!fillMissingParts) {
                            return null;
                        }

                        Zetter.LOG.error("Stitching part " + partCode + " has no data, filling with default color");
                    }
                }

                for (int smallY = 0; smallY < partPixelHeight; smallY++) {
                    for (int smallX = 0; smallX < partPixelWidth; smallX++) {
                        final int bigX = partX * partPixelWidth + smallX;
                        final int bigY = partY * partPixelHeight + smallY;

                        final int colorIndex = (bigY * pixelWidth + bigX) * COLOR_SIZE;

                        if (partCanvasData != null && smallX < partCanvasData.getWidth() && smallY < partCanvasData.getHeight()) {
                            color.putInt(colorIndex, partCanvasData.getColorAt(smallX, smallY));
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

    /**
     * What a stitched canvas is made of, carried on the crafting result
     * until the stitched canvas is actually created. Lets client build
     * a preview from the result stack alone: it never runs the recipe.
     */
    public static class StitchParts {
        public static final String NBT_TAG_STITCH_PARTS = "StitchParts";
        private static final String NBT_TAG_WIDTH = "Width";
        private static final String NBT_TAG_HEIGHT = "Height";
        private static final String NBT_TAG_PART_BLOCK_SIZE = "PartBlockSize";
        private static final String NBT_TAG_CODES = "Codes";

        // Grid size, in parts
        public final int width;
        public final int height;
        public final int[] partBlockSize;
        // Row-major, null for a part without painting
        public final @Nullable String[] codes;

        StitchParts(int width, int height, int[] partBlockSize, @Nullable String[] codes) {
            this.width = width;
            this.height = height;
            this.partBlockSize = partBlockSize;
            this.codes = codes;
        }

        public static StitchParts fromContainer(CraftingContainer craftingContainer, CanvasGridRectangle canvasGridRectangle) {
            final String[] codes = new String[canvasGridRectangle.width * canvasGridRectangle.height];

            for (int y = 0; y < canvasGridRectangle.height; y++) {
                for (int x = 0; x < canvasGridRectangle.width; x++) {
                    final int slot = (canvasGridRectangle.y + y) * craftingContainer.getWidth() + canvasGridRectangle.x + x;
                    codes[y * canvasGridRectangle.width + x] = CanvasItem.getCanvasCode(craftingContainer.getItem(slot));
                }
            }

            return new StitchParts(canvasGridRectangle.width, canvasGridRectangle.height, canvasGridRectangle.canvasBlockSize, codes);
        }

        public static @Nullable StitchParts readFrom(ItemStack stack) {
            final CompoundTag tag = stack.getTag();

            if (tag == null || !tag.contains(NBT_TAG_STITCH_PARTS, Tag.TAG_COMPOUND)) {
                return null;
            }

            final CompoundTag partsTag = tag.getCompound(NBT_TAG_STITCH_PARTS);
            final int width = partsTag.getInt(NBT_TAG_WIDTH);
            final int height = partsTag.getInt(NBT_TAG_HEIGHT);
            final int[] partBlockSize = partsTag.getIntArray(NBT_TAG_PART_BLOCK_SIZE);
            final ListTag codesTag = partsTag.getList(NBT_TAG_CODES, Tag.TAG_STRING);

            if (width <= 0 || height <= 0 || partBlockSize.length != 2 || codesTag.size() != width * height) {
                return null;
            }

            final String[] codes = new String[codesTag.size()];

            for (int i = 0; i < codes.length; i++) {
                final String code = codesTag.getString(i);
                codes[i] = code.isEmpty() ? null : code;
            }

            return new StitchParts(width, height, partBlockSize, codes);
        }

        public void writeTo(ItemStack stack) {
            final CompoundTag partsTag = new CompoundTag();
            partsTag.putInt(NBT_TAG_WIDTH, this.width);
            partsTag.putInt(NBT_TAG_HEIGHT, this.height);
            partsTag.putIntArray(NBT_TAG_PART_BLOCK_SIZE, this.partBlockSize);

            final ListTag codesTag = new ListTag();

            for (String code : this.codes) {
                codesTag.add(StringTag.valueOf(code == null ? "" : code));
            }

            partsTag.put(NBT_TAG_CODES, codesTag);
            stack.getOrCreateTag().put(NBT_TAG_STITCH_PARTS, partsTag);
        }

        public static void removeFrom(ItemStack stack) {
            if (stack.getTag() != null) {
                stack.getTag().remove(NBT_TAG_STITCH_PARTS);
            }
        }

        public boolean hasAnyCode() {
            for (String code : this.codes) {
                if (code != null) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StitchParts that
                && this.width == that.width
                && this.height == that.height
                && Arrays.equals(this.partBlockSize, that.partBlockSize)
                && Arrays.equals(this.codes, that.codes);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(this.codes) + Arrays.hashCode(this.partBlockSize);
        }
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

        /**
         * Size of the stitched canvas in blocks: the grid counts canvases, and every
         * canvas in it is canvasBlockSize blocks big
         *
         * @return
         */
        public int[] getStitchedBlockSize() {
            return new int[]{
                this.width * this.canvasBlockSize[0],
                this.height * this.canvasBlockSize[1]
            };
        }
    }
}
