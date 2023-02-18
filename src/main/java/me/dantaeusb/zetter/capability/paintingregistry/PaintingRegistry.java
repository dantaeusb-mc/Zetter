package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    public static final String SEPARATOR = new String(new byte[] {0}, StandardCharsets.UTF_8);
    public static final byte BYTE_SEPARATOR = SEPARATOR.getBytes(StandardCharsets.UTF_8)[0];

    private World level;
    private List<String> paintingCanvasCodeList = new ArrayList<>();

    public PaintingRegistry() {
        super();
    }

    public void setLevel(World level) {
        this.level = level;
    }

    /**
     * World accessor for canvas tracker
     * @return
     */
    public World getLevel() {
        return this.level;
    }

    public void addPaintingCanvasCode(String canvasCode) {
        this.paintingCanvasCodeList.add(canvasCode);
    }

    public List<String> getPaintingCanvasCodes() {
        return Collections.unmodifiableList(this.paintingCanvasCodeList);
    }

    public void setPaintingCanvasCodes(List<String> paintingCanvasCodeList) {
        this.paintingCanvasCodeList = paintingCanvasCodeList;
    }
}
