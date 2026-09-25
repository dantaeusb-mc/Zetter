package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Rubbing a different dye into a stick of chalk.
 *
 * The stick comes out as worn as it went in. Handing back a fresh one would make
 * this a repair rather than a recolor: draw a stick down to nothing, dye it, and it
 * would come back whole for the price of one dye.
 *
 * Chalk is not made from scratch any more — it comes by the boxful — so this is how
 * a color you have becomes a color you want.
 */
public class ChalkRecoloringRecipe extends CustomRecipe {
    private final Ingredient inputChalk;

    public ChalkRecoloringRecipe(ResourceLocation id, Ingredient inputChalk) {
        super(id, CraftingBookCategory.MISC);

        this.inputChalk = inputChalk;
    }

    @Override
    public String toString() {
        return "ChalkRecoloringRecipe [inputChalk=" + this.inputChalk + "]";
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ZetterItems.CHALKS.get(net.minecraft.world.item.DyeColor.WHITE).get());
    }

    public boolean matches(CraftingContainer craftingInventory, Level world) {
        return !this.assemble(craftingInventory, null).isEmpty();
    }

    /**
     * One stick and one dye, and the dye has to be a different color than the stick
     * already is — otherwise this would sit in the recipe book offering to do nothing.
     *
     * @param craftingInventory
     * @param registryAccess
     * @return
     */
    public @NotNull ItemStack assemble(CraftingContainer craftingInventory, @Nullable RegistryAccess registryAccess) {
        ItemStack chalkStack = ItemStack.EMPTY;
        DyeItem dye = null;

        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            final ItemStack stack = craftingInventory.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputChalk.test(stack)) {
                if (!chalkStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                chalkStack = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                if (dye != null || stack.getCount() != 1) {
                    return ItemStack.EMPTY;
                }

                dye = dyeItem;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (chalkStack.isEmpty() || dye == null) {
            return ItemStack.EMPTY;
        }

        final ItemStack outStack = new ItemStack(ZetterItems.CHALKS.get(dye.getDyeColor()).get());

        if (chalkStack.is(outStack.getItem())) {
            return ItemStack.EMPTY;
        }

        // Carries the wear across, along with anything else somebody put on the stick
        if (chalkStack.hasTag()) {
            outStack.setTag(chalkStack.getTag().copy());
        }

        return outStack;
    }

    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.CHALK_RECOLORING.get();
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public static class Serializer implements RecipeSerializer<ChalkRecoloringRecipe> {
        @Override
        public ChalkRecoloringRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new ChalkRecoloringRecipe(
                recipeId,
                Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "chalk"))
            );
        }

        @Override
        public ChalkRecoloringRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new ChalkRecoloringRecipe(recipeId, Ingredient.fromNetwork(buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ChalkRecoloringRecipe recipe) {
            recipe.inputChalk.toNetwork(buffer);
        }
    }
}
