package com.dantaeusb.immersivemp;

import com.dantaeusb.immersivemp.base.ClientProxy;
import com.dantaeusb.immersivemp.base.CommonProxy;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.core.ModLockBlocks;
import com.dantaeusb.immersivemp.locks.core.ModLockCapabilities;
import com.dantaeusb.immersivemp.locks.core.ModLockGameEvents;
import com.dantaeusb.immersivemp.locks.core.ModLockIMCEvents;
import com.dantaeusb.immersivemp.usefultools.debugging.ForgeLoggerTweaker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ImmersiveMp.MOD_ID)
public class ImmersiveMp
{
    public static final String MOD_ID = "immersivemp";
    public static boolean DEBUG_MODE = false;

    // get a reference to the event bus for this mod;  Registration events are fired on this bus.
    public static IEventBus MOD_EVENT_BUS;

    // Directly reference a log4j logger.
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static ImmersiveMp instance;
    public static CommonProxy proxy;

    public static boolean quarkEnabled;

    public ImmersiveMp() {
        instance = this;

        // Get rid of maybe, doesn't seem to work
        if (DEBUG_MODE) {
            ForgeLoggerTweaker.setMinimumLevel(Level.WARN);
            ForgeLoggerTweaker.applyLoggerFilter();
        }

        quarkEnabled = ModList.get().isLoaded("quark");
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        proxy.start();
    }
}
