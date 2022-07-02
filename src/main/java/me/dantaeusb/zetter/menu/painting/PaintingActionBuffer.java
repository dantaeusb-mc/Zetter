package me.dantaeusb.zetter.menu.painting;

import net.minecraft.util.Tuple;

import java.nio.ByteBuffer;
import java.util.Stack;
import java.util.UUID;

public class PaintingAction {
    private static final int MAX_FRAMES = 100;
    private static final int MAX_TIME = 5000;

    private final UUID authorId;
    private final String toolCode;
    private final Long startTime;

    private static final int FRAME_SIZE = 1 + 2 + 4 + 4; // flag, time offset, position, color
    public static final int BUFFER_SIZE = FRAME_SIZE * MAX_FRAMES;

    private final ByteBuffer buffer;

    private boolean committed = false;

    public PaintingAction(UUID authorId, String toolCode, Long startTime) {
        this(authorId, toolCode, startTime, ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public PaintingAction(UUID authorId, String toolCode, Long startTime, ByteBuffer buffer) {
        this.authorId = authorId;
        this.toolCode = toolCode;
        this.startTime = startTime;

        this.buffer = buffer;
    }

    /**
     * Add action coordinates (like path of the brush)
     * Returns true if can add, false if it should be committed
     * and new action stack prepared
     *
     * @param posX
     * @param posY
     * @return
     */
    public boolean addFrame(float posX, float posY) {
        final long currentTime = System.currentTimeMillis();

        // @todo: check that it's not the time to submit action
        this.frames.push(new Tuple<>(posX, posY));

        return true;
    }

    public void pushAction(long frameTime, int position, int color) {
        short timeOffset = (short) (frameTime - this.frameStartTime);

        buffer.put((byte) 0x1);
        buffer.putShort(timeOffset);

        // Can't use short: it will cause potential overflow with x64 resolution and >= 3x3 images!
        buffer.putInt(position);
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

    public void commit() {
        this.committed = true;
    }

    public boolean isCommitted() {
        return this.committed;
    }
}
