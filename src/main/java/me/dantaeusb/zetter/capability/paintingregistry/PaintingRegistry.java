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
    private static final String NBT_TAG_PAINTING_LIST = "PaintingCanvasCodeList";

    private static final String SEPARATOR = new String(new byte[] {0}, StandardCharsets.UTF_8);
    private static final byte BYTE_SEPARATOR = SEPARATOR.getBytes(StandardCharsets.UTF_8)[0];

    private World level;
    private ArrayList<String> paintingCanvasCodeList = new ArrayList<>();

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

    /*
     * Saving data
     */

    public CompoundNBT serializeNBT() {
        CompoundNBT compound = new CompoundNBT();

        StringBuilder canvasCodeListBuilder = new StringBuilder();

        for (String canvasCode : this.paintingCanvasCodeList) {
            canvasCodeListBuilder.append(canvasCode);
            canvasCodeListBuilder.append(SEPARATOR);
        }

        compound.putByteArray(NBT_TAG_PAINTING_LIST, canvasCodeListBuilder.toString().getBytes(StandardCharsets.UTF_8));

        return compound;
    }

    public void deserializeNBT(INBT tag) {
        this.paintingCanvasCodeList = new ArrayList<>();

        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compoundTag = (CompoundNBT) tag;

            if (!compoundTag.contains(NBT_TAG_PAINTING_LIST)) {
                return;
            }

            ByteBuffer canvasCodesBuffer = ByteBuffer.wrap(compoundTag.getByteArray(NBT_TAG_PAINTING_LIST));
            int lastZeroBytePosition = 0;

            while (canvasCodesBuffer.hasRemaining()) {
                if (canvasCodesBuffer.get() != BYTE_SEPARATOR) {
                    continue;
                }

                // Do not get byte[], it'll share the reference to the full array I suppose
                final int lastPos = canvasCodesBuffer.position();
                final int lastLimit = canvasCodesBuffer.limit();

                canvasCodesBuffer.position(lastZeroBytePosition);
                canvasCodesBuffer.limit(canvasCodesBuffer.position() - lastZeroBytePosition - 1);
                ByteBuffer canvasCodeBuffer = canvasCodesBuffer.slice();
                canvasCodesBuffer.position(lastPos);
                canvasCodesBuffer.limit(lastLimit);

                String canvasCode = StandardCharsets.UTF_8.decode(canvasCodeBuffer).toString();

                if (canvasCode.isEmpty() || canvasCode.contains(SEPARATOR)) {
                    Zetter.LOG.warn("Cannot deserialize canvas code from painting registry");
                } else {
                    this.paintingCanvasCodeList.add(canvasCode);
                }

                lastZeroBytePosition = canvasCodesBuffer.position();
            }
        }
    }

    // Convert to/from NBT
    static class PaintingRegistryStorage implements Capability.IStorage<PaintingRegistry> {
        @Override
        public INBT writeNBT(Capability<PaintingRegistry> capability, PaintingRegistry instance, @Nullable Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<PaintingRegistry> capability, PaintingRegistry instance, Direction side, @Nullable INBT nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
