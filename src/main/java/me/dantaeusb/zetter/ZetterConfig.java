package me.dantaeusb.zetter;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZetterConfig {
    static final ForgeConfigSpec serverSpec;
    public static final ZetterConfig.Server SERVER;

    static final ForgeConfigSpec clientSpec;
    public static final ZetterConfig.Client CLIENT;

    static {
        Pair<ZetterConfig.Server, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder()
            .configure(ZetterConfig.Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();

        Pair<ZetterConfig.Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder()
            .configure(ZetterConfig.Client::new);
        clientSpec = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static class Server {
        public final ForgeConfigSpec.ConfigValue<String> resolution;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Painting options");
            builder.push("painting");

            List<String> availableResolution = new ArrayList<>(Arrays.asList("x16", "x32", "x64"));

            this.resolution = builder
                .comment("The size of paintings on that server [x16, x32, 64]")
                .translation("forge.configgui.zetter.painting.resolution")
                .defineInList("resolution", availableResolution.get(0), availableResolution);

            builder.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.ConfigValue<Boolean> enableHelpButton;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("GUI options");
            builder.push("gui");

            this.enableHelpButton = builder
                .comment("Show small help button in the top right corner of the screen that leads to online manual")
                .translation("forge.configgui.zetter.gui.helpButton")
                .define("help_button", true);

            builder.pop();
        }
    }
}
