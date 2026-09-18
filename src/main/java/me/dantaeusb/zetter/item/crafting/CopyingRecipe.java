package me.dantaeusb.zetter.item.crafting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.item.PaletteItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

/**
 * Only for frames, toggle
 */
public class CopyingRecipe extends CustomRecipe {
    private final Ingredient inputPainting;
    private final Ingredient inputCanvas;
    private final Ingredient inputPalette;

    public CopyingRecipe(Ingredient inputPainting, Ingredient inputCanvas, Ingredient inputPalette) {
        super(CraftingBookCategory.MISC);
        this.inputPainting = inputPainting;
        this.inputCanvas = inputCanvas;
        this.inputPalette = inputPalette;
    }

    @Override
    public String toString() {
        return "CopyingRecipe [inputPainting=" + this.inputPainting + ", inputCanvas=" + this.inputCanvas + ", inputPalette=" + this.inputPalette + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput craftingInventory, Level world) {
        ItemStack paintingStack = ItemStack.EMPTY;
        ItemStack canvasStack = ItemStack.EMPTY;
        ItemStack paletteStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputPainting.test(stack)) {
                if (!paintingStack.isEmpty()) {
                    Zetter.LOG.info("CopyingRecipe: matches failed because multiple paintings found");
                    return false;
                }

                paintingStack = stack;
            } else if (this.inputCanvas.test(stack)) {
                if (!canvasStack.isEmpty()) {
                    Zetter.LOG.info("CopyingRecipe: matches failed because multiple canvases found");
                    return false;
                }

                canvasStack = stack;
            } else if (this.inputPalette.test(stack)) {
                if (!paletteStack.isEmpty()) {
                    Zetter.LOG.info("CopyingRecipe: matches failed because multiple palettes found");
                    return false;
                }

                paletteStack = stack;
            } else {
                Zetter.LOG.info("CopyingRecipe: matches failed because unrelated item found: " + stack.getItem());
                return false;
            }
        }

        if (paintingStack.isEmpty() || canvasStack.isEmpty() || paletteStack.isEmpty()) {
            return false;
        }

        if (PaintingItem.isEmpty(paintingStack)) {
            Zetter.LOG.info("CopyingRecipe: matches failed because painting is empty");
            return false;
        }

        if (!CanvasItem.isEmpty(canvasStack)) {
            Zetter.LOG.info("CopyingRecipe: matches failed because canvas is not empty");
            return false;
        }

        int paletteDamage = paletteStack.getDamageValue();
        final int maxDamage = paletteStack.getMaxDamage() - 1;
        int newDamage = paletteDamage + calculatePaletteDamage(paintingStack);

        if (newDamage > maxDamage) {
            Zetter.LOG.info("CopyingRecipe: matches failed because palette is too damaged / not enough durability");
            return false;
        }

        int[] paintingSize = PaintingItem.getBlockSize(paintingStack);
        int[] canvasSize = CanvasItem.getBlockSize(canvasStack);

        if (!Arrays.equals(paintingSize, canvasSize)) {
            Zetter.LOG.info("CopyingRecipe: matches failed because painting and canvas size do not match");
            return false;
        }

        Zetter.LOG.info("CopyingRecipe: MATCHED SUCCESS!");
        return true;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public @NotNull ItemStack assemble(CraftingInput craftingInventory, HolderLookup.Provider registries) {
        ItemStack paintingStack = ItemStack.EMPTY;
        ItemStack canvasStack = ItemStack.EMPTY;
        ItemStack paletteStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (this.inputPainting.test(stack)) {
                if (!paintingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paintingStack = stack;
            } else if (this.inputCanvas.test(stack)) {
                if (!canvasStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                canvasStack = stack;
            } else if (this.inputPalette.test(stack)) {
                if (!paletteStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paletteStack = stack;
            }
        }

        if (paintingStack.isEmpty() || !Helper.hasTag(paintingStack)) {
            Zetter.LOG.info("CopyingRecipe: assemble failed: painting empty or has no tag");
            return ItemStack.EMPTY;
        }

        if (PaintingItem.isEmpty(paintingStack)) {
            Zetter.LOG.info("CopyingRecipe: assemble failed: painting is empty");
            return ItemStack.EMPTY;
        }

        if (!CanvasItem.isEmpty(canvasStack)) {
            Zetter.LOG.info("CopyingRecipe: assemble failed: canvas is not empty");
            return ItemStack.EMPTY;
        }

        int paletteDamage = paletteStack.getDamageValue();
        final int maxDamage = paletteStack.getMaxDamage() - 1;
        int newDamage = paletteDamage + calculatePaletteDamage(paintingStack);

        if (newDamage > maxDamage) {
            Zetter.LOG.info("CopyingRecipe: assemble failed: palette too damaged");
            return ItemStack.EMPTY;
        }

        int[] paintingSize = PaintingItem.getBlockSize(paintingStack);
        int[] canvasSize = CanvasItem.getBlockSize(canvasStack);

        if (!Arrays.equals(paintingSize, canvasSize)) {
            Zetter.LOG.info("CopyingRecipe: assemble failed: size mismatch");
            return ItemStack.EMPTY;
        }

        ItemStack outStack = paintingStack.copy();
        outStack.setCount(1);

        CompoundTag compoundTag = Helper.getTag(paintingStack).copy();
        Helper.setTag(outStack, compoundTag);

        int generation = Math.min(PaintingItem.GENERATION_COPY_OF_COPY, PaintingItem.getGeneration(paintingStack) + 1);
        PaintingItem.setGeneration(outStack, generation);

        Zetter.LOG.info("CopyingRecipe: assemble success, output stack: " + outStack);
        return outStack;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        ItemStack originalPaintingStack = null;
        ItemStack paletteStack = null;
        int paletteDamage = 0;

        for (int i = 0; i < remainingItems.size(); ++i) {
            ItemStack stackInSlot = inv.getItem(i);

            if (stackInSlot.getItem() instanceof PaintingItem) {
                Item originalPainting = stackInSlot.getItem();

                originalPaintingStack = new ItemStack(originalPainting);
                originalPaintingStack.setCount(1);

                if (Helper.hasTag(stackInSlot)) {
                    CompoundTag compoundTag = Helper.getTag(stackInSlot).copy();
                    Helper.setTag(originalPaintingStack, compoundTag);
                }

                remainingItems.set(i, originalPaintingStack);
            } else if (stackInSlot.getItem() instanceof PaletteItem) {
                Item palette = stackInSlot.getItem();
                paletteDamage = stackInSlot.getDamageValue();

                paletteStack = new ItemStack(palette);
                paletteStack.setCount(1);

                if (Helper.hasTag(stackInSlot)) {
                    CompoundTag compoundTag = Helper.getTag(stackInSlot).copy();
                    Helper.setTag(paletteStack, compoundTag);
                }
                paletteStack.setDamageValue(stackInSlot.getDamageValue());

                remainingItems.set(i, paletteStack);
            }
        }

        if (originalPaintingStack != null && paletteStack != null) {
            final int maxDamage = paletteStack.getMaxDamage() - 1;
            int newDamage = paletteDamage + calculatePaletteDamage(originalPaintingStack);
            newDamage = Math.min(newDamage, maxDamage);

            paletteStack.setDamageValue(newDamage);
        }

        return remainingItems;
    }

    private static int calculatePaletteDamage(ItemStack painting) {
        int[] paintingSize = PaintingItem.getBlockSize(painting);

        if (paintingSize == null || paintingSize.length != 2) {
            Zetter.LOG.error("Cannot find painting size to damage palette");
            return 0;
        }

        return (paintingSize[0] * PaintingItem.getResolution(painting)) * (paintingSize[1] * PaintingItem.getResolution(painting));
    }

    /**
     * @return
     */
    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.COPYING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static final MapCodec<CopyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("painting").forGetter(recipe -> recipe.inputPainting),
        Ingredient.CODEC.fieldOf("canvas").forGetter(recipe -> recipe.inputCanvas),
        Ingredient.CODEC.fieldOf("palette").forGetter(recipe -> recipe.inputPalette)
    ).apply(instance, CopyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputPainting,
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputCanvas,
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputPalette,
        CopyingRecipe::new
    );

    public static class Serializer implements RecipeSerializer<CopyingRecipe> {
        @Override
        public MapCodec<CopyingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CopyingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}