package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.ArtistTableBlock;
import me.dantaeusb.zetter.deprecated.block.EaselBlock;
import me.dantaeusb.zetter.item.crafting.FramingRecipe;
import me.dantaeusb.zetter.item.crafting.UnframingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterCraftingRecipes
{
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Zetter.MOD_ID);

    public static final RegistryObject<RecipeSerializer<FramingRecipe>> FRAMING = RECIPES.register("framing", FramingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<UnframingRecipe>> UNFRAMING = RECIPES.register("unframing", UnframingRecipe.Serializer::new);

    public static void init(IEventBus bus) {
        RECIPES.register(bus);
    }
}