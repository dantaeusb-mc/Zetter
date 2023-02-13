package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class PaintingRegistryCapability {
    @CapabilityInject(CanvasTracker.class)
    public static Capability<PaintingRegistry> CAPABILITY_PAINTING_REGISTRY;

    public static void register() {
        CapabilityManager.INSTANCE.register(
            PaintingRegistry.class,
            new PaintingRegistry.PaintingRegistryStorage(),
            PaintingRegistry::new
        );
    }
}