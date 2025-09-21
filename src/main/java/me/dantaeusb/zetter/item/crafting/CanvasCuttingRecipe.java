package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.CanvasStitchingHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CanvasCuttingRecipe extends CustomRecipe {
    public CanvasCuttingRecipe(ResourceLocation id) {
        super(id, CraftingBookCategory.MISC);
    }

    @Override
    public String toString() {
        return "CanvasCuttingRecipe []";
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return new ItemStack(ZetterItems.CANVAS.get());
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(@NotNull CraftingContainer craftingInventory, @NotNull Level level) {
        ItemStack canvas = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.getContainerSize(); i++) {
            ItemStack currentStack = craftingInventory.getItem(i);
            if (!currentStack.isEmpty()) {
                if (currentStack.getItem() != ZetterItems.CANVAS.get()) {
                    return false;
                }
                if (!canvas.isEmpty()) {
                    // More than one canvas found
                    return false;
                }
                canvas = currentStack;
            }
        }

        if (canvas.isEmpty()) {
            return false;
        }

        int[] blockSize = CanvasItem.getBlockSize(canvas);

        return blockSize != null && blockSize[0] > 1 || blockSize[1] > 1;
    }

    /**
     * Returns an Item that is the result of this recipe.
     * The actual canvas on that item will be written after the recipe is actually used,
     * by handling an event (Forge: PlayerContainerEvent.ItemCraftedEvent)
     *
     * I am avoiding full canvas registration here, as that would trash the canvas data
     * storage with potentially thousands of discarded canvases.
     */
    public @NotNull ItemStack assemble(@NotNull CraftingContainer craftingInventory, @NotNull RegistryAccess registryAccess) {
        CanvasStitchingHelper.CanvasGridRectangle canvasGridRectangle = CanvasStitchingHelper.getCraftingContainerCanvasRectangle(craftingInventory);

        if (canvasGridRectangle == null) {
            return ItemStack.EMPTY;
        }

        boolean anyCanvasHasData = craftingInventory.hasAnyMatching(stack -> !CanvasItem.isEmpty(stack));

        ItemStack outCanvas = new ItemStack(ZetterItems.CANVAS.get());
        outCanvas.setCount(1);
        // Should use combined code only if there's painting data
        if (anyCanvasHasData) {
            DummyCanvasData stitchedCanvas = CanvasStitchingHelper.createStitchedCanvasData(craftingInventory, canvasGridRectangle, Minecraft.getInstance().level);
            //stitchedCanvas
            CanvasItem.setCanvasCode(outCanvas, Helper.COMBINED_CANVAS_CODE);
        }
        CanvasItem.setBlockSize(outCanvas, canvasGridRectangle.width, canvasGridRectangle.height);

        return outCanvas;
    }

    /**
     * @return
     */
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.COPYING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 || height >= 2;
    }

    public static class Serializer implements RecipeSerializer<CanvasCuttingRecipe> {
        @Override
        public @NotNull CanvasCuttingRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            return new CanvasCuttingRecipe(recipeId);
        }

        @Override
        public CanvasCuttingRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new CanvasCuttingRecipe(recipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CanvasCuttingRecipe recipe) {

        }
    }
}