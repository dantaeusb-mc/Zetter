package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Painting registry can be used to control
 * and moderate painting that exist on server.
 * It is just a list of strings.
 */
public class PaintingRegistry {
    private static final String NBT_TAG_PAINTING_LIST = "PaintingCanvasCodeList";

    private static final String SEPARATOR = new String(new byte[] {0}, StandardCharsets.UTF_8);
    private static final byte BYTE_SEPARATOR = SEPARATOR.getBytes(StandardCharsets.UTF_8)[0];

    private final Level world;
    private ArrayList<String> paintingCanvasCodeList = new ArrayList<>();

    public PaintingRegistry(Level world) {
        super();

        this.world = world;
    }

    /**
     * World accessor for canvas tracker
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

    /*
     * Saving data
     */

    public Tag serializeNBT() {
        CompoundTag compound = new CompoundTag();

        StringBuilder canvasCodeListBuilder = new StringBuilder();

        for (String canvasCode : this.paintingCanvasCodeList) {
            canvasCodeListBuilder.append(canvasCode);
            canvasCodeListBuilder.append(SEPARATOR);
        }

        compound.putByteArray(NBT_TAG_PAINTING_LIST, canvasCodeListBuilder.toString().getBytes(StandardCharsets.UTF_8));

        return compound;
    }

    public void deserializeNBT(Tag tag) {
        if (tag.getType() == CompoundTag.TYPE) {
            CompoundTag compoundTag = (CompoundTag) tag;

            this.paintingCanvasCodeList = new ArrayList<>();

            if (!compoundTag.contains(NBT_TAG_PAINTING_LIST)) {
                return;
            }

            ByteBuffer canvasCodesBuffer = ByteBuffer.wrap(compoundTag.getByteArray(NBT_TAG_PAINTING_LIST));
            int lastZeroBytePosition = 0;

            do {
                if (canvasCodesBuffer.get() == BYTE_SEPARATOR) {
                    // Do not get byte[], it'll share the reference to the full array I suppose
                    ByteBuffer canvasCodeBuffer = canvasCodesBuffer.slice(lastZeroBytePosition, canvasCodesBuffer.position() - lastZeroBytePosition - 1);
                    String canvasCode = StandardCharsets.UTF_8.decode(canvasCodeBuffer).toString();

                    if (canvasCode.isEmpty() || canvasCode.contains(SEPARATOR)) {
                        Zetter.LOG.warn("Cannot deserialize canvas code from painting registry");
                    } else {
                        this.paintingCanvasCodeList.add(canvasCode);
                    }

                    lastZeroBytePosition = canvasCodesBuffer.position();
                }
            } while (canvasCodesBuffer.hasRemaining());
        }
    }
}
