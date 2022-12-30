package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @todo: [MID] Get rid of this class, all functions can be moved to the classes with execution context
 */
public class Helper {
    public static final int DUMMY_BLACK_COLOR = 0xFF000000;
    public static final int DUMMY_PINK_COLOR = 0xFFFF00FF;
    public static final int CANVAS_COLOR = 0xFFE0DACE;

    public static final String COMBINED_CANVAS_CODE = Zetter.MOD_ID + "_combined_canvas";
    public static final String FALLBACK_CANVAS_CODE = Zetter.MOD_ID + "_fallback_canvas";

    public static final int CANVAS_CODE_MAX_LENGTH = 64;
    public static final int PAINTING_TITLE_MAX_LENGTH = 32;

    /**
     * Basic resolution is a minimal resolution of a painting, 16px
     * Used for GUIs and converting from ENUM resolution (power pf 2)
     * to the numeric resolution (actual pixel count)
     * @return
     */
    public static AbstractCanvasData.Resolution getBasicResolution() {
        return AbstractCanvasData.Resolution.x16;
    }

    public static AbstractCanvasData.Resolution getResolution() {
        return AbstractCanvasData.Resolution.x16;
    }

    public static CanvasTracker getLevelCanvasTracker(Level level) {
        CanvasTracker canvasTracker;

        if (!level.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            canvasTracker = level.getServer().overworld().getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = level.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        }

        return canvasTracker;
    }

    public static PaintingRegistry getLevelPaintingRegistry(Level world) {
        PaintingRegistry paintingRegistry;

        if (!world.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            paintingRegistry = world.getServer().overworld().getCapability(ZetterCapabilities.PAINTING_REGISTRY).orElse(null);
        } else {
            throw new IllegalArgumentException("Painting Registry is not supposed to exist on client");
        }

        return paintingRegistry;
    }

    public static String getFrameKey(PaintingEntity.Materials material, boolean plated) {
        String key = material.toString();

        if (plated) {
            key += "/plated";
        }

        return key;
    }

    /**
     * Before we did not store player UUID data in paintings,
     * to provide compatibility layer we would need to convert
     * player nickname back to uuid, this should work for the most cases.
     * @param level
     * @param authorNickname
     * @return
     */
    public static @Nullable UUID tryToRestoreAuthorUuid(ServerLevel level, String authorNickname) {
        List<ServerPlayer> playersWithAuthorNickname = level.getPlayers(serverPlayer -> {
            return serverPlayer.getName().getString().equals(authorNickname);
        });

        if (playersWithAuthorNickname.size() == 1) {
            return playersWithAuthorNickname.get(0).getUUID();
        }

        return null;
    }
}
