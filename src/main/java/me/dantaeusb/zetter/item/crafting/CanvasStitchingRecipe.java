package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.*;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CanvasStitchingRecipe extends CustomRecipe {
    final int[][] shapes;

    public CanvasStitchingRecipe(ResourceLocation id, int[][] shapes) {
        super(id, CraftingBookCategory.MISC);

        this.shapes = shapes;
    }

    @Override
    public String toString() {
        return "CanvasStitchingRecipe [shapes=" + Arrays.deepToString(this.shapes) + "]";
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
        CanvasStitchingHelper.CanvasGridRectangle canvasGridRectangle = CanvasStitchingHelper.getCraftingContainerCanvasRectangle(craftingInventory);

        if (canvasGridRectangle == null) {
            return false;
        }

        int blockWidth = canvasGridRectangle.width * canvasGridRectangle.canvasBlockSize[0];
        int blockHeight = canvasGridRectangle.height * canvasGridRectangle.canvasBlockSize[1];

        // Just a single canvas
        if (blockWidth == canvasGridRectangle.canvasBlockSize[0] && blockHeight == canvasGridRectangle.canvasBlockSize[1]) {
            return false;
        }

        // Final block size should be of allowed shapes
        boolean shapeAvailable = false;
        for (int[] shape : this.shapes) {
            if (blockWidth == shape[0] && blockHeight == shape[1]) {
                shapeAvailable = true;
                break;
            }
        }

        if (!shapeAvailable) {
            return false;
        }

        // @todo: It's never client side!
        /*if (level.isClientSide()) {
            ClientCombinedCanvasHelper.getInstance().cleanupCombinedCanvas(level);
            DummyCanvasData combinedCanvasData = ClientCombinedCanvasHelper.getInstance().getOrRequestCanvasesForStitching(craftingInventory, level);

            *//**
             * Otherwise it'll be registered when all parts are ready
             * @see {@link ClientCombinedCanvasHelper#updateCombinedCanvas}
             *//*
            if (combinedCanvasData != null) {
                Helper.getLevelCanvasTracker(level).registerCanvasData(Helper.COMBINED_CANVAS_CODE, combinedCanvasData);
            }
        }*/

        return true;
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

    public static class Serializer implements RecipeSerializer<CanvasStitchingRecipe> {
        @Override
        public @NotNull CanvasStitchingRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            final JsonArray shapesJson = GsonHelper.getAsJsonArray(json, "shapes");
            final int[][] shapes = new int[shapesJson.size()][2];

            for (int i = 0; i < shapesJson.size(); i++) {
                JsonElement shapeElement = shapesJson.get(i);
                if (shapeElement.isJsonArray()) {
                    JsonArray shapeArray = shapeElement.getAsJsonArray();
                    if (shapeArray.size() == 2) {
                        shapes[i][0] = GsonHelper.convertToInt(shapeArray.get(0), "width");
                        shapes[i][1] = GsonHelper.convertToInt(shapeArray.get(1), "height");
                    } else {
                        throw new IllegalArgumentException("Shape must be an array of two integers: [width, height]");
                    }
                } else {
                    throw new IllegalArgumentException("Shape must be an array: " + shapeElement);
                }
            }

            return new CanvasStitchingRecipe(recipeId, shapes);
        }

        @Override
        public CanvasStitchingRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int shapeCount = buffer.readVarInt();
            int[][] shapes = new int[shapeCount][2];

            for (int i = 0; i < shapeCount; i++) {
                shapes[i][0] = buffer.readVarInt();
                shapes[i][1] = buffer.readVarInt();
            }

            return new CanvasStitchingRecipe(recipeId, shapes);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CanvasStitchingRecipe recipe) {
            buffer.writeVarInt(recipe.shapes.length);
            for (int[] shape : recipe.shapes) {
                if (shape.length != 2) {
                    throw new IllegalArgumentException("Shape must be an array of two integers: [width, height]");
                }
                buffer.writeVarInt(shape[0]);
                buffer.writeVarInt(shape[1]);
            }
        }
    }
}