package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.item.crafting.FramingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.registry.Registry;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCraftingRecipes
{
    public static final IRecipeType<FramingRecipe> FRAMING_RECIPE_TYPE = new RecipeType<>();

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(FramingRecipe.SERIALIZER);

        Registry.register(Registry.RECIPE_TYPE, FramingRecipe.TYPE_ID, FRAMING_RECIPE_TYPE);
    }

    private static <T extends IForgeRegistryEntry<? extends T>> T name(T entry, String name) {
        return entry.setRegistryName(new ResourceLocation(Zetter.MOD_ID, name));
    }

    private static class RecipeType<T extends IRecipe<?>> implements IRecipeType<T> {
        @Override
        public String toString() {
            return Registry.RECIPE_TYPE.getKey(this).toString();
        }
    }
}