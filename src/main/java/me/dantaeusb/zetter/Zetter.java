package me.dantaeusb.zetter;

import me.dantaeusb.zetter.core.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Zetter.MOD_ID)
public class Zetter
{
    public static final String MOD_ID = "zetter";
    public static final boolean DEBUG_MODE = false;
    public static final boolean DEBUG_SERVER = false;
    public static final boolean DEBUG_CLIENT = false;

    // get a reference to the event bus for this mod;  Registration events are fired on this bus.
    public static IEventBus MOD_EVENT_BUS;

    // Directly reference a log4j logger.
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static Zetter instance;

    public static boolean quarkEnabled;

    public Zetter() {
        instance = this;

        quarkEnabled = ModList.get().isLoaded("quark");
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ZetterConfig.serverSpec, "zetter-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ZetterConfig.clientSpec, "zetter-client.toml");

        ZetterBlocks.init(MOD_EVENT_BUS);
        ZetterItems.init(MOD_EVENT_BUS);
        ZetterBlockEntities.init(MOD_EVENT_BUS);
        ZetterContainerMenus.init(MOD_EVENT_BUS);
        ZetterEntities.init(MOD_EVENT_BUS);
        ZetterCraftingRecipes.init(MOD_EVENT_BUS);
        ZetterConsoleCommands.init(MOD_EVENT_BUS);

        // Custom types and registries
        ZetterRegistries.init(MOD_EVENT_BUS);
        ZetterCanvasTypes.init(MOD_EVENT_BUS);
    }
}
