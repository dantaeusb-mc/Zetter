package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.crafting.CopyingRecipe;
import me.dantaeusb.zetter.item.crafting.FramingRecipe;
import me.dantaeusb.zetter.item.crafting.PaletteRechargeRecipe;
import me.dantaeusb.zetter.item.crafting.UnframingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ZetterCraftingRecipes
{
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Zetter.MOD_ID);

    public static final RegistryObject<IRecipeSerializer<FramingRecipe>> FRAMING = RECIPES.register("framing", FramingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<UnframingRecipe>> UNFRAMING = RECIPES.register("unframing", UnframingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<PaletteRechargeRecipe>> PALETTE_RECHARGE = RECIPES.register("palette_recharge", PaletteRechargeRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<CopyingRecipe>> COPYING = RECIPES.register("copying", CopyingRecipe.Serializer::new);

    public static void init(IEventBus bus) {
        RECIPES.register(bus);
    }
}