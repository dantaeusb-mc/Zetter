package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
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
                        .sized(0.8F, 1.8F)
                        .build(Zetter.MOD_ID + "_easel_entity");
        EASEL_ENTITY.setRegistryName(Zetter.MOD_ID, "easel_entity");
        event.getRegistry().register(EASEL_ENTITY);
    }
}