package me.dantaeusb.zetter;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZetterConfig {
    static final ModConfigSpec serverSpec;
    public static final ZetterConfig.Server SERVER;

    static final ModConfigSpec clientSpec;
    public static final ZetterConfig.Client CLIENT;

    static {
        Pair<ZetterConfig.Server, ModConfigSpec> serverPair = new ModConfigSpec.Builder()
            .configure(ZetterConfig.Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();

        Pair<ZetterConfig.Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder()
            .configure(ZetterConfig.Client::new);
        clientSpec = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static class Server {
        public final ModConfigSpec.ConfigValue<String> resolution;

        public Server(ModConfigSpec.Builder builder) {
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
        public final ModConfigSpec.ConfigValue<Boolean> enableHelpButton;

        public Client(ModConfigSpec.Builder builder) {
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
