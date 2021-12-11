package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.model.EaselModel;
import com.dantaeusb.zetter.client.renderer.entity.CustomPaintingRenderer;
import com.dantaeusb.zetter.client.renderer.entity.EaselRenderer;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities
{
    public static EntityType<CustomPaintingEntity> CUSTOM_PAINTING_ENTITY;
    public static EntityType<EaselEntity> EASEL_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityTypeRegistration(final RegistryEvent.Register<EntityType<?>> event) {
        CUSTOM_PAINTING_ENTITY =
                EntityType.Builder.<CustomPaintingEntity>of(CustomPaintingEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .build(Zetter.MOD_ID + "_custom_painting_entity");
        CUSTOM_PAINTING_ENTITY.setRegistryName(Zetter.MOD_ID, "custom_painting_entity");
        event.getRegistry().register(CUSTOM_PAINTING_ENTITY);

        EASEL_ENTITY =
                EntityType.Builder.<EaselEntity>of(EaselEntity::new, MobCategory.MISC)
                        .sized(0.8F, 2.0F)
                        .build(Zetter.MOD_ID + "_easel_entity");
        EASEL_ENTITY.setRegistryName(Zetter.MOD_ID, "_easel_entity");
        event.getRegistry().register(EASEL_ENTITY);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    @OnlyIn(Dist.CLIENT)
    public static void onModelRegistryEvent(ModelRegistryEvent event) {
        for (ModelResourceLocation modelLocation : CustomPaintingRenderer.FRAME_MODELS.values()) {
            ModelLoader.addSpecialModel(modelLocation);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    @OnlyIn(Dist.CLIENT)
    public static void onEntityRenderersRegistryEvent(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOM_PAINTING_ENTITY, CustomPaintingRenderer::new);
        event.registerEntityRenderer(ModEntities.EASEL_ENTITY, EaselRenderer::new);
    }

    public static ModelLayerLocation EASEL_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "easel"), "body_layer");
    public static ModelLayerLocation PAINTING_PLATE_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "custom_painting"), "plate_layer");

    @SubscribeEvent
    @SuppressWarnings("unused")
    @OnlyIn(Dist.CLIENT)
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(EASEL_BODY_LAYER, EaselModel::createBodyLayer);
        event.registerLayerDefinition(PAINTING_PLATE_LAYER, CustomPaintingRenderer::createPlateLayer);
    }
}