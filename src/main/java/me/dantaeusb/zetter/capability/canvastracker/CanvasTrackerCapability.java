package me.dantaeusb.zetter.capability.canvastracker;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CanvasTrackerCapability {
    @CapabilityInject(CanvasTracker.class)
    public static Capability<CanvasTracker> CAPABILITY_CANVAS_TRACKER;

    public static void register() {
        CapabilityManager.INSTANCE.register(
            CanvasServerTracker.class,
            new CanvasServerTracker.CanvasTrackerStorage(),
            CanvasServerTracker::new
        );
    }
}