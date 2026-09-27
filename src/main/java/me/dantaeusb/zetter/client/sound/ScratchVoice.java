package me.dantaeusb.zetter.client.sound;

/**
 * The sound of something dragged across a board.
 *
 * Does not make a noise and shape it: it scans a surface. The board is a fixed,
 * endless run of catches laid out along the board, and speed decides how fast a
 * hand crosses them.
 *
 * See docs/painting-sound.md
 */
public final class ScratchVoice {
    /**
     * What the chalk is being dragged over.
     *
     * @param catchesPerPixel how crowded the board is, per canvas pixel
     * @param roughness       how uneven they are; 0 is a rasp, 1 a long tail of grit
     * @param crumble         how much of a catch is shed grit rather than a knock
     * @param slipMs          how long one crumbles for; longer is duller
     */
    public record Surface(float catchesPerPixel, float roughness, float crumble, float slipMs) {
    }

    /**
     * The hand pushing the chalk, which is not the mouse driving it.
     *
     * A real feedback rather than a wobble on top: a catch slows the scan, a slower
     * scan meets fewer catches, so it recovers and runs on until the next one. The
     * level swings even with the input speed held constant, which is the point.
     *
     * @param grab     how much a catch holds the hand up, 0 to 1
     * @param grabMs   how long it takes to push through one
     * @param tremor   how unsteady the hand is between catches, as a share of speed
     * @param tremorHz how quickly that unsteadiness wanders
     */
    public record Hand(float grab, float grabMs, float tremor, float tremorHz) {
    }

    /**
     * Where the sound lives. A board falls away above its peak at about six decibels
     * an octave, which is one pole's worth.
     *
     * @param lowHz    bottom of the band, at half power
     * @param highHz   top of the band, at half power
     * @param topPoles how steep the top is, one being gentle and three a cliff
     */
    public record Band(float lowHz, float highHz, int topPoles) {
    }

    /**
     * A fixed lift or cut, the way a band on a graphic equaliser is fixed. The same
     * three numbers an equaliser asks for, so a curve dialled in by ear elsewhere can
     * be typed in here unchanged.
     *
     * @param centreHz where it sits
     * @param q        how sharp it is, low being broad
     * @param gainDb   how far it lifts, or cuts when negative
     */
    public record Shape(float centreHz, float q, float gainDb) {
    }

    /**
     * Flat noise under the band, so its roll off becomes a shelf. Without it the sound
     * is a dome, and a dome reads as round.
     *
     * @param fromHz below this it is kept out of the way of the band proper
     * @param gainDb where the shelf sits against the band's flat part
     */
    public record Air(float fromHz, float gainDb) {
    }

    /**
     * The board itself, as a sheet that rings, at (m/width) squared plus (n/height)
     * squared for every whole number pair.
     *
     * Which of them answer depends on where the board is touched, and that is most of
     * what makes the sound move. Sharpness has to climb with frequency or the high
     * ones smear into their neighbours, since modes are spaced evenly in frequency
     * while a fixed sharpness is not.
     *
     * @param firstHz where the lowest mode sits, which sets the whole series
     * @param aspect  how much wider than tall the board is
     * @param modes   how many to bother with, lowest first; past sixty they measure
     *                identically to sixty
     * @param topHz   and where to stop regardless
     * @param q       how long the lowest ones ring; a mounted board is damped
     * @param qTilt   how much sharper each octave up is, in doublings per octave
     * @param gainDb  how loud the ringing is against the scraping
     * @param tiltDb  how much quieter each octave up the series is
     */
    public record Plate(float firstHz, float aspect, int modes, float topHz, float q, float qTilt, float gainDb, float tiltDb) {
    }

    /**
     * @param surface what is being scratched
     * @param hand    what is doing the scratching
     * @param band    where the sound lives
     * @param shape   fixed lifts and cuts under it
     * @param air     the shelf that keeps the top from falling away for ever
     * @param plate   the board ringing under everything
     * @param gain    levelling, so one voice is not far louder than the next
     */
    public record Settings(Surface surface, Hand hand, Band band, Shape[] shape, Air air, Plate plate, float gain) {
    }

    /**
     * Chalk on slate. Every frequency sits two semitones above where it was fitted to
     * the recording — that board is not this board. The grain is untouched by it:
     * how often the chalk catches is a fact about the surface, not about tuning.
     */
    public static final Settings CHALK = new Settings(
        new Surface(150f, 0.5f, 0.62f, 0.25f),
        new Hand(0.55f, 45f, 0.35f, 6f),
        new Band(832f, 6098f, 1),
        new Shape[]{new Shape(3437f, 1.1f, 3.0f), new Shape(653f, 1.0f, -3.0f)},
        new Air(4890f, -4.0f),
        new Plate(169f, 1.5f, 60, 7858f, 12f, 0.55f, 16f, -3.5f),
        0.153f
    );

    /**
     * How fast the scan follows a change in hand speed, in seconds. Speed arrives
     * twenty times a second and the sound is built sample by sample, so stepping
     * straight to it would put a seam on every tick.
     */
    private static final float SPEED_EASE_SECONDS = 0.02f;

    /**
     * Ticks a second, which is the rate hand speed is quoted in.
     */
    private static final float TICKS_PER_SECOND = 20f;

    /**
     * Filters in a row are each already down a little at the corner, so the run is
     * down that much more. Spreading them puts the run back at half power where it
     * was asked for. One entry per number of poles.
     */
    private static final float[] CASCADE = {1f, 1.5538f, 1.9615f};

    /**
     * Samples between one nudge of the hand's unsteadiness and the next
     */
    private static final int TREMOR_INTERVAL = 64;

    /**
     * Samples between working out which modes the chalk is currently touching
     */
    private static final int COUPLE_INTERVAL = 512;

    /**
     * The narrowest a bell may be set. Below this the damping term alone reaches the
     * stability ceiling and there is nowhere left to put the bell.
     */
    private static final float MIN_Q = 0.55f;

    private final int sampleRate;

    private final float catchesPerSecondPerSpeed;
    private final float roughness;
    private final float crumble;
    private final float slipDecay;

    private final float speedEase;

    private final float grab;
    private final float grabDecay;
    private final float tremor;
    private final float tremorChance;
    private final float tremorEase;

    private final float lowPass;
    private final float highPass;
    private final int topPoles;

    private final float airHighPass;
    private final float airMix;


    private final Resonator[] shapes;

    /**
     * The board's modes, with the two whole numbers that put each of them where it
     * is — kept because those are also what decide how strongly each answers a touch
     * at a given place, and the place moves.
     */
    private Resonator[] plate;
    private int[] plateM;
    private int[] plateN;
    private float[] plateMix;

    private final float gain;

    /**
     * How fast the hand is going, in canvas pixels a tick.
     *
     * Written on the client thread as the player draws and read on the sound engine's
     * thread as samples are made. A stale read costs nothing — it is a number that
     * changes twenty times a second and is smoothed on the way in anyway.
     */
    private volatile float speed;

    /**
     * Where the chalk is on the board, written by the client thread with the speed
     */
    private volatile float contactX = 0.5f;
    private volatile float contactY = 0.5f;

    private int untilCouple;

    /**
     * Where the scan has got to along the board, how far to the next catch, and what
     * is still ringing. Carried between calls, so consecutive buffers join without a
     * seam — there is no such thing as the start of a buffer once this is running.
     */
    private float scanRate;
    private float held;
    private float unsteady;
    private float unsteadyTarget;
    private int untilTremor;
    private int silent;
    private float toNextCatch;
    private float slip;

    private float lowFirst;
    private float lowSecond;
    private float lowThird;
    private float highFirst;
    private float highSecond;
    private float airCarry;


    private long noise;

    /**
     * @param settings   the surface and tool
     * @param sampleRate samples per second the caller is going to ask for
     * @param seed       anything but zero; xorshift stays at zero for ever if given it
     */
    public ScratchVoice(Settings settings, int sampleRate, long seed) {
        this.sampleRate = sampleRate;

        /*
         * Catches are laid along the board, so their rate in time is however many the
         * hand crosses in a second — which is the only place speed enters the whole
         * voice
         */
        this.catchesPerSecondPerSpeed = settings.surface().catchesPerPixel() * TICKS_PER_SECOND;
        this.roughness = Math.max(0f, Math.min(1f, settings.surface().roughness()));
        this.crumble = Math.max(0f, Math.min(1f, settings.surface().crumble()));
        this.slipDecay = (float) Math.exp(-1000d / (Math.max(0.01f, settings.surface().slipMs()) * sampleRate));

        this.speedEase = (float) (1d - Math.exp(-1d / (SPEED_EASE_SECONDS * sampleRate)));

        this.grab = Math.max(0f, Math.min(0.95f, settings.hand().grab()));
        this.grabDecay = (float) Math.exp(-1000d / (Math.max(1f, settings.hand().grabMs()) * sampleRate));
        this.tremor = Math.max(0f, settings.hand().tremor());

        final float tremorRate = (float) sampleRate / TREMOR_INTERVAL;

        this.tremorChance = Math.min(1f, settings.hand().tremorHz() / tremorRate);
        this.tremorEase = (float) (1d - Math.exp(-2d * Math.PI * settings.hand().tremorHz() / tremorRate));

        this.topPoles = Math.max(1, Math.min(3, settings.band().topPoles()));
        this.lowPass = onePole(settings.band().highHz() * CASCADE[this.topPoles - 1], sampleRate);
        this.highPass = onePole(settings.band().lowHz() / CASCADE[1], sampleRate);

        this.airHighPass = onePole(settings.air().fromHz(), sampleRate);
        this.airMix = (float) Math.pow(10d, settings.air().gainDb() / 20d);


        this.shapes = new Resonator[settings.shape().length];

        for (int i = 0; i < this.shapes.length; i++) {
            final Shape shape = settings.shape()[i];

            this.shapes[i] = new Resonator();
            this.shapes[i].damping = damping(shape.q());
            this.shapes[i].step = resonatorStep(shape.centreHz(), sampleRate, this.shapes[i].damping);
            this.shapes[i].mix = mix(shape.gainDb());
        }

        this.buildPlate(settings.plate(), sampleRate);
        this.gain = settings.gain();

        this.noise = seed == 0L ? 1L : seed;
        this.toNextCatch = 1f;
    }

    /**
     * How fast the hand is moving, in canvas pixels a tick. Everything the sound does
     * about speed follows from this and nothing else.
     *
     * @param pixelsPerTick
     */
    public void setSpeed(float pixelsPerTick) {
        this.speed = Math.max(0f, pixelsPerTick);
    }

    /**
     * Where on the board the chalk is, across and down, each from zero to one.
     *
     * Only the modes care, and they care a lot: it decides which of them are being
     * touched at a belly and which at a node.
     *
     * @param x
     * @param y
     */
    public void setContact(float x, float y) {
        this.contactX = Math.max(0f, Math.min(1f, x));
        this.contactY = Math.max(0f, Math.min(1f, y));
    }

    /*
     * The board
     */

    /**
     * One bell. The band output of a state variable filter rings at Q where it is
     * tuned, so bringing it back down by the same Q leaves something worth one where
     * it sits and nothing anywhere else — added it lifts a band, subtracted it scoops
     * one out.
     */
    private static final class Resonator {
        private float step;
        private float damping;
        private float mix;

        private float low;
        private float band;

        private float next(float input) {
            this.low += this.step * this.band;

            final float high = input - this.low - this.damping * this.band;

            this.band += this.step * high;

            return this.band * this.damping * this.mix;
        }
    }

    /**
     * Builds the board's modes, lowest first. Worked out in whole numbers and only
     * then scaled, so the lowest lands where it was asked for and the rest follow
     * from the shape of the board.
     *
     * @param settings
     * @param sampleRate
     */
    private void buildPlate(Plate settings, int sampleRate) {
        final int reach = Math.max(4, (int) Math.sqrt(settings.modes()) + 4);
        final java.util.List<int[]> pairs = new java.util.ArrayList<>();

        for (int m = 1; m <= reach; m++) {
            for (int n = 1; n <= reach; n++) {
                pairs.add(new int[]{m, n});
            }
        }

        pairs.sort((left, right) -> Double.compare(value(left, settings.aspect()), value(right, settings.aspect())));

        final int count = Math.min(settings.modes(), pairs.size());
        final double first = value(pairs.get(0), settings.aspect());

        final java.util.List<Resonator> modes = new java.util.ArrayList<>(count);
        final java.util.List<int[]> kept = new java.util.ArrayList<>(count);
        final java.util.List<Float> mixes = new java.util.ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final int[] pair = pairs.get(i);
            final float hz = (float) (settings.firstHz() * value(pair, settings.aspect()) / first);

            if (hz > settings.topHz()) {
                break;
            }

            final float octaves = (float) (Math.log(hz / settings.firstHz()) / Math.log(2d));
            final float damping = damping((float) (settings.q() * Math.pow(2d, settings.qTilt() * octaves)));

            // Dropped rather than moved: everything above the ceiling used to come
            // back sitting exactly on it, so sixty modes landed on one note and sang
            final Resonator resonator = new Resonator();

            resonator.damping = damping;
            resonator.step = resonatorStep(hz, sampleRate, damping);

            if (resonator.step < rawStep(hz, sampleRate)) {
                continue;
            }

            // Its own weight, not a share of one: modes never add up at any single
            // frequency, so splitting a level between a hundred silences the lot
            modes.add(resonator);
            kept.add(pair);
            mixes.add((float) Math.pow(10d, (settings.gainDb() + settings.tiltDb() * octaves) / 20d));
        }

        this.plate = modes.toArray(new Resonator[0]);
        this.plateM = new int[modes.size()];
        this.plateN = new int[modes.size()];
        this.plateMix = new float[modes.size()];

        for (int i = 0; i < modes.size(); i++) {
            this.plateM[i] = kept.get(i)[0];
            this.plateN[i] = kept.get(i)[1];
            this.plateMix[i] = mixes.get(i);
        }

        this.couple();
    }

    /**
     * Where a mode falls in the series, in whole numbers, before anything is scaled
     *
     * @param pair
     * @param aspect
     * @return
     */
    private static double value(int[] pair, float aspect) {
        return Math.pow(pair[0] / aspect, 2d) + (double) pair[1] * pair[1];
    }

    /**
     * Sets how strongly each mode answers a touch where the chalk currently is: a
     * mode is deaf at its own nodes, so this is a pair of sines across the board.
     * Worked out a few hundred times a second rather than per sample.
     */
    private void couple() {
        final float x = this.contactX;
        final float y = this.contactY;

        for (int i = 0; i < this.plate.length; i++) {
            final float across = (float) Math.sin(this.plateM[i] * Math.PI * x);
            final float down = (float) Math.sin(this.plateN[i] * Math.PI * y);

            // Doubled, since a pair of sines averages about half and the board should
            // not get quieter for being touched in a normal place
            this.plate[i].mix = this.plateMix[i] * across * down * 2f;
        }
    }

    /*
     * Setting up
     */

    private static float mix(float gainDb) {
        return (float) (Math.pow(10d, gainDb / 20d) - 1d);
    }

    private static float damping(float q) {
        return 1f / Math.max(MIN_Q, q);
    }

    /**
     * The coefficient that puts this filter's corner at the given frequency.
     *
     * Solved for directly, because the two formulas anyone reaches for first are both
     * wrong for this topology — see docs/painting-sound.md.
     *
     * @param frequencyHz
     * @param sampleRate
     * @return
     */
    private static float onePole(float frequencyHz, int sampleRate) {
        final double corner = Math.min(frequencyHz, sampleRate * 0.4999d);
        final double shoulder = 2d - Math.cos(2d * Math.PI * corner / sampleRate);
        final double pole = shoulder - Math.sqrt(shoulder * shoulder - 1d);

        return (float) (1d - pole);
    }

    /**
     * The state variable filter's frequency term, held where the filter stays one.
     *
     * The ceiling is not a constant: it runs away once the term reaches two minus the
     * damping term, so a broad bell has far less headroom than a narrow one. Guarding
     * frequency alone looks like enough and fills the buffer with NaN.
     *
     * @param frequencyHz
     * @param sampleRate
     * @param damping
     * @return
     */
    private static float resonatorStep(float frequencyHz, int sampleRate, float damping) {
        return Math.min(rawStep(frequencyHz, sampleRate), 0.8f * Math.max(0.05f, 2f - damping));
    }

    private static float rawStep(float frequencyHz, int sampleRate) {
        return (float) (2d * Math.sin(Math.PI * Math.min(frequencyHz, sampleRate * 0.49d) / sampleRate));
    }

    /*
     * Running
     */

    /**
     * White noise in [-1, 1), from the top 24 bits of an xorshift — the low bits of
     * one of these are not worth listening to.
     *
     * @return
     */
    private float white() {
        this.noise ^= this.noise << 13;
        this.noise ^= this.noise >>> 7;
        this.noise ^= this.noise << 17;

        return (this.noise >> 40) * (1f / 8388608f);
    }

    /**
     * Fills the whole array with the next stretch of sound, picking up exactly where
     * the last call left off.
     *
     * @param out
     */
    public void generate(float[] out) {
        final float target = this.speed * this.catchesPerSecondPerSpeed / this.sampleRate;

        // Quiet by itself, and once it has rung out there is nothing to compute —
        // which is what lets the sound be left running between strokes
        if (target <= 0f) {
            this.silent += out.length;

            if (this.silent > this.sampleRate / 2) {
                java.util.Arrays.fill(out, 0f);
                return;
            }
        } else {
            this.silent = 0;
        }

        for (int i = 0; i < out.length; i++) {
            if (--this.untilCouple <= 0) {
                this.untilCouple = COUPLE_INTERVAL;
                this.couple();
            }

            this.scanRate += (target * this.hand() - this.scanRate) * this.speedEase;

            out[i] = this.voice(this.catches()) * this.gain;
        }
    }

    /**
     * The next catch, if the hand has reached one. Counted down in board rather than
     * in seconds, so a still hand never reaches the next one.
     *
     * @return
     */
    private float catches() {
        this.slip *= this.slipDecay;

        this.toNextCatch -= this.scanRate;

        if (this.toNextCatch <= 0f) {
            final float size = this.nextSize();

            this.toNextCatch += size;

            // A big catch holds longer and sheds more, but not in proportion
            this.slip = (float) Math.sqrt(size);

            // And holds the hand up while it does, which is most of the unevenness
            this.held = Math.min(1f, this.held + size * 0.5f);

        }

        /*
         * Grit shed, with a knock under it. All knock is a record crackle; all grit
         * is a hiss; chalk is mostly the first with a little of the second.
         */
        return this.slip * (this.crumble * this.white() + (1f - this.crumble));
    }

    /**
     * How much of the speed asked for the hand is actually managing: held up by
     * whatever it last caught on, and never quite steady in between.
     *
     * @return
     */
    private float hand() {
        this.held *= this.grabDecay;

        if (--this.untilTremor <= 0) {
            this.untilTremor = TREMOR_INTERVAL;

            if (this.white() < this.tremorChance * 2f - 1f) {
                this.unsteadyTarget = this.white();
            }

            this.unsteady += this.tremorEase * (this.unsteadyTarget - this.unsteady);
        }

        return Math.max(0.05f, (1f - this.grab * this.held) * (1f + this.tremor * this.unsteady));
    }

    /**
     * How far to the next catch, which is also how big this one is. The mean is held
     * at one whatever the roughness, so changing how uneven the board is does not
     * quietly change how often it is hit.
     *
     * @return
     */
    private float nextSize() {
        final float even = (this.white() + 1f) * 0.5f;
        final float tail = (float) -Math.log(Math.max(1e-4f, even));

        return Math.max(0.02f, 1f - this.roughness + this.roughness * tail);
    }

    /**
     * Everything that shapes what the surface handed over: the band, the standing
     * shapes, the air above them and the board underneath.
     *
     * @param excitation
     * @return
     */
    private float voice(float excitation) {
        this.lowFirst += this.lowPass * (excitation - this.lowFirst);

        float rolled = this.lowFirst;

        if (this.topPoles > 1) {
            this.lowSecond += this.lowPass * (rolled - this.lowSecond);
            rolled = this.lowSecond;
        }

        if (this.topPoles > 2) {
            this.lowThird += this.lowPass * (rolled - this.lowThird);
            rolled = this.lowThird;
        }

        this.highFirst += this.highPass * (rolled - this.highFirst);

        final float once = rolled - this.highFirst;

        this.highSecond += this.highPass * (once - this.highSecond);

        final float band = once - this.highSecond;

        float shaped = band;

        // The standing shapes read the flat band, not each other's output
        for (Resonator resonator : this.shapes) {
            shaped += resonator.next(band);
        }

        this.airCarry += this.airHighPass * (excitation - this.airCarry);

        shaped += (excitation - this.airCarry) * this.airMix;

        for (Resonator mode : this.plate) {
            shaped += mode.next(excitation);
        }

        return shaped;
    }
}
