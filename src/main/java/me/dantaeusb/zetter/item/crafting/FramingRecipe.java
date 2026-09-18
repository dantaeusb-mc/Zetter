package me.dantaeusb.zetter.item.crafting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
public class FramingRecipe extends CustomRecipe {
    private final Ingredient inputFrame;
    private final Ingredient inputPainting;

    public FramingRecipe(Ingredient inputFrame, Ingredient inputPainting) {
        super(CraftingBookCategory.MISC);
        this.inputFrame = inputFrame;
        this.inputPainting = inputPainting;
    }

    @Override
    public String toString() {
        return "FramingRecipe [inputFrame=" + this.inputFrame + ", inputPainting=" + this.inputPainting + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput craftingInventory, Level world) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputFrame.test(stack)) {
                if (!frameStack.isEmpty()) {
                    Zetter.LOG.debug("FramingRecipe: matches failed because multiple frames found");
                    return false;
                }

                frameStack = stack;
            } else if (this.inputPainting.test(stack)) {
                if (!paintingStack.isEmpty()) {
                    Zetter.LOG.debug("FramingRecipe: matches failed because multiple paintings found");
                    return false;
                }

                paintingStack = stack;
            } else {
                Zetter.LOG.debug("FramingRecipe: matches failed because unrelated item found: " + stack.getItem());
                return false;
            }
        }

        if (frameStack.isEmpty() || paintingStack.isEmpty()) {
            return false;
        }

        if (!Helper.hasTag(paintingStack)) {
            Zetter.LOG.debug("FramingRecipe: matches failed because painting has no tag");
            return false;
        }

        if (!FrameItem.isEmpty(frameStack)) {
            Zetter.LOG.debug("FramingRecipe: matches failed because frame is not empty");
            return false;
        }

        if (PaintingItem.isEmpty(paintingStack)) {
            Zetter.LOG.debug("FramingRecipe: matches failed because painting is empty");
            return false;
        }

        Zetter.LOG.debug("FramingRecipe: MATCHED SUCCESS!");
        return true;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public @NotNull ItemStack assemble(CraftingInput craftingInventory, HolderLookup.Provider registries) {
        ItemStack frameStack = ItemStack.EMPTY;
        ItemStack paintingStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (this.inputFrame.test(stack)) {
                if (!frameStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                frameStack = stack;
            } else if (this.inputPainting.test(stack)) {
                if (!paintingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paintingStack = stack;
            }
        }

        if (frameStack.isEmpty() || paintingStack.isEmpty()) {
            Zetter.LOG.debug("FramingRecipe: assemble failed: empty inputs");
            return ItemStack.EMPTY;
        }

        if (!Helper.hasTag(paintingStack)) {
            Zetter.LOG.debug("FramingRecipe: assemble failed: painting has no tag");
            return ItemStack.EMPTY;
        }

        if (!FrameItem.isEmpty(frameStack) || PaintingItem.isEmpty(paintingStack)) {
            Zetter.LOG.debug("FramingRecipe: assemble failed: frame not empty or painting empty");
            return ItemStack.EMPTY;
        }

        ItemStack outStack = frameStack.copy();
        outStack.setCount(1);

        CompoundTag compoundTag = Helper.getTag(paintingStack).copy();
        Helper.setTag(outStack, compoundTag);

        Zetter.LOG.debug("FramingRecipe: assemble success, returning output stack: " + outStack);
        return outStack;
    }

    /**
     * @return
     */
    public RecipeSerializer<?> getSerializer() {
        return ZetterCraftingRecipes.FRAMING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public static final MapCodec<FramingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("frame").forGetter(recipe -> recipe.inputFrame),
        Ingredient.CODEC.fieldOf("painting").forGetter(recipe -> recipe.inputPainting)
    ).apply(instance, FramingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FramingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputFrame,
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputPainting,
        FramingRecipe::new
    );

    public static class Serializer implements RecipeSerializer<FramingRecipe> {
        @Override
        public MapCodec<FramingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FramingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}