package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.base.CommonProxy;
import com.dantaeusb.zetter.core.ModCraftingRecipes;
import com.dantaeusb.zetter.core.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
public class FramingRecipe extends SpecialRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    public static final ResourceLocation TYPE_ID = new ResourceLocation(Zetter.MOD_ID, "framing");

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
     * @todo: Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getSizeInventory(); ++i) {
            if (craftingInventory.getStackInSlot(i).isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(craftingInventory.getStackInSlot(i))) {
                if (!frameStack.isEmpty()) {
                    return false;
                }

                frameStack = craftingInventory.getStackInSlot(i);
            } else if (this.inputPainting.test(craftingInventory.getStackInSlot(i))) {
                if (!paintingStack.isEmpty()) {
                    return false;
                }

                paintingStack = craftingInventory.getStackInSlot(i);
            }
        }

        return !frameStack.isEmpty() && (!paintingStack.isEmpty() && paintingStack.hasTag());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory craftingInventory) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getSizeInventory(); ++i) {
            if (this.inputFrame.test(craftingInventory.getStackInSlot(i))) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = craftingInventory.getStackInSlot(i);
            } else if (this.inputPainting.test(craftingInventory.getStackInSlot(i))) {
                if (!paintingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paintingStack = craftingInventory.getStackInSlot(i);
            }
        }

        if (!paintingStack.isEmpty() && paintingStack.hasTag()) {
            ItemStack outStack = frameStack.copy();
            CompoundNBT compoundnbt = paintingStack.getTag().copy();
            outStack.setTag(compoundnbt);
            return outStack;
        } else {
            return ItemStack.EMPTY;
        }
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
            final JsonElement inputFrameJson = JSONUtils.getJsonObject(json, "frame");
            final Ingredient inputFrame = Ingredient.deserialize(inputFrameJson);

            final JsonElement inputPaintingJson = JSONUtils.getJsonObject(json, "painting");
            final Ingredient inputPainting = Ingredient.deserialize(inputPaintingJson);

            return new FramingRecipe(recipeId, inputFrame, inputPainting);
        }

        @Override
        public FramingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient frameIngredient = Ingredient.read(buffer);
            Ingredient paintingIngredient = Ingredient.read(buffer);
            return new FramingRecipe(recipeId, frameIngredient, paintingIngredient);
        }

        @Override
        public void write(PacketBuffer buffer, FramingRecipe recipe) {
            recipe.inputFrame.write(buffer);
            recipe.inputPainting.write(buffer);
        }
    }
}