package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * Only for frames, toggle
 */
public class FramingRecipe extends SpecialRecipe {
    private final Ingredient inputFrame;
    private final Ingredient inputPainting;

    public FramingRecipe(ResourceLocation id, Ingredient inputFrame, Ingredient inputPainting) {
        super(id);

        this.inputFrame = inputFrame;
        this.inputPainting = inputPainting;
    }

    @Override
    public String toString () {
        return "FramingRecipe [inputFrame=" + this.inputFrame + ", inputPainting=" + this.inputPainting + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(craftingInventory.getItem(i))) {
                if (!frameStack.isEmpty()) {
                    return false;
                }

                frameStack = craftingInventory.getItem(i);
            } else if (this.inputPainting.test(craftingInventory.getItem(i))) {
                if (!paintingStack.isEmpty()) {
                    return false;
                }

                paintingStack = craftingInventory.getItem(i);
            } else {
                // We have something else in the grid
                return false;
            }
        }

        if (frameStack.isEmpty() || paintingStack.isEmpty()) {
            return false;
        }

        if (!paintingStack.hasTag()) {
            return false;
        }

        if (!FrameItem.isEmpty(frameStack) || PaintingItem.isEmpty(paintingStack)) {
            return false;
        }

        return true;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingInventory craftingInventory) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (this.inputFrame.test(craftingInventory.getItem(i))) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = craftingInventory.getItem(i);
            } else if (this.inputPainting.test(craftingInventory.getItem(i))) {
                if (!paintingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paintingStack = craftingInventory.getItem(i);
            }
        }

        if (frameStack.isEmpty() || paintingStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!paintingStack.hasTag()) {
            return ItemStack.EMPTY;
        }

        if (!FrameItem.isEmpty(frameStack) || PaintingItem.isEmpty(paintingStack)) {
            return ItemStack.EMPTY;
        }

        ItemStack outStack = frameStack.copy();
        outStack.setCount(1);

        CompoundNBT compoundTag = paintingStack.getTag().copy();
        outStack.setTag(compoundTag);

        return outStack;
    }

    /**
     * @return
     */
    public IRecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.FRAMING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FramingRecipe> {
        @Override
        public FramingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputFrameJson = JSONUtils.getAsJsonObject(json, "frame");
            final Ingredient inputFrame = Ingredient.fromJson(inputFrameJson);

            final JsonElement inputPaintingJson = JSONUtils.getAsJsonObject(json, "painting");
            final Ingredient inputPainting = Ingredient.fromJson(inputPaintingJson);

            return new FramingRecipe(recipeId, inputFrame, inputPainting);
        }

        @Override
        public FramingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient frameIngredient = Ingredient.fromNetwork(buffer);
            Ingredient paintingIngredient = Ingredient.fromNetwork(buffer);
            return new FramingRecipe(recipeId, frameIngredient, paintingIngredient);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, FramingRecipe recipe) {
            recipe.inputFrame.toNetwork(buffer);
            recipe.inputPainting.toNetwork(buffer);
        }
    }
}