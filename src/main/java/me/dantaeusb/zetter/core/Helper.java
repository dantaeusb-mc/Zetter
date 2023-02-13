package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTrackerCapability;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistryCapability;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

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

    public static CanvasTracker getLevelCanvasTracker(World level) {
        CanvasTracker canvasTracker;

        if (!level.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            canvasTracker = level.getServer().overworld().getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = level.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        }

        return canvasTracker;
    }

    public static PaintingRegistry getLevelPaintingRegistry(World world) {
        PaintingRegistry paintingRegistry;

        if (!world.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            paintingRegistry = world.getServer().overworld().getCapability(PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY).orElse(null);
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
    public static @Nullable UUID tryToRestoreAuthorUuid(ServerWorld level, String authorNickname) {
        List<ServerPlayerEntity> playersWithAuthorNickname = level.getPlayers(serverPlayer -> {
            return serverPlayer.getName().getString().equals(authorNickname);
        });

        if (playersWithAuthorNickname.size() == 1) {
            return playersWithAuthorNickname.get(0).getUUID();
        }

        return null;
    }

    /**
     * Saves painting data as a png relative to rootDirectory
     * could be called from server or client
     * Sanitizes name a bit
     *
     * @param canvasCode
     * @param paintingData
     */
    public static void exportPainting(File rootDirectory, String canvasCode, PaintingData paintingData) throws IOException {
        File exportDirectory = new File(rootDirectory, "zetter");

        if (!exportDirectory.exists()) {
            if (!exportDirectory.mkdir()) {
                throw new IOException(new TranslationTextComponent("console.zetter.error.file_write_folder_unable").getString());
            }
        } else if (!exportDirectory.isDirectory()) {
            throw new IOException(new TranslationTextComponent("console.zetter.error.file_write_folder_exists").getString());
        }

        String name = paintingData.getPaintingName().replaceAll("\s", "-");
        name = name.replaceAll("[^a-zA-Z0-9.-]", "");
        if (name.isEmpty()) {
            name = canvasCode;
        }

        int maxLength = Math.min(name.length(), 32);
        name = name.substring(0, maxLength) + "_" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date());

        File exportFile = new File(exportDirectory, name + ".png");

        int width = paintingData.getWidth();
        int height = paintingData.getHeight();

        BufferedImage bufferedImage = new BufferedImage(
            width,
            height,
            TYPE_INT_ARGB
        );

        IntBuffer colorByteBuffer = ByteBuffer.wrap(paintingData.getColorData()).asIntBuffer();
        int[] colorIntArray = new int[colorByteBuffer.remaining()];
        colorByteBuffer.get(colorIntArray);

        bufferedImage.setRGB(
            0, 0, width, height, colorIntArray, 0, width
        );

        try {
            ImageIO.write(bufferedImage, "PNG", exportFile);
        } catch (IOException e) {
            throw new IOException(new TranslationTextComponent("console.zetter.error.file_write_file").getString());
        }
    }

    /**
     * Try to find a painting by it's name using
     * level canvas tracker
     * @param paintingName
     * @param level
     * @return
     */
    public static @Nullable String lookupPaintingCodeByName(String paintingName, World level) {
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

        for (int id = 0; id < canvasTracker.getLastPaintingId() + 1; id++) {
            final String code = PaintingData.getCanvasCode(id);
            PaintingData paintingData = canvasTracker.getCanvasData(code);

            if (paintingData == null || !paintingData.getType().equals(ZetterCanvasTypes.PAINTING)) {
                continue;
            }

            if (paintingData.getPaintingName().equals(paintingName)) {
                return code;
            }
        }

        return null;
    }
}
