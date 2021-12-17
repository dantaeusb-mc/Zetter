package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.crafting.FramingRecipe;
import me.dantaeusb.zetter.item.crafting.UnframingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterCraftingRecipes
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
        event.getRegistry().register(FramingRecipe.SERIALIZER);
        event.getRegistry().register(UnframingRecipe.SERIALIZER);
    }
}