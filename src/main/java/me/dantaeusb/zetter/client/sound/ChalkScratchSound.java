package me.dantaeusb.zetter.client.sound;

import me.dantaeusb.zetter.core.ZetterSounds;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;

/**
 * The sound of one player dragging chalk across one board.
 * Hands speed and contact point to the voice and otherwise only fades.
 *
 * See docs/painting-sound.md
 */
@OnlyIn(Dist.CLIENT)
public class ChalkScratchSound extends AbstractTickableSoundInstance {
    /**
     * How fast the fade follows the hand. Nearly immediate going up, since the voice
     * itself is silent until the hand moves and there is nothing to ease into; slower
     * coming down, so a tick without movement — a direction change, a dropped frame —
     * does not punch a hole in the sound.
     */
    private static final float ATTACK = 0.9f;
    private static final float RELEASE = 0.25f;

    /**
     * Ticks of no drawing before the sound is let go of entirely. Long on purpose,
     * so the next stroke starts instantly.
     */
    private static final int IDLE_TICKS = 40;

    private final AbstractBoardEntity board;
    private final ScratchAudioStream stream;

    /**
     * Pixels travelled since the last tick, added to by the input handler and taken
     * by the tick.
     */
    private float travelled;

    private int idle;
    private boolean releasing;

    public ChalkScratchSound(AbstractBoardEntity board) {
        super(ZetterSounds.CHALK_SCRATCH.get(), SoundSource.BLOCKS, RandomSource.create());

        this.board = board;
        this.stream = new ScratchAudioStream(ScratchVoice.CHALK, board.getId() * 2654435761L + 1L);

        this.looping = true;
        this.delay = 0;
        this.volume = 0f;

        /*
         * At the board's own position it panned to wherever the entity's middle was,
         * which is not where the chalk is. Drawing means standing in front of it.
         */
        this.relative = true;
        this.attenuation = Attenuation.NONE;

        this.x = 0d;
        this.y = 0d;
        this.z = 0d;

        this.pitch = 1f;

    }

    /**
     * Hands the sound engine samples rather than a file.
     *
     * @param soundBuffers
     * @param sound
     * @param looping
     * @return
     */
    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return CompletableFuture.completedFuture(this.stream);
    }

    /**
     * The hand moved this far since the last time it was told
     *
     * @param pixels
     */
    public void moved(float pixels) {
        this.travelled += pixels;
        this.idle = 0;
        this.releasing = false;
    }

    /**
     * Where on the board the chalk is now, across and down, each from zero to one.
     *
     * @param x
     * @param y
     */
    public void contact(float x, float y) {
        this.stream.setContact(x, y);
    }

    /**
     * Told the player has stopped drawing. The sound is not cut — it eases down over
     * the next few ticks, because chalk lifted mid stroke does not stop dead.
     */
    public void release() {
        this.releasing = true;
    }

    /**
     * Told the player is drawing again on a sound that was let go of but has not yet
     * run out of patience.
     *
     * @return whether this was a fresh start rather than a stroke already running,
     *         which is when the chalk actually touches the board
     */
    public boolean resume() {
        final boolean restarted = this.releasing;

        this.releasing = false;
        this.idle = 0;

        return restarted;
    }

    public AbstractBoardEntity getBoard() {
        return this.board;
    }

    @Override
    public void tick() {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || !this.board.isAlive()) {
            this.stop();
            return;
        }

        final float speed = this.releasing ? 0f : this.travelled;
        this.travelled = 0f;

        if (this.releasing && ++this.idle > IDLE_TICKS && this.volume < 0.02f) {
            this.stop();
            return;
        }

        this.stream.setSpeed(speed);

        final float targetVolume = this.releasing ? 0f : 1f;
        final float ease = targetVolume > this.volume ? ATTACK : RELEASE;

        this.volume += (targetVolume - this.volume) * ease;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
