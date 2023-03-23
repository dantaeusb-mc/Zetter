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
    private final Level world;
    private ArrayList<String> paintingCanvasCodeList = new ArrayList<>();

    public PaintingRegistry(Level world) {
        super();

        this.world = world;
    }

    /**
     * World accessor for canvas tracker
     *
     * @return
     */
    public Level getWorld() {
        return this.world;
    }

    public void addPaintingCanvasCode(String canvasCode) {
        this.paintingCanvasCodeList.add(canvasCode);
    }

    public List<String> getPaintingCanvasCodes() {
        return Collections.unmodifiableList(this.paintingCanvasCodeList);
    }
}
