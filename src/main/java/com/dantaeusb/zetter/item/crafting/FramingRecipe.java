package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.base.CommonProxy;
import com.dantaeusb.zetter.core.ModCraftingRecipes;
import com.dantaeusb.zetter.core.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * Only for frames, toggle
 */
public class FramingRecipe extends SpecialRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    public static final ResourceLocation TYPE_ID = new ResourceLocation(Zetter.MOD_ID, "framing");

    private static final Ingredient INGREDIENT_FRAME = Ingredient.fromTag(Zetter.FRAMES_TAG);
    private static final Ingredient INGREDIENT_PAINTING = Ingredient.fromItems(ModItems.PAINTING);

    public FramingRecipe(ResourceLocation id) {
        super(id);

        Zetter.LOG.info("Added Recipe " + this.toString());
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     * @todo: Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        boolean hasFrame = false;
        boolean hasPainting = false;

        for(int j = 0; j < craftingInventory.getSizeInventory(); ++j) {
            ItemStack stackInSlot = craftingInventory.getStackInSlot(j);

            if (!stackInSlot.isEmpty()) {
                if (INGREDIENT_PAINTING.test(stackInSlot)) {
                    if (hasPainting) {
                        return false;
                    }

                    hasPainting = true;
                } else if (INGREDIENT_FRAME.test(stackInSlot)) {
                    if (hasFrame) {
                        return false;
                    }

                    hasFrame = true;
                }

            }
        }

        return hasFrame && hasPainting;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory craftingInventory) {
        ItemStack frameStack = null;
        CompoundNBT paintingNbt = null;

        for(int j = 0; j < craftingInventory.getSizeInventory(); ++j) {
            ItemStack stackInSlot = craftingInventory.getStackInSlot(j);

            if (!stackInSlot.isEmpty()) {
                if (INGREDIENT_PAINTING.test(stackInSlot)) {
                    paintingNbt = stackInSlot.getTag();
                } else if (INGREDIENT_FRAME.test(stackInSlot)) {
                    frameStack = stackInSlot;
                }

            }
        }

        if (frameStack == null || paintingNbt == null) {
            return ItemStack.EMPTY;
        }

        ItemStack outStack = frameStack.copy();
        outStack.setTag(paintingNbt);

        return outStack;
    }

    public IRecipeType<?> getType() {
        return ModCraftingRecipes.FRAMING_RECIPE_TYPE;
    }

    /**
     * @todo: Not sure if that's the right thing use CRAFTING_SPECIAL_BOOKCLONING here
     * @return
     */
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canFit(int width, int height) {
        return width >= 2 && height >= 2;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FramingRecipe> {

        Serializer() {
            setRegistryName(new ResourceLocation(Zetter.MOD_ID, "framing"));
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, JsonObject json) {
            return new FramingRecipe(recipeId);
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new FramingRecipe(recipeId);
        }

        @Override
        public void write(PacketBuffer buffer, FramingRecipe recipe) {
            // NO OP
        }
    }
}