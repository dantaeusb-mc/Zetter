package me.dantaeusb.zetter.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.ChalkBoxItem;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Packing a box full of chalk of one color.
 *
 * One recipe rather than sixteen: the dye in the grid says which color comes out,
 * the way it does for a firework.
 *
 * The box itself is optional. Without one the materials have to include whatever the
 * box is made of, and a new box comes out; with an empty one on the grid that box is
 * refilled and the wrapping is not needed again. Either way it has to be empty, so
 * nothing is ever wasted filling a box that had no room — a box of mixed colors is
 * built by hand, one stick at a time.
 */
public class ChalkBoxFillingRecipe extends CustomRecipe {
    /**
     * An ingredient and how much of it the grid has to hold, exactly
     */
    public record Material(Ingredient ingredient, int count) {
    }

    private final @Nullable Ingredient inputBox;
    private final List<Material> materials;

    public ChalkBoxFillingRecipe(ResourceLocation id, @Nullable Ingredient inputBox, List<Material> materials) {
        super(id, CraftingBookCategory.MISC);

        this.inputBox = inputBox;
        this.materials = materials;
    }

    @Override
    public String toString() {
        return "ChalkBoxFillingRecipe [inputBox=" + this.inputBox + ", materials=" + this.materials.size() + "]";
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ZetterItems.CHALK_BOX.get());
    }

    public boolean matches(CraftingContainer craftingInventory, Level world) {
        return this.findBox(craftingInventory) != null;
    }

    public @NotNull ItemStack assemble(CraftingContainer craftingInventory, RegistryAccess registryAccess) {
        final ItemStack boxStack = this.findBox(craftingInventory);
        final DyeItem dye = findDye(craftingInventory);

        if (boxStack == null || dye == null) {
            return ItemStack.EMPTY;
        }

        final ItemStack outStack = boxStack.copy();
        outStack.setCount(1);

        for (int i = 0; i < ChalkBoxItem.CAPACITY; i++) {
            ChalkBoxItem.add(outStack, new ItemStack(ZetterItems.CHALKS.get(dye.getDyeColor()).get()));
        }

        return outStack;
    }

    /**
     * The box this craft fills — the empty one on the grid, or a new one when the
     * recipe does not call for one. Null when the grid is not exactly this recipe.
     *
     * @param craftingInventory
     * @return
     */
    private @Nullable ItemStack findBox(CraftingContainer craftingInventory) {
        ItemStack boxStack = ItemStack.EMPTY;
        int dyes = 0;

        final int[] found = new int[this.materials.size()];

        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            final ItemStack stack = craftingInventory.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof DyeItem) {
                dyes += stack.getCount();
                continue;
            }

            if (this.inputBox != null && this.inputBox.test(stack)) {
                // A box with chalk in it would have nowhere to put the rest
                if (!boxStack.isEmpty() || ChalkBoxItem.getCount(stack) > 0) {
                    return null;
                }

                boxStack = stack;
                continue;
            }

            final int material = this.indexOfMaterial(stack);

            // Anything this recipe does not call for, including a box it did not ask for
            if (material < 0) {
                return null;
            }

            found[material] += stack.getCount();
        }

        if (dyes != 1) {
            return null;
        }

        if (this.inputBox != null && boxStack.isEmpty()) {
            return null;
        }

        for (int material = 0; material < this.materials.size(); ++material) {
            if (found[material] != this.materials.get(material).count()) {
                return null;
            }
        }

        return this.inputBox == null ? new ItemStack(ZetterItems.CHALK_BOX.get()) : boxStack;
    }

    private int indexOfMaterial(ItemStack stack) {
        for (int material = 0; material < this.materials.size(); ++material) {
            if (this.materials.get(material).ingredient().test(stack)) {
                return material;
            }
        }

        return -1;
    }

    private static @Nullable DyeItem findDye(CraftingContainer craftingInventory) {
        for (int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).getItem() instanceof DyeItem dye) {
                return dye;
            }
        }

        return null;
    }

    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.CHALK_BOX_FILLING.get();
    }

    public boolean canCraftInDimensions(int width, int height) {
        int slots = 1 + (this.inputBox == null ? 0 : 1);

        for (Material material : this.materials) {
            slots += material.count();
        }

        return width * height >= slots;
    }

    public static class Serializer implements RecipeSerializer<ChalkBoxFillingRecipe> {
        @Override
        public ChalkBoxFillingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final Ingredient box = json.has("box")
                ? Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "box"))
                : null;

            final List<Material> materials = new ArrayList<>();
            final JsonArray entries = GsonHelper.getAsJsonArray(json, "materials");

            for (JsonElement entry : entries) {
                final JsonObject material = GsonHelper.convertToJsonObject(entry, "material");

                materials.add(new Material(
                    Ingredient.fromJson(GsonHelper.getAsJsonObject(material, "ingredient")),
                    GsonHelper.getAsInt(material, "count", 1)
                ));
            }

            return new ChalkBoxFillingRecipe(recipeId, box, materials);
        }

        @Override
        public ChalkBoxFillingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            final Ingredient box = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : null;

            final int count = buffer.readVarInt();
            final List<Material> materials = new ArrayList<>(count);

            for (int i = 0; i < count; ++i) {
                materials.add(new Material(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }

            return new ChalkBoxFillingRecipe(recipeId, box, materials);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ChalkBoxFillingRecipe recipe) {
            buffer.writeBoolean(recipe.inputBox != null);

            if (recipe.inputBox != null) {
                recipe.inputBox.toNetwork(buffer);
            }

            buffer.writeVarInt(recipe.materials.size());

            for (Material material : recipe.materials) {
                material.ingredient().toNetwork(buffer);
                buffer.writeVarInt(material.count());
            }
        }
    }
}
