package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Loads canvas tracker and painting registry data written by Forge builds of the mod, moving from
 * Forge's "capabilities" SavedData to a NeoForge attachment.
 */
@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ZetterCapabilityMigration {
    private static final String LEGACY_SAVED_DATA_ID = "capabilities";
    private static final String LEGACY_CANVAS_TRACKER_KEY = Zetter.MOD_ID + ":canvas_tracker_capability";
    private static final String LEGACY_PAINTING_REGISTRY_KEY = Zetter.MOD_ID + ":painting_registry_capability";

    @SubscribeEvent
    public static void migrateLegacyCapabilities(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }

        boolean migrateCanvasTracker = !level.hasData(ZetterCapabilities.CANVAS_TRACKER);
        boolean migratePaintingRegistry = !level.hasData(ZetterCapabilities.PAINTING_REGISTRY);

        if (!migrateCanvasTracker && !migratePaintingRegistry) {
            return;
        }

        CompoundTag legacyData = readLegacyData(level);

        if (legacyData == null) {
            return;
        }

        HolderLookup.Provider registries = level.registryAccess();

        if (migrateCanvasTracker && legacyData.contains(LEGACY_CANVAS_TRACKER_KEY)) {
            CanvasTracker canvasTracker = level.getData(ZetterCapabilities.CANVAS_TRACKER);
            canvasTracker.deserializeNBT(registries, legacyData.getCompound(LEGACY_CANVAS_TRACKER_KEY));

            Zetter.LOG.info("Migrated canvas tracker from Forge capability data");
        }

        if (migratePaintingRegistry && legacyData.contains(LEGACY_PAINTING_REGISTRY_KEY)) {
            PaintingRegistry paintingRegistry = level.getData(ZetterCapabilities.PAINTING_REGISTRY);
            paintingRegistry.deserializeNBT(registries, legacyData.getCompound(LEGACY_PAINTING_REGISTRY_KEY));

            Zetter.LOG.info("Migrated painting registry from Forge capability data");
        }
    }

    private static CompoundTag readLegacyData(ServerLevel level) {
        // No data fixer: this compound holds mod data that Mojang's level fixers know nothing about
        SavedData.Factory<LegacyCapabilityData> factory = new SavedData.Factory<>(
                LegacyCapabilityData::new,
                (tag, registries) -> new LegacyCapabilityData(tag)
        );

        LegacyCapabilityData legacyData = level.getDataStorage().get(factory, LEGACY_SAVED_DATA_ID);

        return legacyData == null ? null : legacyData.tag;
    }

    private static class LegacyCapabilityData extends SavedData {
        private final CompoundTag tag;

        private LegacyCapabilityData() {
            this(new CompoundTag());
        }

        private LegacyCapabilityData(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            return tag;
        }

        @Override
        public boolean isDirty() {
            return false;
        }
    }
}
