package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.item.PaintingItem;
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
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * Only for frames, toggle
 */
public class UnframingRecipe extends CustomRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    public static final ResourceLocation TYPE_ID = new ResourceLocation(Zetter.MOD_ID, "unframing");

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
     * @todo: Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingContainer craftingInventory, Level world) {
        ItemStack frameStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getContainerSize(); ++i) {
            if (craftingInventory.getItem(i).isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(craftingInventory.getItem(i))) {
                if (!frameStack.isEmpty()) {
                    return false;
                }

                frameStack = craftingInventory.getItem(i);
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
            ItemStack outStack = new ItemStack(ModItems.PAINTING);
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
        return SERIALIZER;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    private static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<UnframingRecipe> {

        Serializer() {
            setRegistryName(new ResourceLocation(Zetter.MOD_ID, "unframing"));
        }

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