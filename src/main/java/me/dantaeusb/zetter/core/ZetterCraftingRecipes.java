package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.crafting.CopyingRecipe;
import me.dantaeusb.zetter.item.crafting.FramingRecipe;
import me.dantaeusb.zetter.item.crafting.PaletteRechargeRecipe;
import me.dantaeusb.zetter.item.crafting.UnframingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZetterCraftingRecipes
{
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Zetter.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FramingRecipe>> FRAMING = RECIPES.register("framing", FramingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UnframingRecipe>> UNFRAMING = RECIPES.register("unframing", UnframingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PaletteRechargeRecipe>> PALETTE_RECHARGE = RECIPES.register("palette_recharge", PaletteRechargeRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CopyingRecipe>> COPYING = RECIPES.register("copying", CopyingRecipe.Serializer::new);

    public static void init(IEventBus bus) {
        RECIPES.register(bus);
    }
}