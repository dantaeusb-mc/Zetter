package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.item.crafting.FramingRecipe;
import com.dantaeusb.zetter.item.crafting.UnframingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCraftingRecipes
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(FramingRecipe.SERIALIZER);
        event.getRegistry().register(UnframingRecipe.SERIALIZER);
    }
}