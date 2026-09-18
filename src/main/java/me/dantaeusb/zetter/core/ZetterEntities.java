package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZetterEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Zetter.MOD_ID);

    public static DeferredHolder<EntityType<?>, EntityType<PaintingEntity>> FRAMED_PAINTING_ENTITY = ENTITIES.register("custom_painting_entity", () -> EntityType.Builder.<PaintingEntity>of(PaintingEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .build(Zetter.MOD_ID + "_custom_painting_entity"));
    public static DeferredHolder<EntityType<?>, EntityType<EaselEntity>> EASEL_ENTITY = ENTITIES.register("easel_entity", () -> EntityType.Builder.<EaselEntity>of(EaselEntity::new, MobCategory.MISC)
            .sized(0.8F, 1.8F)
            .build(Zetter.MOD_ID + "_easel_entity"));

    public static void init(IEventBus bus) {
        ENTITIES.register(bus);
    }
}