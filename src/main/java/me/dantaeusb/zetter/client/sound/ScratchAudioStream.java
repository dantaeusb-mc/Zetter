package me.dantaeusb.zetter.client.sound;

import net.minecraft.client.sounds.AudioStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A ScratchVoice dressed as something the sound engine will play.
 *
 * Forge asks SoundInstance#getStream for the audio of anything sounds.json marks as
 * streaming, and queues whatever comes back — so the samples are made up as the
 * channel asks for them, with no mixin and no file.
 *
 * See docs/painting-sound.md
 */
@OnlyIn(Dist.CLIENT)
public class ScratchAudioStream implements AudioStream {
    /**
     * The rate the voice was tuned at. Its filters hold their corners at any rate,
     * but there is no reason to run it anywhere else.
     */
    public static final int SAMPLE_RATE = 44100;

    /**
     * Signed, little endian, and one channel — OpenAL will not place a stereo buffer
     * in the world, and a board has a position.
     */
    private final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    /**
     * How much sound to hand over at a time.
     *
     * The engine asks for a second and queues four, which is four seconds decided
     * before any of it is heard — useless for a stream that has to answer the player.
     * Nothing checks the size of what comes back, so a shorter buffer is simply a
     * shorter queue, and the engine tops it up every tick.
     */
    private static final int BUFFER_MILLIS = 35;

    private final ScratchVoice voice;

    private float[] samples = new float[0];

    public ScratchAudioStream(ScratchVoice.Settings settings, long seed) {
        this.voice = new ScratchVoice(settings, SAMPLE_RATE, seed);
    }

    /**
     * How fast the hand is moving, in canvas pixels a tick. The whole of the sound's
     * expression
     *
     * @param pixelsPerTick
     */
    public void setSpeed(float pixelsPerTick) {
        this.voice.setSpeed(pixelsPerTick);
    }

    /**
     * Where on the board the chalk is, across and down, each from zero to one
     *
     * @param x
     * @param y
     */
    public void setContact(float x, float y) {
        this.voice.setContact(x, y);
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    /**
     * The next stretch of sound, as many bytes as were asked for.
     *
     * Returns less than was asked for, deliberately — see BUFFER_MILLIS.
     *
     * @param size
     * @return
     */
    @Override
    public ByteBuffer read(int size) {
        final int wanted = Math.max(1, Math.min(size / 2, SAMPLE_RATE * BUFFER_MILLIS / 1000));

        /*
         * Sized to exactly what was asked for:
         * the voice fills whatever it is given, so a longer array would generate
         * sound that is then thrown away, and the next call would carry on from the
         * wrong place — a seam, every buffer.
         */
        if (this.samples.length != wanted) {
            this.samples = new float[wanted];
        }

        this.voice.generate(this.samples);

        final ByteBuffer buffer = BufferUtils.createByteBuffer(wanted * 2).order(ByteOrder.LITTLE_ENDIAN);

        for (float sample : this.samples) {
            buffer.putShort((short) Math.round(Math.max(-1f, Math.min(1f, sample)) * 32767f));
        }

        buffer.flip();

        return buffer;
    }

    @Override
    public void close() {
    }
}
