package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.model.EaselModel;
import me.dantaeusb.zetter.client.renderer.entity.EaselRenderer;
import me.dantaeusb.zetter.client.renderer.entity.FramedPaintingRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZetterModels
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onModelRegistryEvent(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation modelLocation : FramedPaintingRenderer.FRAME_MODELS.values()) {
            event.register(modelLocation);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityRenderersRegistryEvent(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZetterEntities.FRAMED_PAINTING_ENTITY.get(), FramedPaintingRenderer::new);
        event.registerEntityRenderer(ZetterEntities.EASEL_ENTITY.get(), EaselRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(EaselModel.EASEL_BODY_LAYER, EaselModel::createBodyLayer);
        event.registerLayerDefinition(FramedPaintingRenderer.PAINTING_PLATE_LAYER, FramedPaintingRenderer::createPlateLayer);
    }
}