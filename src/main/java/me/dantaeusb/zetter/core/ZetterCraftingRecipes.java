package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.crafting.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterCraftingRecipes
{
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Zetter.MOD_ID);

    public static final RegistryObject<RecipeSerializer<FramingRecipe>> FRAMING = RECIPE_SERIALIZERS.register("framing", FramingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<UnframingRecipe>> UNFRAMING = RECIPE_SERIALIZERS.register("unframing", UnframingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<PaletteRechargeRecipe>> PALETTE_RECHARGE = RECIPE_SERIALIZERS.register("palette_recharge", PaletteRechargeRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CopyingRecipe>> COPYING = RECIPE_SERIALIZERS.register("copying", CopyingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CanvasStitchingRecipe>> CANVAS_STITCHING = RECIPE_SERIALIZERS.register("canvas_stitching", CanvasStitchingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CanvasCuttingRecipe>> CANVAS_CUTTING = RECIPE_SERIALIZERS.register("canvas_cutting", CanvasCuttingRecipe.Serializer::new);

    public static void init(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}