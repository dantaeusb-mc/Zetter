package me.dantaeusb.zetter.server.command;

import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.world.level.Level;

public class PaintingInput {
    private String code;
    private String title;

    private boolean didLookup = false;
    private PaintingData paintingData;

    private PaintingInput(int id) {
        this.code = PaintingData.getCanvasCode(id);
        this.title = null;
    }

    private PaintingInput(String title) {
        this.code = null;
        this.title = title;
    }

    public static PaintingInput fromId(int id) {
        return new PaintingInput(id);
    }

    public static PaintingInput fromCode(String code) {
        int id = Integer.parseInt(code.substring(PaintingData.CODE_PREFIX.length()));
        return new PaintingInput(id);
    }

    public static PaintingInput fromTitle(String title) {
        return new PaintingInput(title);
    }

    public void lookup(Level level) {
        this.didLookup = true;

        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

        if (this.code != null) {
            this.paintingData = canvasTracker.getCanvasData(this.code);
        } else if (this.title != null) {
            for (int id = 0; id < canvasTracker.getLastPaintingId() + 1; id++) {
                final String code = PaintingData.getCanvasCode(id);
                PaintingData paintingData = canvasTracker.getCanvasData(code);

                if (paintingData == null || !paintingData.getType().equals(ZetterCanvasTypes.PAINTING.get())) {
                    continue;
                }

                if (paintingData.getPaintingName().equals(this.title)) {
                    this.code = code;
                    this.paintingData = paintingData;
                    break;
                }
            }
        }
    }

    public boolean hasPaintingData(Level level) {
        if (!this.didLookup) {
            this.lookup(level);
        }

        return this.paintingData != null;
    }

    public String getPaintingCode() {
        return this.code;
    }

    public String getPaintingTitle() {
        return this.title;
    }

    public PaintingData getPaintingData() {
        return this.paintingData;
    }
}
