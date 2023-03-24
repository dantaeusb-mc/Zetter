package me.dantaeusb.zetter;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZetterConfig {
    static final ForgeConfigSpec serverSpec;
    public static final ZetterConfig.Server SERVER;

    static {
        Pair<ZetterConfig.Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
            .configure(ZetterConfig.Server::new);
        serverSpec = pair.getRight();
        SERVER = pair.getLeft();
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
}
