package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTrackerProvider;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistryProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterCapabilities
{
    private static final ResourceLocation CANVAS_TRACKER_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "canvas_tracker_capability");
    private static final ResourceLocation PAINTING_REGISTRY_CAPABILITY_LOCATION = new ResourceLocation(Zetter.MOD_ID, "painting_registry_capability");

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();

        // For client, it doesn't matter which world we're attaching to.
        // For server, it's always saved with overworld.
        if (world.isClientSide() || world.dimension() == World.OVERWORLD) {
            event.addCapability(CANVAS_TRACKER_CAPABILITY_LOCATION, new CanvasTrackerProvider(world));

            if (!world.isClientSide()) {
                event.addCapability(PAINTING_REGISTRY_CAPABILITY_LOCATION, new PaintingRegistryProvider(world));
            }
        }
    }
}