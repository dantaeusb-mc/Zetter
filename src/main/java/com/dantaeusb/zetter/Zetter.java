package com.dantaeusb.zetter;

import com.dantaeusb.zetter.base.ClientProxy;
import com.dantaeusb.zetter.base.CommonProxy;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Zetter.MOD_ID)
public class Zetter
{
    public static final String MOD_ID = "zetter";
    public static boolean DEBUG_MODE = false;

    // get a reference to the event bus for this mod;  Registration events are fired on this bus.
    public static IEventBus MOD_EVENT_BUS;

    // Directly reference a log4j logger.
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static Zetter instance;
    public static CommonProxy proxy;

    public static boolean quarkEnabled;

    public Zetter() {
        instance = this;

        quarkEnabled = ModList.get().isLoaded("quark");
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        proxy.start();
    }
}
