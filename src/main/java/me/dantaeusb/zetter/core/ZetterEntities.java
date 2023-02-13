package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.renderer.entity.EaselEntityRenderer;
import me.dantaeusb.zetter.client.renderer.entity.FramedPaintingRenderer;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Zetter.MOD_ID);

    public static RegistryObject<EntityType<PaintingEntity>> FRAMED_PAINTING_ENTITY = ENTITIES.register("custom_painting_entity", () -> EntityType.Builder.<PaintingEntity>of(PaintingEntity::new, EntityClassification.MISC)
            .sized(1.0F, 1.0F)
            .build(Zetter.MOD_ID + "_custom_painting_entity"));
    public static RegistryObject<EntityType<EaselEntity>> EASEL_ENTITY = ENTITIES.register("easel_entity", () -> EntityType.Builder.<EaselEntity>of(EaselEntity::new, EntityClassification.MISC)
            .sized(0.8F, 1.8F)
            .build(Zetter.MOD_ID + "_easel_entity"));

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onModelRegistryEvent(ModelRegistryEvent event) {
        for (ModelResourceLocation modelLocation : FramedPaintingRenderer.FRAME_MODELS.values()) {
            ModelLoader.addSpecialModel(modelLocation);
        }

        RenderingRegistry.registerEntityRenderingHandler(ZetterEntities.FRAMED_PAINTING_ENTITY.get(), FramedPaintingRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ZetterEntities.EASEL_ENTITY.get(), EaselEntityRenderer::new);
    }

    public static void init(IEventBus bus) {
        ENTITIES.register(bus);
    }
}