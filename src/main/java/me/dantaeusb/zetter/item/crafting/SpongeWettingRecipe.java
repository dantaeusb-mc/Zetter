package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.SpongeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Wringing water into a sponge.
 */
public class SpongeWettingRecipe extends CustomRecipe {
    private final Ingredient inputSponge;

    public SpongeWettingRecipe(ResourceLocation id, Ingredient inputSponge) {
        super(id, CraftingBookCategory.MISC);

        this.inputSponge = inputSponge;
    }

    @Override
    public String toString() {
        return "SpongeWettingRecipe [inputSponge=" + this.inputSponge + "]";
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ZetterItems.SMALL_SPONGE.get());
    }

    /**
     * Anything that can be poured into a sponge without being used up in some other
     * way. Bottles and buckets differ only in what comes back, see getRemainingItems.
     *
     * @param stack
     * @return
     */
    private static boolean isWaterSource(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return true;
        }

        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER;
    }

    /**
     * One sponge with room for water, one thing to pour, nothing else
     *
     * @param craftingInventory
     * @param world
     * @return
     */
    public boolean matches(CraftingContainer craftingInventory, Level world) {
        ItemStack spongeStack = ItemStack.EMPTY;
        ItemStack waterStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            final ItemStack stack = craftingInventory.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputSponge.test(stack)) {
                if (!spongeStack.isEmpty()) {
                    return false;
                }

                spongeStack = stack;
            } else if (isWaterSource(stack)) {
                if (!waterStack.isEmpty()) {
                    return false;
                }

                waterStack = stack;
            } else {
                return false;
            }
        }

        // A sponge that is already full has nothing to gain, and would craft forever
        return !spongeStack.isEmpty() && !waterStack.isEmpty() && spongeStack.getDamageValue() > 0;
    }

    public @NotNull ItemStack assemble(CraftingContainer craftingInventory, RegistryAccess registryAccess) {
        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            final ItemStack stack = craftingInventory.getItem(i);

            if (!this.inputSponge.test(stack)) {
                continue;
            }

            final ItemStack outStack = stack.copy();
            outStack.setCount(1);
            SpongeItem.soak(outStack);

            return outStack;
        }

        return ItemStack.EMPTY;
    }

    /**
     * What stays on the grid once the sponge is taken. The bucket is handed straight
     * back rather than emptied, which is the whole reason this is a recipe of its own
     * instead of a shapeless one.
     *
     * @param craftingInventory
     * @return
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer craftingInventory) {
        final NonNullList<ItemStack> remainingItems = NonNullList.withSize(craftingInventory.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < remainingItems.size(); ++i) {
            final ItemStack stack = craftingInventory.getItem(i);

            if (stack.is(Items.WATER_BUCKET)) {
                remainingItems.set(i, new ItemStack(Items.WATER_BUCKET));
            } else if (isWaterSource(stack)) {
                remainingItems.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        return remainingItems;
    }

    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.SPONGE_WETTING.get();
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public static class Serializer implements RecipeSerializer<SpongeWettingRecipe> {
        @Override
        public SpongeWettingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new SpongeWettingRecipe(
                recipeId,
                Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "sponge"))
            );
        }

        @Override
        public SpongeWettingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new SpongeWettingRecipe(recipeId, Ingredient.fromNetwork(buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SpongeWettingRecipe recipe) {
            recipe.inputSponge.toNetwork(buffer);
        }
    }
}
