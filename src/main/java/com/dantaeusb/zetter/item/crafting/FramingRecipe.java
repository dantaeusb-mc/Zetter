package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModCraftingRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * Only for frames, toggle
 */
public class FramingRecipe implements ICraftingRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    public static final ResourceLocation TYPE_ID = new ResourceLocation(Zetter.MOD_ID, "framing");

    private final ResourceLocation id;
    private final NonNullList<Ingredient> input;
    private final ItemStack output;

    public FramingRecipe(ResourceLocation id, NonNullList<Ingredient> input, ItemStack output) {
        this.id = id;
        this.input = input;
        this.output = output;

        Zetter.LOG.info("Added Recipe " + this.toString());
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public String toString () {
        return "FramingRecipe [input=" + this.input + ", output=" + this.output + ", id=" + this.id + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     * @todo: Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        int i = 0;

        for(int j = 0; j < craftingInventory.getSizeInventory(); ++j) {
            ItemStack itemstack = craftingInventory.getStackInSlot(j);
            if (!itemstack.isEmpty()) {
                ++i;
                inputs.add(itemstack);
            }
        }

        return i == this.input.size() && net.minecraftforge.common.util.RecipeMatcher.findMatches(inputs,  this.input) != null;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory craftingInventory) {
        return this.output.copy();
    }

    /**
     * Output is variable and depends on exact input (NBT in our case)
     */
    public boolean isDynamic() {
        return true;
    }

    public IRecipeType<?> getType() {
        return ModCraftingRecipes.FRAMING_RECIPE_TYPE;
    }

    @Override
    public ItemStack getRecipeOutput () {
        return this.output;
    }

    /**
     * @todo: Not sure if that's the right thing use CRAFTING_SPECIAL_BOOKCLONING here
     * @return
     */
    public IRecipeSerializer<?> getSerializer() {
        return IRecipeSerializer.CRAFTING_SPECIAL_BOOKCLONING;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FramingRecipe> {

        Serializer() {
            setRegistryName(new ResourceLocation(Zetter.MOD_ID, "framing"));
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, JsonObject json) {
            NonNullList<Ingredient> ingredientsList = readIngredients(JSONUtils.getJsonArray(json, "ingredients"));

            if (ingredientsList.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            }

            ItemStack outStack = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));
            return new FramingRecipe(recipeId, ingredientsList, outStack);
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            // Reads a recipe from a packet buffer. This code is called on the client.
            int i = buffer.readVarInt();
            NonNullList<Ingredient> ingredientsList = NonNullList.withSize(i, Ingredient.EMPTY);

            for(int j = 0; j < ingredientsList.size(); ++j) {
                ingredientsList.set(j, Ingredient.read(buffer));
            }

            ItemStack outStack = buffer.readItemStack();
            return new FramingRecipe(recipeId, ingredientsList, outStack);
        }

        private static NonNullList<Ingredient> readIngredients(JsonArray ingredientArray) {
            NonNullList<Ingredient> ingredientsList = NonNullList.create();

            for(int i = 0; i < ingredientArray.size(); ++i) {
                Ingredient ingredient = Ingredient.deserialize(ingredientArray.get(i));
                if (!ingredient.hasNoMatchingItems()) {
                    ingredientsList.add(ingredient);
                }
            }

            return ingredientsList;
        }

        @Override
        public void write(PacketBuffer buffer, FramingRecipe recipe) {
            buffer.writeVarInt(recipe.input.size());

            for(Ingredient ingredient : recipe.input) {
                ingredient.write(buffer);
            }

            buffer.writeItemStack(recipe.output);
        }
    }
}
