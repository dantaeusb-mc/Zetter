package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.model.EaselModel;
import com.dantaeusb.zetter.client.renderer.entity.CustomPaintingRenderer;
import com.dantaeusb.zetter.client.renderer.entity.EaselRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModModels
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onModelRegistryEvent(ModelRegistryEvent event) {
        for (ModelResourceLocation modelLocation : CustomPaintingRenderer.FRAME_MODELS.values()) {
            ForgeModelBakery.addSpecialModel(modelLocation);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityRenderersRegistryEvent(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOM_PAINTING_ENTITY, CustomPaintingRenderer::new);
        event.registerEntityRenderer(ModEntities.EASEL_ENTITY, EaselRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(EaselModel.EASEL_BODY_LAYER, EaselModel::createBodyLayer);
        event.registerLayerDefinition(CustomPaintingRenderer.PAINTING_PLATE_LAYER, CustomPaintingRenderer::createPlateLayer);
    }
}