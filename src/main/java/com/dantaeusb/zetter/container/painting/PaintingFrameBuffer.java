package com.dantaeusb.zetter.container.painting;

import com.dantaeusb.zetter.Zetter;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @todo: extend PacketBuffer
 * @todo: add entity signature to track where canvas is edited
 */
public class PaintingFrameBuffer {
    // this will allow to use byte time offset in data frame

    public static int FRAME_TIME_LIMIT = 500; // send packet every 0.5 seconds

    private static final int MAX_FRAMES = 10;
    private static final int FRAME_SIZE = 1 + 2 + 2 + 4; // flag, time offset, position, color
    public static final int BUFFER_SIZE = FRAME_SIZE * MAX_FRAMES;

    private final ByteBuffer buffer;

    long frameStartTime;

    public PaintingFrameBuffer(long frameStartTime) {
        this.frameStartTime = frameStartTime;
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    public PaintingFrameBuffer(long frameStartTime, ByteBuffer buffer) {
        this.frameStartTime = frameStartTime;
        this.buffer = buffer;
    }

    public boolean isEmpty() {
        return buffer.remaining() == BUFFER_SIZE;
    }

    public void updateStartFrameTime(long frameStartTime) throws Exception {
        if (!this.isEmpty()) {
            throw new Exception("Cannot update frame start time: PaintingFrameBuffer is not empty!");
        }

        this.frameStartTime = frameStartTime;
    }

    public boolean shouldBeSent(long currentTime) {
        return currentTime >= this.frameStartTime + FRAME_TIME_LIMIT || this.buffer.position() == FRAME_SIZE * MAX_FRAMES;
    }

    public void writeChange(long frameTime, int position, int color) {
        short timeOffset = (short) (frameTime - this.frameStartTime);
        short sPosition = (short) position;

        buffer.put((byte) 0x1);
        buffer.putShort(timeOffset);
        buffer.putShort(sPosition);
        buffer.putInt(color);
    }

    public long getFrameStartTime() {
        return this.frameStartTime;
    }

    /**
     * Prepare the data for sending - flip & lock
     * @return
     */
    public ByteBuffer getBufferData() {
        this.buffer.flip();
        return this.buffer.asReadOnlyBuffer();
    }

    public Vector<PaintingFrame> getFrames(UUID ownerId) {
        Vector<PaintingFrame> list = new Vector<>();
        int lastPosition = this.buffer.position();
        this.buffer.rewind();

        while (this.buffer.hasRemaining()) {
            byte flag = buffer.get();
            short timeOffset = buffer.getShort();
            short position = buffer.getShort();
            int color = buffer.getInt();

            if (flag != 0x1) {
                break;
            }

            long frameTime = this.frameStartTime + timeOffset;
            PaintingFrame frame = new PaintingFrame(frameTime, position, color, ownerId);
            list.add(frame);
        }

        this.buffer.position(lastPosition);

        Zetter.LOG.warn(list);

        return list;
    }
}
