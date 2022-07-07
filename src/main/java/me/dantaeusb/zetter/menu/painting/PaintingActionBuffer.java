package me.dantaeusb.zetter.menu.painting;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Because actions could be repetitive, we don't send only one action,
 * but try to accumulate similar actions made in short amount of time.
 *
 * To do so, we store common information, like tool, parameters and color
 * In one place, and canvas actions (where action was applied) in another.
 *
 * Theoretically, we also can use relative time of "sub-action" but
 * it's complicated and not yet worth it
 */
public class PaintingActionBuffer {
    private static final int MAX_FRAMES = 100;
    private static final long MAX_TIME = 5000L;
    private static final long MAX_INACTIVE_TIME = 1000L;

    public final UUID actionId;
    public final UUID authorId;
    public final String toolCode;

    public final int color;

    public final AbstractToolParameters parameters;
    public final Long startTime;

    /**
     * 1 byte -- meta reserved
     * 2 bytes -- time offset (up to 32s)
     * 4+4 bytes x and y as floats
     * 8 bytes -- extra
     */
    private static final int FRAME_SIZE = 1 + 2 + 8;
    public static final int BUFFER_SIZE = FRAME_SIZE * MAX_FRAMES;

    private ByteBuffer actionBuffer;

    private PaintingAction lastAction;

    private boolean committed = false;

    // Could be false only on client
    private boolean sent = false;

    private boolean canceled = false;

    public PaintingActionBuffer(UUID authorId, String toolCode, int color, AbstractToolParameters parameters) {
        this(authorId, toolCode, color, parameters, System.currentTimeMillis(), ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public PaintingActionBuffer(UUID authorId, String toolCode, int color, AbstractToolParameters parameters, Long startTime, ByteBuffer actionBuffer) {
        this.actionId = UUID.randomUUID(); // is it too much?
        this.authorId = authorId;
        this.toolCode = toolCode;
        this.color = color;
        this.parameters = parameters;
        this.startTime = startTime;

        this.actionBuffer = actionBuffer;
    }

    private PaintingActionBuffer(UUID actionId, UUID authorId, String toolCode, int color, AbstractToolParameters parameters, Long startTime, ByteBuffer actionBuffer, boolean canceled) {
        this.actionId = actionId;
        this.authorId = authorId;
        this.toolCode = toolCode;
        this.color = color;
        this.parameters = parameters;
        this.startTime = startTime;

        // When we create with data, it should be non-editable
        this.actionBuffer = actionBuffer.asReadOnlyBuffer();

        this.committed = true;
        this.sent = true;
        this.canceled = canceled;
    }

    /**
     * Can we just add frame here
     *
     * @param authorId
     * @param toolCode
     * @param parameters
     * @return
     */
    public boolean canContinue(UUID authorId, String toolCode, AbstractToolParameters parameters) {
        return !this.shouldCommit() && this.isActionCompatible(authorId, toolCode, parameters);
    }

    /**
     * Is action we're trying to extend compatible with current action
     * @param authorId
     * @param toolCode
     * @param parameters
     * @return
     */
    public boolean isActionCompatible(UUID authorId, String toolCode, AbstractToolParameters parameters) {
        return  this.authorId == authorId &&
                this.toolCode.equals(toolCode);
    }

    /**
     * Should we stop writing in this action and start new
     * @return
     */
    public boolean shouldCommit() {
        final long currentTime = System.currentTimeMillis();

        // Action started long ago
        if (currentTime - this.startTime > MAX_TIME) {
            return true;
        }

        // Did nothing for a while
        if (this.lastAction != null && (currentTime - (this.startTime + this.lastAction.time)) > MAX_INACTIVE_TIME) {
            return true;
        }

        if (!this.actionBuffer.hasRemaining()) {
            return true;
        }

        return false;
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
        if (this.committed) {
            return false;
        }

        if (this.shouldCommit()) {
            this.commit();
            return false;
        }

        final long currentTime = System.currentTimeMillis();
        final int passedTime = (int) (currentTime - this.startTime);

        final PaintingAction action = new PaintingAction(passedTime, posX, posY);
        PaintingAction.writeToBuffer(action, this.actionBuffer);

        this.lastAction = action;

        return true;
    }

    public Stream<PaintingAction> getActionStream() {
        this.actionBuffer.rewind();

        if (this.actionBuffer.limit() % FRAME_SIZE != 0) {
            throw new IllegalStateException("Incorrect amount of frames in buffer");
        } else if (this.actionBuffer.limit() == 0) {
            throw new IllegalStateException("Incoming action buffer is empty");
        }

        return Stream.generate(() -> PaintingAction.readFromBuffer(this.actionBuffer)).limit(this.actionBuffer.limit() / FRAME_SIZE);
    }

    public void commit() {
        this.sealBuffer();
        this.committed = true;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public void setSent() {
        this.commit();

        this.sent = true;
    }

    public boolean isSent() {
        return this.sent;
    }

    public void undo() {
        this.commit();

        this.canceled = true;
    }

    public void redo() {
        this.commit();

        this.canceled = false;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public @Nullable PaintingAction getLastAction() {
        return this.lastAction;
    }

    /**
     * Prepare the data for sending - flip & lock
     * @return
     */
    public void sealBuffer() {
        this.actionBuffer.flip();
        this.actionBuffer = this.actionBuffer.asReadOnlyBuffer();
    }

    public static class PaintingAction {
        public final byte meta;

        public final int time;
        public final float posX;
        public final float posY;

        public PaintingAction(int time, float posX, float posY) {
            this.meta = (byte) 0x1;

            if (time > 0xFFFF) {
                throw new IllegalStateException("Time offset for action is to big");
            }

            this.time = time;
            this.posX = posX;
            this.posY = posY;
        }

        private PaintingAction(byte meta, int time, float posX, float posY) {
            this.meta = meta;
            this.time = time;
            this.posX = posX;
            this.posY = posY;
        }

        public static void writeToBuffer(PaintingAction action, ByteBuffer buffer) {
            // Meta
            buffer.put((byte) 0x1);

            // Time
            buffer.put((byte) (action.time & 0xFF));
            buffer.put((byte) ((action.time >> 8) & 0xFF));

            // Position
            buffer.putFloat(action.posX);
            buffer.putFloat(action.posY);
        }

        public static PaintingAction readFromBuffer(ByteBuffer buffer) {
            // Meta
            final byte meta = buffer.get();

            // Time
            final int time = buffer.get() << 8 & buffer.get();

            // Position
            final float posX = buffer.getFloat();
            final float posY = buffer.getFloat();

            return new PaintingAction(meta, time, posX, posY);
        }
    }

    public static void writePacketData(PaintingActionBuffer actionBuffer, FriendlyByteBuf buffer) {
        buffer.writeUUID(actionBuffer.actionId);
        buffer.writeUUID(actionBuffer.authorId);
        buffer.writeUtf(actionBuffer.toolCode, 32);
        buffer.writeInt(actionBuffer.color);
        buffer.writeLong(actionBuffer.startTime);
        buffer.writeBoolean(actionBuffer.canceled);
        AbstractToolParameters.writePacketData(actionBuffer.parameters, buffer);

        buffer.writeInt(actionBuffer.actionBuffer.limit());
        buffer.writeBytes(actionBuffer.actionBuffer);
    }

    public static PaintingActionBuffer readPacketData(FriendlyByteBuf buffer) {
        UUID actionId = buffer.readUUID();
        UUID authorId = buffer.readUUID();
        String toolCode = buffer.readUtf(32);
        int color = buffer.readInt();
        Long startTime = buffer.readLong();
        boolean canceled = buffer.readBoolean();
        AbstractToolParameters parameters = AbstractToolParameters.readPacketData(buffer, toolCode);

        int bufferSize = buffer.readInt();
        ByteBuffer actionsBuffer = buffer.readBytes(bufferSize).nioBuffer();

        return new PaintingActionBuffer(
                actionId,
                authorId,
                toolCode,
                color,
                parameters,
                startTime,
                actionsBuffer,
                canceled
        );
    }
}
