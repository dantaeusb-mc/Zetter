package me.dantaeusb.zetter;

import me.dantaeusb.zetter.core.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

    public Zetter(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        instance = this;

        quarkEnabled = ModList.get().isLoaded("quark");
        MOD_EVENT_BUS = modEventBus;

        modContainer.registerConfig(ModConfig.Type.SERVER, ZetterConfig.serverSpec, "zetter-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ZetterConfig.clientSpec, "zetter-client.toml");

        ZetterBlocks.init(MOD_EVENT_BUS);
        ZetterItems.init(MOD_EVENT_BUS);
        ZetterBlockEntities.init(MOD_EVENT_BUS);
        ZetterContainerMenus.init(MOD_EVENT_BUS);
        ZetterEntities.init(MOD_EVENT_BUS);
        ZetterCraftingRecipes.init(MOD_EVENT_BUS);
        ZetterConsoleCommands.init(MOD_EVENT_BUS);

        // Custom types and registries
        ZetterCapabilities.init(MOD_EVENT_BUS);
        ZetterRegistries.init(MOD_EVENT_BUS);
        ZetterCanvasTypes.init(MOD_EVENT_BUS);
    }
}
