package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTrackerProvider;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistryProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterCapabilities
{
    private static final ResourceLocation CANVAS_TRACKER_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "canvas_tracker_capability");
    private static final ResourceLocation PAINTING_REGISTRY_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "painting_registry_capability");

    public static Capability<CanvasTracker> CANVAS_TRACKER = CapabilityManager.get(new CapabilityToken<>(){});
    public static Capability<PaintingRegistry> PAINTING_REGISTRY = CapabilityManager.get(new CapabilityToken<>(){});

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<Level> event) {
        Level world = event.getObject();

        // For client, it doesn't matter which world we're attaching to.
        // For server, it's always saved with overworld.
        if (world.isClientSide() || world.dimension() == Level.OVERWORLD) {
            event.addCapability(CANVAS_TRACKER_CAPABILITY_LOCATION, new CanvasTrackerProvider(world));

            if (!world.isClientSide()) {
                event.addCapability(PAINTING_REGISTRY_CAPABILITY_LOCATION, new PaintingRegistryProvider(world));
            }
        }
    }

    @SubscribeEvent
    public static void registerCapabilityHandler(RegisterCapabilitiesEvent event) {
        event.register(CanvasTracker.class);
        event.register(PaintingRegistry.class);
    }
}