package me.dantaeusb.zetter.item.crafting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Only for frames, toggle
 */
public class UnframingRecipe extends CustomRecipe {
    private final Ingredient inputFrame;

    public UnframingRecipe(Ingredient inputFrame) {
        super(CraftingBookCategory.MISC);
        this.inputFrame = inputFrame;
    }

    @Override
    public String toString() {
        return "UnframingRecipe [inputFrame=" + this.inputFrame + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput craftingInventory, Level world) {
        ItemStack frameStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(stack)) {
                if (!frameStack.isEmpty()) {
                    Zetter.LOG.info("UnframingRecipe: matches failed because multiple frames found");
                    // We already found frame
                    return false;
                }

                frameStack = stack;
            } else {
                Zetter.LOG.info("UnframingRecipe: matches failed because unrelated item found: " + stack.getItem());
                // We have something else in the grid
                return false;
            }
        }

        boolean matched = !frameStack.isEmpty() && PaintingItem.getPaintingCode(frameStack) != null;
        Zetter.LOG.info("UnframingRecipe: matches result: " + matched + " (frame empty: " + frameStack.isEmpty() + ")");
        return matched;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < remainingItems.size(); ++i) {
            ItemStack stackInSlot = inv.getItem(i);

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
    public @NotNull ItemStack assemble(CraftingInput craftingInventory, HolderLookup.Provider registries) {
        ItemStack frameStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (this.inputFrame.test(stack)) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = stack;
            }
        }

        if (!frameStack.isEmpty() && Helper.hasTag(frameStack)) {
            ItemStack outStack = new ItemStack(ZetterItems.PAINTING.get());
            CompoundTag compoundnbt = Helper.getTag(frameStack).copy();
            Helper.setTag(outStack, compoundnbt);
            Zetter.LOG.info("UnframingRecipe: assemble success, output stack: " + outStack);
            return outStack;
        } else {
            Zetter.LOG.info("UnframingRecipe: assemble failed checks (frame empty or has no tag)");
            return ItemStack.EMPTY;
        }
    }

    /**
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

    public static final MapCodec<UnframingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("frame").forGetter(recipe -> recipe.inputFrame)
    ).apply(instance, UnframingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnframingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputFrame,
        UnframingRecipe::new
    );

    public static class Serializer implements RecipeSerializer<UnframingRecipe> {
        @Override
        public MapCodec<UnframingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UnframingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}