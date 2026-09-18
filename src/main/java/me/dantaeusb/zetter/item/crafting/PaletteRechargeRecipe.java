package me.dantaeusb.zetter.item.crafting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCraftingRecipes;
import me.dantaeusb.zetter.core.ZetterItems;
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
public class PaletteRechargeRecipe extends CustomRecipe {
    private final Ingredient inputPalette;
    private final Ingredient inputRecharge;

    public PaletteRechargeRecipe(Ingredient inputPalette, Ingredient inputRecharge) {
        super(CraftingBookCategory.MISC);
        this.inputPalette = inputPalette;
        this.inputRecharge = inputRecharge;
    }

    @Override
    public String toString() {
        return "PaletteRechargeRecipe [inputPalette=" + this.inputPalette + ", inputRecharge=" + this.inputRecharge + "]";
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput craftingInventory, Level world) {
        ItemStack paletteStack = ItemStack.EMPTY;
        ItemStack rechargeStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (this.inputPalette.test(stack)) {
                if (!paletteStack.isEmpty()) {
                    Zetter.LOG.info("PaletteRechargeRecipe: matches failed because multiple palettes found");
                    return false;
                }

                paletteStack = stack;
            } else if (this.inputRecharge.test(stack)) {
                if (!rechargeStack.isEmpty()) {
                    Zetter.LOG.info("PaletteRechargeRecipe: matches failed because multiple recharges found");
                    return false;
                }

                rechargeStack = stack;
            } else {
                Zetter.LOG.info("PaletteRechargeRecipe: matches failed because unrelated item found: " + stack.getItem());
                return false;
            }
        }

        boolean matched = (!paletteStack.isEmpty() && paletteStack.getDamageValue() > 0) && !rechargeStack.isEmpty();
        Zetter.LOG.info("PaletteRechargeRecipe: matches result: " + matched + " (palette empty: " + paletteStack.isEmpty() + ", damage: " + paletteStack.getDamageValue() + ", recharge empty: " + rechargeStack.isEmpty() + ")");
        return matched;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public @NotNull ItemStack assemble(CraftingInput craftingInventory, HolderLookup.Provider registries) {
        ItemStack paletteStack = ItemStack.EMPTY;
        ItemStack rechargeStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); ++i) {
            ItemStack stack = craftingInventory.getItem(i);
            if (this.inputPalette.test(stack)) {
                if (!paletteStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                paletteStack = stack;
            } else if (this.inputRecharge.test(stack)) {
                if (!rechargeStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                rechargeStack = stack;
            }
        }

        if (!paletteStack.isEmpty() && paletteStack.getDamageValue() > 0 && !rechargeStack.isEmpty()) {
            ItemStack outStack = paletteStack.copy();

            int newDamage = paletteStack.getDamageValue();

            if (rechargeStack.is(ZetterItems.PALETTE.get()) && rechargeStack.getDamageValue() > 0) {
                newDamage -= (rechargeStack.getMaxDamage() - rechargeStack.getDamageValue());
                newDamage = Math.max(newDamage, 0);
            } else {
                newDamage = 0;
            }

            if (Helper.hasTag(paletteStack)) {
                CompoundTag compoundnbt = Helper.getTag(paletteStack).copy();
                Helper.setTag(outStack, compoundnbt);
            }
            outStack.setDamageValue(newDamage);

            Zetter.LOG.info("PaletteRechargeRecipe: assemble success, output stack damage: " + newDamage);
            return outStack;
        } else {
            Zetter.LOG.info("PaletteRechargeRecipe: assemble failed checks");
            return ItemStack.EMPTY;
        }
    }

    /**
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

    public static final MapCodec<PaletteRechargeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("palette").forGetter(recipe -> recipe.inputPalette),
        Ingredient.CODEC.fieldOf("recharge").forGetter(recipe -> recipe.inputRecharge)
    ).apply(instance, PaletteRechargeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PaletteRechargeRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputPalette,
        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.inputRecharge,
        PaletteRechargeRecipe::new
    );

    public static class Serializer implements RecipeSerializer<PaletteRechargeRecipe> {
        @Override
        public MapCodec<PaletteRechargeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PaletteRechargeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}