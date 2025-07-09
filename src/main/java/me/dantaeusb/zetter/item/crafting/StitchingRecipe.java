package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.item.PaletteItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Only for frames, toggle
 */
public class CopyingRecipe extends CustomRecipe {
    private final Ingredient inputPainting;
    private final Ingredient inputCanvas;
    private final Ingredient inputPalette;

    public CopyingRecipe(ResourceLocation id, Ingredient inputPainting, Ingredient inputCanvas, Ingredient inputPalette) {
        super(id, CraftingBookCategory.MISC);

        this.inputPainting = inputPainting;
        this.inputCanvas = inputCanvas;
        this.inputPalette = inputPalette;
    }

    @Override
    public String toString() {
        return "FramingRecipe [inputPainting=" + this.inputPainting + ", inputCanvas=" + this.inputCanvas + ", inputPalette=" + this.inputPalette + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer craftingInventory, Level world) {
        ItemStack paintingStack = ItemStack.EMPTY;
        ItemStack canvasStack = ItemStack.EMPTY;
        ItemStack paletteStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).isEmpty()) {
                continue;
            }

            if (this.inputPainting.test(craftingInventory.getItem(i))) {
                if (!paintingStack.isEmpty()) {
                    return false;
                }

                paintingStack = craftingInventory.getItem(i);
            } else if (this.inputCanvas.test(craftingInventory.getItem(i))) {
                if (!canvasStack.isEmpty()) {
                    return false;
                }

                canvasStack = craftingInventory.getItem(i);
            } else if (this.inputPalette.test(craftingInventory.getItem(i))) {
                if (!paletteStack.isEmpty()) {
                    return false;
                }

                paletteStack = craftingInventory.getItem(i);
            } else {
                // We have something else in the grid
                return false;
            }
        }

        // Check that we actually have items
        if (paintingStack.isEmpty() || canvasStack.isEmpty() || paletteStack.isEmpty()) {
            return false;
        }

        // There's no painting data
        if (PaintingItem.isEmpty(paintingStack)) {
            return false;
        }

        // Canvas to copy is not empty and could be overwritten
        if (!CanvasItem.isEmpty(canvasStack)) {
            return false;
        }

        // Check if we have enough paints in palette
        int paletteDamage = paletteStack.getDamageValue();
        final int maxDamage = paletteStack.getMaxDamage() - 1;
        int newDamage = paletteDamage + calculatePaletteDamage(paintingStack);

        if (newDamage > maxDamage) {
            return false;
        }

        // Check that the sizes are equal
        int[] paintingSize = PaintingItem.getBlockSize(paintingStack);
        int[] canvasSize = CanvasItem.getBlockSize(canvasStack);

        if (!Arrays.equals(paintingSize, canvasSize)) {
            return false;
        }

        return true;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public @NotNull ItemStack assemble(CraftingContainer craftingInventory, RegistryAccess registryAccess) {
        ItemStack paintingStack = ItemStack.EMPTY;
        ItemStack canvasStack = ItemStack.EMPTY;
        ItemStack paletteStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (this.inputPainting.test(craftingInventory.getItem(i))) {
                if (!paintingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paintingStack = craftingInventory.getItem(i);
            } else if (this.inputCanvas.test(craftingInventory.getItem(i))) {
                if (!canvasStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                canvasStack = craftingInventory.getItem(i);
            } else if (this.inputPalette.test(craftingInventory.getItem(i))) {
                if (!paletteStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paletteStack = craftingInventory.getItem(i);
            }
        }

        if (paintingStack.isEmpty() || !paintingStack.hasTag()) {
            return ItemStack.EMPTY;
        }

        // There's no painting data
        if (PaintingItem.isEmpty(paintingStack)) {
            return ItemStack.EMPTY;
        }

        // Canvas to copy is not empty and could be overwritten
        if (!CanvasItem.isEmpty(canvasStack)) {
            return ItemStack.EMPTY;
        }

        // Check if we have enough paints in palette
        int paletteDamage = paletteStack.getDamageValue();
        final int maxDamage = paletteStack.getMaxDamage() - 1;
        int newDamage = paletteDamage + calculatePaletteDamage(paintingStack);

        if (newDamage > maxDamage) {
            return ItemStack.EMPTY;
        }

        int[] paintingSize = PaintingItem.getBlockSize(paintingStack);
        int[] canvasSize = CanvasItem.getBlockSize(canvasStack);

        if (!Arrays.equals(paintingSize, canvasSize)) {
            return ItemStack.EMPTY;
        }

        ItemStack outStack = paintingStack.copy();
        outStack.setCount(1);

        CompoundTag compoundTag = paintingStack.getTag().copy();
        outStack.setTag(compoundTag);

        int generation = Math.min(PaintingItem.GENERATION_COPY_OF_COPY, PaintingItem.getGeneration(paintingStack) + 1);
        PaintingItem.setGeneration(outStack, generation);

        return outStack;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        ItemStack originalPaintingStack = null;
        ItemStack paletteStack = null;
        int paletteDamage = 0;

        for (int i = 0; i < remainingItems.size(); ++i) {
            ItemStack stackInSlot = inv.getItem(i);

            if (stackInSlot.getItem() instanceof PaintingItem) {
                Item originalPainting = stackInSlot.getItem();

                originalPaintingStack = new ItemStack(originalPainting);
                originalPaintingStack.setCount(1);

                CompoundTag compoundTag = stackInSlot.getTag().copy();

                originalPaintingStack.setTag(compoundTag);

                remainingItems.set(i, originalPaintingStack);
            } else if (stackInSlot.getItem() instanceof PaletteItem) {
                Item palette = stackInSlot.getItem();
                paletteDamage = stackInSlot.getDamageValue();

                paletteStack = new ItemStack(palette);
                paletteStack.setCount(1);

                CompoundTag compoundTag = stackInSlot.getTag().copy();

                paletteStack.setTag(compoundTag);
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

    public static class Serializer implements RecipeSerializer<CopyingRecipe> {
        @Override
        public CopyingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputPaintingJson = GsonHelper.getAsJsonObject(json, "painting");
            final Ingredient inputPainting = Ingredient.fromJson(inputPaintingJson);

            final JsonElement inputCanvasJson = GsonHelper.getAsJsonObject(json, "canvas");
            final Ingredient inputCanvas = Ingredient.fromJson(inputCanvasJson);

            final JsonElement inputPaletteJson = GsonHelper.getAsJsonObject(json, "palette");
            final Ingredient inputPalette = Ingredient.fromJson(inputPaletteJson);

            return new CopyingRecipe(recipeId, inputPainting, inputCanvas, inputPalette);
        }

        @Override
        public CopyingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient paintingIngredient = Ingredient.fromNetwork(buffer);
            Ingredient canvasIngredient = Ingredient.fromNetwork(buffer);
            Ingredient paletteIngredient = Ingredient.fromNetwork(buffer);
            return new CopyingRecipe(recipeId, paintingIngredient, canvasIngredient, paletteIngredient);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CopyingRecipe recipe) {
            recipe.inputPainting.toNetwork(buffer);
            recipe.inputCanvas.toNetwork(buffer);
            recipe.inputPalette.toNetwork(buffer);
        }
    }
}