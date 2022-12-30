package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Only for frames, toggle
 */
public class PaletteRechargeRecipe extends CustomRecipe {
    private final Ingredient inputPalette;
    private final Ingredient inputRecharge;

    public PaletteRechargeRecipe(ResourceLocation id, Ingredient inputPalette, Ingredient inputRecharge) {
        super(id);

        this.inputPalette = inputPalette;
        this.inputRecharge = inputRecharge;
    }

    @Override
    public String toString () {
        return "PaletteRechargeRecipe [inputPalette=" + this.inputPalette + ", inputRecharge=" + this.inputRecharge + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer craftingInventory, Level world) {
        ItemStack paletteStack = ItemStack.EMPTY;
        ItemStack rechargeStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).isEmpty()) {
                continue;
            }

            if (this.inputPalette.test(craftingInventory.getItem(i))) {
                if (!paletteStack.isEmpty()) {
                    return false;
                }

                paletteStack = craftingInventory.getItem(i);
            } else if (this.inputRecharge.test(craftingInventory.getItem(i))) {
                if (!rechargeStack.isEmpty()) {
                    return false;
                }

                rechargeStack = craftingInventory.getItem(i);
            }
        }

        return (!paletteStack.isEmpty() && paletteStack.hasTag() && paletteStack.getDamageValue() > 0) && !rechargeStack.isEmpty();
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer craftingInventory) {
        ItemStack paletteStack = ItemStack.EMPTY;
        ItemStack rechargeStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (this.inputPalette.test(craftingInventory.getItem(i))) {
                if (!paletteStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paletteStack = craftingInventory.getItem(i);
            } else if (this.inputRecharge.test(craftingInventory.getItem(i))) {
                if (!rechargeStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                rechargeStack = craftingInventory.getItem(i);
            }
        }

        if ((!paletteStack.isEmpty() && paletteStack.hasTag() && paletteStack.getDamageValue() > 0) && !rechargeStack.isEmpty()) {
            ItemStack outStack = paletteStack.copy();

            int newDamage = paletteStack.getDamageValue();

            if (rechargeStack.is(ZetterItems.PALETTE.get()) && rechargeStack.getDamageValue() > 0) {
                newDamage -= (rechargeStack.getMaxDamage() - rechargeStack.getDamageValue());
                newDamage = Math.max(newDamage, 0);
            } else {
                newDamage = 0;
            }

            CompoundTag compoundnbt = paletteStack.getTag().copy();
            outStack.setTag(compoundnbt);
            outStack.setDamageValue(newDamage);

            return outStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    /**
     * @todo: Not sure if that's the right thing use CRAFTING_SPECIAL_BOOKCLONING here
     * @return
     */
    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.PALETTE_RECHARGE.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static class Serializer implements RecipeSerializer<PaletteRechargeRecipe> {
        @Override
        public PaletteRechargeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputFrameJson = GsonHelper.getAsJsonObject(json, "palette");
            final Ingredient inputFrame = Ingredient.fromJson(inputFrameJson);

            final JsonElement inputPaintingJson = GsonHelper.getAsJsonObject(json, "recharge");
            final Ingredient inputPainting = Ingredient.fromJson(inputPaintingJson);

            return new PaletteRechargeRecipe(recipeId, inputFrame, inputPainting);
        }

        @Override
        public PaletteRechargeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient frameIngredient = Ingredient.fromNetwork(buffer);
            Ingredient paintingIngredient = Ingredient.fromNetwork(buffer);
            return new PaletteRechargeRecipe(recipeId, frameIngredient, paintingIngredient);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PaletteRechargeRecipe recipe) {
            recipe.inputPalette.toNetwork(buffer);
            recipe.inputRecharge.toNetwork(buffer);
        }
    }
}