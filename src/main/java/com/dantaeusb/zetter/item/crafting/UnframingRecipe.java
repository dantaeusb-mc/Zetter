package com.dantaeusb.zetter.item.crafting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.item.PaintingItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
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
public class UnframingRecipe extends SpecialRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    public static final ResourceLocation TYPE_ID = new ResourceLocation(Zetter.MOD_ID, "unframing");

    private final Ingredient inputFrame;

    public UnframingRecipe(ResourceLocation id, Ingredient inputFrame) {
        super(id);

        this.inputFrame = inputFrame;

        Zetter.LOG.info("Added Recipe " + this.toString());
    }

    @Override
    public String toString () {
        return "UnframingRecipe [inputFrame=" + this.inputFrame + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     * @todo: Maybe we can just extend ShapelessRecipe
     */
    public boolean matches(CraftingInventory craftingInventory, World world) {
        ItemStack frameStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getSizeInventory(); ++i) {
            if (craftingInventory.getStackInSlot(i).isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(craftingInventory.getStackInSlot(i))) {
                if (!frameStack.isEmpty()) {
                    return false;
                }

                frameStack = craftingInventory.getStackInSlot(i);
            }
        }

        return !frameStack.isEmpty() && PaintingItem.getPaintingCode(frameStack) != null;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for(int i = 0; i < remainingItems.size(); ++i) {
            ItemStack stackInSlot = inv.getStackInSlot(i);

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
    public ItemStack getCraftingResult(CraftingInventory craftingInventory) {
        ItemStack frameStack = ItemStack.EMPTY;

        for(int i = 0; i < craftingInventory.getSizeInventory(); ++i) {
            if (this.inputFrame.test(craftingInventory.getStackInSlot(i))) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = craftingInventory.getStackInSlot(i);
            }
        }

        if (!frameStack.isEmpty() && frameStack.hasTag()) {
            ItemStack outStack = new ItemStack(ModItems.PAINTING);
            CompoundNBT compoundnbt = frameStack.getTag().copy();
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

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<UnframingRecipe> {

        Serializer() {
            setRegistryName(new ResourceLocation(Zetter.MOD_ID, "unframing"));
        }

        @Override
        public UnframingRecipe read(ResourceLocation recipeId, JsonObject json) {
            final JsonElement inputFrameJson = JSONUtils.getJsonObject(json, "frame");
            final Ingredient inputFrame = Ingredient.deserialize(inputFrameJson);

            return new UnframingRecipe(recipeId, inputFrame);
        }

        @Override
        public UnframingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient frameIngredient = Ingredient.read(buffer);
            return new UnframingRecipe(recipeId, frameIngredient);
        }

        @Override
        public void write(PacketBuffer buffer, UnframingRecipe recipe) {
            recipe.inputFrame.write(buffer);
        }
    }
}