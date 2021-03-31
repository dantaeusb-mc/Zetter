package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities
{
    public static EntityType<CustomPaintingEntity> CUSTOM_PAINTING_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityTypeRegistration(final RegistryEvent.Register<EntityType<?>> event) {
        CUSTOM_PAINTING_ENTITY =
                EntityType.Builder.<CustomPaintingEntity>create(CustomPaintingEntity::new, EntityClassification.MISC)
                        .build(Zetter.MOD_ID + "_custom_painting_entity");
        CUSTOM_PAINTING_ENTITY.setRegistryName(Zetter.MOD_ID, "custom_painting_entity");
        event.getRegistry().register(CUSTOM_PAINTING_ENTITY);
    }
}