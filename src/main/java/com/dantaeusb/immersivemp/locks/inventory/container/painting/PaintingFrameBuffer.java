package com.dantaeusb.immersivemp.locks.inventory.container.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.nio.ByteBuffer;
import java.util.*;

public class PaintingFrameBuffer {
    // this will allow to use byte time offset in data frame

    public static int FRAME_TIME_LIMIT = 10; // send packet every 0.5 seconds

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
        return currentTime >= this.frameStartTime + FRAME_TIME_LIMIT || this.buffer.position() == MAX_FRAMES;
    }

    public void writeChange(long frameTime, int position, int color) {
        ImmersiveMp.LOG.info("writing to painting frame buffer");
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

    public ByteBuffer getBufferData() {
        return this.buffer;
    }

    public Vector<PaintingFrame> getFrames(UUID ownerId) {
        Vector<PaintingFrame> list = new Vector<>();
        int lastPosition = this.buffer.position();
        this.buffer.rewind();

        ImmersiveMp.LOG.warn(this.buffer);

        while (this.buffer.hasRemaining()) {
            byte flag = buffer.get();

            if (flag == 0) {
                break;
            }

            short timeOffset = buffer.getShort();
            short position = buffer.getShort();
            int color = buffer.getInt();

            long frameTime = this.frameStartTime + timeOffset;

            list.add(new PaintingFrame(frameTime, position, color, ownerId));
        }

        this.buffer.position(lastPosition);

        return list;
    }
}
