package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCapabilities
{
    private static ResourceLocation CANVAS_TRACKER_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "canvas_tracker_capability");

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();

        // For client, it doesn't matter which world we're attaching to.
        // For server, it's always saved with overworld.
        if (world.isClientSide() || world.dimension() == World.OVERWORLD) {
            event.addCapability(CANVAS_TRACKER_CAPABILITY_LOCATION, new CanvasTrackerProvider(world));
        }
    }
}