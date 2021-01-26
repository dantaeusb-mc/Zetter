package com.dantaeusb.immersivemp.locks.capability.canvastracker;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.concurrent.Callable;

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
