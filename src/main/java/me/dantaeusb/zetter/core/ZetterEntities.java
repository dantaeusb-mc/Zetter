package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Zetter.MOD_ID);

    public static RegistryObject<EntityType<PaintingEntity>> CUSTOM_PAINTING_ENTITY = ENTITIES.register("custom_painting_entity", () -> EntityType.Builder.<PaintingEntity>of(PaintingEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .build(Zetter.MOD_ID + "_custom_painting_entity"));
    public static RegistryObject<EntityType<EaselEntity>> EASEL_ENTITY = ENTITIES.register("easel_entity", () -> EntityType.Builder.<EaselEntity>of(EaselEntity::new, MobCategory.MISC)
            .sized(0.8F, 1.8F)
            .build(Zetter.MOD_ID + "_easel_entity"));

    public static void init(IEventBus bus) {
        ENTITIES.register(bus);
    }
}