package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonObject;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.CanvasCuttingHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
     * A single canvas larger than 1x1, and room in the grid for its parts
     */
    public boolean matches(@NotNull CraftingContainer craftingInventory, @NotNull Level level) {
        final int sourceSlot = CanvasCuttingHelper.findSource(craftingInventory);

        if (sourceSlot == -1) {
            return false;
        }

        final int[] size = CanvasItem.getBlockSize(craftingInventory.getItem(sourceSlot));

        if (size[0] <= 1 && size[1] <= 1) {
            return false;
        }

        return CanvasCuttingHelper.halve(size[0]).length <= craftingInventory.getWidth()
            && CanvasCuttingHelper.halve(size[1]).length <= craftingInventory.getHeight();
    }

    /**
     * A promise of the top-left part: blank, sized as that part, and marked
     * with the source, so it gets no preview. It becomes the actual part once
     * the craft is confirmed, see {@link CanvasCuttingHelper#finishCutting}.
     */
    public @NotNull ItemStack assemble(@NotNull CraftingContainer craftingInventory, @NotNull RegistryAccess registryAccess) {
        final int sourceSlot = CanvasCuttingHelper.findSource(craftingInventory);

        if (sourceSlot == -1) {
            return ItemStack.EMPTY;
        }

        final ItemStack sourceStack = craftingInventory.getItem(sourceSlot);
        final int[] size = CanvasItem.getBlockSize(sourceStack);
        final List<CanvasCuttingHelper.Part> parts = CanvasCuttingHelper.cut(size[0], size[1]);
        final CanvasCuttingHelper.Part topLeft = parts.get(0);

        final ItemStack outCanvas = CanvasItem.createBlank(topLeft.blockWidth(), topLeft.blockHeight());
        outCanvas.getOrCreateTag().put(
            CanvasCuttingHelper.NBT_TAG_CUT_SOURCE,
            CanvasCuttingHelper.createCutSourceTag(sourceStack, parts.size())
        );

        return outCanvas;
    }

    /**
     * Every part but the top-left one goes back to the grid, laid out as in the source.
     * Called by the result slot after the crafting event and before the source is
     * taken from the grid, so this is the last to read it and releases it.
     * <p>
     * On client this only predicts: parts are blank until the server syncs the grid.
     */
    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer craftingInventory) {
        final NonNullList<ItemStack> remainingItems = NonNullList.withSize(craftingInventory.getContainerSize(), ItemStack.EMPTY);
        final int sourceSlot = CanvasCuttingHelper.findSource(craftingInventory);

        if (sourceSlot == -1) {
            return remainingItems;
        }

        final ItemStack sourceStack = craftingInventory.getItem(sourceSlot);
        // Set by the result slot around this call only
        final Player player = ForgeHooks.getCraftingPlayer();

        if (player == null) {
            // No crafting event without a player, so no top-left part: keep the source
            Zetter.LOG.warn("Canvas cut without a crafting player, source is kept");
            remainingItems.set(sourceSlot, sourceStack.copyWithCount(1));
            return remainingItems;
        }

        final int[] size = CanvasItem.getBlockSize(sourceStack);
        final List<CanvasCuttingHelper.Part> parts = CanvasCuttingHelper.cut(size[0], size[1]);
        final CanvasCuttingHelper.Part lastPart = parts.get(parts.size() - 1);
        final int gridWidth = craftingInventory.getWidth();

        final int[] layoutStart = CanvasCuttingHelper.placeInGrid(
            sourceSlot % gridWidth,
            sourceSlot / gridWidth,
            lastPart.column() + 1,
            lastPart.row() + 1,
            gridWidth,
            craftingInventory.getHeight()
        );

        final Level level = player.level();

        for (CanvasCuttingHelper.Part part : parts.subList(1, parts.size())) {
            final int slot = (layoutStart[1] + part.row()) * gridWidth + layoutStart[0] + part.column();

            remainingItems.set(slot, level.isClientSide()
                ? CanvasItem.createBlank(part.blockWidth(), part.blockHeight())
                : CanvasCuttingHelper.createPart(sourceStack, part, level)
            );
        }

        final String sourceCode = CanvasItem.getCanvasCode(sourceStack);

        if (!level.isClientSide() && sourceCode != null && CanvasItem.getCanvasData(sourceStack, level) != null) {
            Helper.getLevelCanvasTracker(level).unregisterCanvasData(sourceCode);
        }

        return remainingItems;
    }

    /**
     * @return
     */
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.CANVAS_CUTTING.get();
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