package me.dantaeusb.zetter.item.crafting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Only for frames, toggle
 */
public class UnframingRecipe extends CustomRecipe {
    private final Ingredient inputFrame;

    public UnframingRecipe(ResourceLocation id, Ingredient inputFrame) {
        super(id);

        this.inputFrame = inputFrame;
    }

    @Override
    public String toString () {
        return "UnframingRecipe [inputFrame=" + this.inputFrame + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     * @todo: [LOW] Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingContainer craftingInventory, Level world) {
        ItemStack frameStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(craftingInventory.getItem(i))) {
                if (!frameStack.isEmpty()) {
                    // We already found frame
                    return false;
                }

                frameStack = craftingInventory.getItem(i);
            } else {
                // We have something else in the grid
                return false;
            }
        }

        return !frameStack.isEmpty() && PaintingItem.getPaintingCode(frameStack) != null;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for(int i = 0; i < remainingItems.size(); ++i) {
            ItemStack stackInSlot = inv.getItem(i);

            // @todo: do we need containerItem?
            /*if (stackInSlot.hasContainerItem()) {
                remainingItems.set(i, stackInSlot.getContainerItem());
            } else*/
            if (stackInSlot.getItem() instanceof FrameItem) {
                Item keepItem = stackInSlot.getItem();
                ItemStack keepStack = new ItemStack(keepItem);
                keepStack.setCount(1);
                remainingItems.set(i, keepStack);
                break;
            }
        }

        return remainingItems;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer craftingInventory) {
        ItemStack frameStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (this.inputFrame.test(craftingInventory.getItem(i))) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = craftingInventory.getItem(i);
            }
        }

        if (!frameStack.isEmpty() && frameStack.hasTag()) {
            ItemStack outStack = new ItemStack(ZetterItems.PAINTING.get());
            CompoundTag compoundnbt = frameStack.getTag().copy();
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
    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.UNFRAMING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static class Serializer implements RecipeSerializer<UnframingRecipe> {
        @Override
        public UnframingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputFrameJson = GsonHelper.getAsJsonObject(json, "frame");
            final Ingredient inputFrame = Ingredient.fromJson(inputFrameJson);

            return new UnframingRecipe(recipeId, inputFrame);
        }

        @Override
        public UnframingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient frameIngredient = Ingredient.fromNetwork(buffer);
            return new UnframingRecipe(recipeId, frameIngredient);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, UnframingRecipe recipe) {
            recipe.inputFrame.toNetwork(buffer);
        }
    }
}