package com.dantaeusb.zetter.canvastracker;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CanvasTrackerCapability {
    @CapabilityInject(CanvasDefaultTracker.class)
    public static Capability<ICanvasTracker> CAPABILITY_CANVAS_TRACKER;

    public static void register() {
        CapabilityManager.INSTANCE.register(
            CanvasDefaultTracker.class,
            new CanvasDefaultTracker.CanvasTrackerNBTStorage(),
            CanvasDefaultTracker::new
        );
    }
}
