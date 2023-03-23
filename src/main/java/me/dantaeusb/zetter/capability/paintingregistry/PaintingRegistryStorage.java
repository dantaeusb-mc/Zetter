package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Painting registry can be used to control
 * and moderate painting that exist on server.
 * It is just a list of strings.
 *
 * @todo: [HIGH] Serializer needs more love, it's poor
 */
public class PaintingRegistryStorage {
    private static final String NBT_TAG_PAINTING_LIST = "PaintingCanvasCodeList";

    private static final String SEPARATOR = new String(new byte[] {0}, StandardCharsets.UTF_8);
    private static final byte BYTE_SEPARATOR = SEPARATOR.getBytes(StandardCharsets.UTF_8)[0];

    public static Tag save(PaintingRegistry paintingRegistry) {
        CompoundTag compound = new CompoundTag();

        StringBuilder canvasCodeListBuilder = new StringBuilder();

        for (String canvasCode : paintingRegistry.getPaintingCanvasCodes()) {
            canvasCodeListBuilder.append(canvasCode);
            canvasCodeListBuilder.append(SEPARATOR);
        }

        compound.putByteArray(NBT_TAG_PAINTING_LIST, canvasCodeListBuilder.toString().getBytes(StandardCharsets.UTF_8));

        return compound;
    }

    public static void load(PaintingRegistry paintingRegistry, Tag tag) {
        if (tag.getType() == CompoundTag.TYPE) {
            CompoundTag compoundTag = (CompoundTag) tag;

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
                ByteBuffer canvasCodeBuffer = canvasCodesBuffer.slice(lastZeroBytePosition, canvasCodesBuffer.position() - lastZeroBytePosition - 1);
                String canvasCode = StandardCharsets.UTF_8.decode(canvasCodeBuffer).toString();

                if (canvasCode.isEmpty() || canvasCode.contains(SEPARATOR)) {
                    Zetter.LOG.warn("Cannot deserialize canvas code from painting registry");
                } else {
                    paintingRegistry.addPaintingCanvasCode(canvasCode);
                }

                lastZeroBytePosition = canvasCodesBuffer.position();
            }
        }
    }
}
