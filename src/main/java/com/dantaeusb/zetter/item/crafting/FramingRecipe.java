package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
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
    private final Ingredient input;
    private final ItemStack output;

    public FramingRecipe(ResourceLocation id, Ingredient input, ItemStack output) {
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
        return "FrameRecipe [input=" + this.input + ", output=" + this.output + ", id=" + this.id + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        ItemStack lockableStack = ItemStack.EMPTY;
        ItemStack lockStack = ItemStack.EMPTY;

        return !(lockableStack.isEmpty() || lockStack.isEmpty());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory craftingInventory) {
        ItemStack lockableStack = ItemStack.EMPTY;
        ItemStack lockStack = ItemStack.EMPTY;

        for(int j = 0; j < craftingInventory.getSizeInventory(); ++j) {
            ItemStack stackInSlot = craftingInventory.getStackInSlot(j);

            if (!stackInSlot.isEmpty()) {
                if (this.input.test(stackInSlot)) {
                    if (!lockableStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    lockableStack = stackInSlot;
                } else {
                    if (!lockStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    lockStack = stackInSlot;
                }
            }
        }

        if (!lockStack.isEmpty() && lockStack.hasTag()) {
            ItemStack outStack = this.output.copy();
            CompoundNBT compoundnbt = lockStack.getTag().copy();
            outStack.setTag(compoundnbt);
            return outStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Output is variable and depends on exact input (NBT in our case)
     */
    public boolean isDynamic() {
        return true;
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
            // Reads a recipe from json.

            // Reads the input. Accepts items, tags, and anything else that
            // Ingredient.deserialize can understand.
            final JsonElement inputElement = JSONUtils.isJsonArray(json, "ingredients") ? JSONUtils.getJsonArray(json, "ingredients") : JSONUtils.getJsonObject(json, "ingredients");
            final Ingredient input = Ingredient.deserialize(inputElement);

            // Reads the output. The common utility method in ShapedRecipe is what all vanilla
            // recipe classes use for this.
            final ItemStack output = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

            return new FramingRecipe(recipeId, input, output);
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {

            // Reads a recipe from a packet buffer. This code is called on the client.
            final Ingredient input = Ingredient.read(buffer);
            final ItemStack output = buffer.readItemStack();
            final ResourceLocation blockId = buffer.readResourceLocation();

            return new FramingRecipe(recipeId, input, output);
        }

        @Override
        public void write(PacketBuffer buffer, FramingRecipe recipe) {

            // Writes the recipe to a packet buffer. This is called on the server when a player
            // connects or when /reload is used.
            recipe.input.write(buffer);
            buffer.writeItemStack(recipe.output);
        }
    }
}
