package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
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
public class PaletteRechargeRecipe extends SpecialRecipe {
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
    public boolean matches(CraftingInventory craftingInventory, World world) {
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
    public ItemStack assemble(CraftingInventory craftingInventory) {
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

            CompoundNBT compoundnbt = paletteStack.getTag().copy();
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
    public IRecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.PALETTE_RECHARGE.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<PaletteRechargeRecipe> {
        @Override
        public PaletteRechargeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputFrameJson = JSONUtils.getAsJsonObject(json, "palette");
            final Ingredient inputFrame = Ingredient.fromJson(inputFrameJson);

            final JsonElement inputPaintingJson = JSONUtils.getAsJsonObject(json, "recharge");
            final Ingredient inputPainting = Ingredient.fromJson(inputPaintingJson);

            return new PaletteRechargeRecipe(recipeId, inputFrame, inputPainting);
        }

        @Override
        public PaletteRechargeRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient frameIngredient = Ingredient.fromNetwork(buffer);
            Ingredient paintingIngredient = Ingredient.fromNetwork(buffer);
            return new PaletteRechargeRecipe(recipeId, frameIngredient, paintingIngredient);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, PaletteRechargeRecipe recipe) {
            recipe.inputPalette.toNetwork(buffer);
            recipe.inputRecharge.toNetwork(buffer);
        }
    }
}