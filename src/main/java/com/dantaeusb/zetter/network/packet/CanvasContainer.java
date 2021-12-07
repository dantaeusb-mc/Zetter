package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;

public abstract class CanvasContainer {
    private final static AbstractCanvasData.Type[] typeValues = AbstractCanvasData.Type.values();

    public static AbstractCanvasData readPacketCanvasData(FriendlyByteBuf networkBuffer) {
        try {
            int type = networkBuffer.readInt();

            final int resolutionOrdinal = networkBuffer.readInt();
            AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.values()[resolutionOrdinal];

            final int width = networkBuffer.readInt();
            final int height = networkBuffer.readInt();

            final int colorDataSize = networkBuffer.readInt();
            ByteBuffer colorData = networkBuffer.readBytes(colorDataSize).nioBuffer();
            byte[] unwrappedColorData = new byte[width * height * 4];
            colorData.get(unwrappedColorData);

            AbstractCanvasData readCanvasData;

            switch (typeValues[type]) {
                case CANVAS:
                    readCanvasData = CanvasData.createWrap(resolution, width, height, unwrappedColorData);
                    break;
                case PAINTING:
                    readCanvasData = PaintingData.createWrap(resolution, width, height, unwrappedColorData);
                    break;
                default:
                    readCanvasData = DummyCanvasData.createWrap(resolution, width, height, unwrappedColorData);
                    break;
            }

            return readCanvasData;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while extracting canvas from container: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public static void writePacketCanvasData(FriendlyByteBuf networkBuffer, AbstractCanvasData canvasData) {
        networkBuffer.writeInt(canvasData.getType().ordinal());
        networkBuffer.writeInt(canvasData.getResolution().ordinal());
        networkBuffer.writeInt(canvasData.getWidth());
        networkBuffer.writeInt(canvasData.getHeight());
        networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
        networkBuffer.writeBytes(canvasData.getColorDataBuffer());
    }
}
