package me.dantaeusb.zetter.capability.paintingregistry;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Painting registry can be used to control
 * and moderate painting that exist on server.
 * It is just a list of strings.
 *
 * @todo: [HIGH] Serializer needs more love, it's poor
 */
public class PaintingRegistry {
    private Level level;
    private ArrayList<String> paintingCanvasCodeList = new ArrayList<>();

    public PaintingRegistry() {
        super();
    }

    public void setLevel(Level level) {
        if (this.level != null) {
            throw new IllegalStateException("Cannot change level for capability");
        }

        this.level = level;
    }

    public Level getLevel() {
        return this.level;
    }

    public void addPaintingCanvasCode(String canvasCode) {
        this.paintingCanvasCodeList.add(canvasCode);
    }

    public List<String> getPaintingCanvasCodes() {
        return Collections.unmodifiableList(this.paintingCanvasCodeList);
    }
}
