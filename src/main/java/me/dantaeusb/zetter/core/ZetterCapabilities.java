package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasDefaultTracker;
import me.dantaeusb.zetter.canvastracker.CanvasTrackerProvider;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
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
    public static Capability<ICanvasTracker> CANVAS_TRACKER = CapabilityManager.get(new CapabilityToken<>(){});

    private static ResourceLocation CANVAS_TRACKER_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "canvas_tracker_capability");

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<Level> event) {
        Level world = event.getObject();

        // For client, it doesn't matter which world we're attaching to.
        // For server, it's always saved with overworld.
        if (world.isClientSide() || world.dimension() == Level.OVERWORLD) {
            event.addCapability(CANVAS_TRACKER_CAPABILITY_LOCATION, new CanvasTrackerProvider(world));
        }
    }

    @SubscribeEvent
    public static void registerCapabilityHandler(RegisterCapabilitiesEvent event) {
        event.register(CanvasDefaultTracker.class);
    }
}