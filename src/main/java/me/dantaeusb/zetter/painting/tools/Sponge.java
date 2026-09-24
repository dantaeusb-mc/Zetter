package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.SpongeParameters;
import me.dantaeusb.zetter.painting.tools.brush.Capsule;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * Wiping a board down.
 *
 * Chalk is taken off rather than moved about, and how evenly it comes off is what
 * the sponge's wetness decides. A soaked sponge meets the board flat and lifts
 * everything; a dry one only catches in places, leaving chalk alive in lanes that
 * run the way the hand went. Wipe a board enough times from enough directions and
 * it keeps a record of all of them, which is the entire point of the tool.
 *
 * See docs/painting-blending.md
 */
public class Sponge extends AbstractTool<SpongeParameters> {
    /**
     * How much of a pixel's coverage the sponge lifts where it meets the board
     * squarely, dry and soaked.
     */
    private static final float DRY_ERASE = 0.025f;
    private static final float WET_ERASE = 0.2f;

    /**
     * How much of what is behind a pixel gets dragged onto it, dry and soaked.
     */
    private static final float DRY_SMEAR = 0.55f;
    private static final float WET_SMEAR = 0.02f;

    /**
     * How far back along the stroke the sponge reaches for what it drags, in canvas
     * pixels. It normally reaches exactly as far as the hand moved; this only comes
     * in on a fast swipe, where reaching the whole distance would pick a color up in
     * one place and set it down in another rather than smearing it between.
     */
    private static final float MAX_SMEAR = 8f;

    /**
     * How much of the erase each lane across the sponge takes, relative to the rest.
     */
    private static final float[] LANES = {1.0f, 0.35f, 0.85f, 0.15f, 0.7f, 0.5f};

    /**
     * How sharply the sponge stops at its edge.
     */
    private static final float FACE_EDGE = 2.5f;

    private final Component translatableComponent = Component.translatable("container.zetter.painting.tools.sponge");

    public Sponge() {
        super(new ArrayList<>());
    }

    @Override
    public ToolShape getShape(SpongeParameters params) {
        return null;
    }

    @Override
    public Component getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public boolean shouldAddAction(CanvasData canvasData, SpongeParameters params, float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        final int maxActiveDistance = (int) Math.ceil(params.getSize() / 2);

        if (newPosX < -maxActiveDistance || newPosX > canvasData.getWidth() + maxActiveDistance) {
            return false;
        }

        if (newPosY < -maxActiveDistance || newPosY > canvasData.getHeight() + maxActiveDistance) {
            return false;
        }

        if (lastPosX == null || lastPosY == null) {
            return true;
        }

        final float deltaX = newPosX - lastPosX;
        final float deltaY = newPosY - lastPosY;

        return deltaX * deltaX + deltaY * deltaY > 1f;
    }

    @Override
    public int apply(CanvasData canvasData, SpongeParameters params, int color, float posX, float posY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        return this.useSegment(
            canvasData, params,
            lastPosX == null ? posX : lastPosX,
            lastPosY == null ? posY : lastPosY,
            posX, posY
        );
    }

    /**
     * A dab, which has no direction to streak along and so wipes evenly
     */
    @Override
    protected int useTool(CanvasData canvasData, SpongeParameters params, int color, float posX, float posY) {
        return this.useSegment(canvasData, params, posX, posY, posX, posY);
    }

    /**
     * Takes coverage off every pixel the sponge swept over, by how squarely the
     * sponge met it and by which of its lanes caught it.
     *
     * @param canvasData
     * @param params
     * @param fromX
     * @param fromY
     * @param toX
     * @param toY
     * @return
     */
    private int useSegment(CanvasData canvasData, SpongeParameters params, float fromX, float fromY, float toX, float toY) {
        final int width = canvasData.getWidth();
        final int height = canvasData.getHeight();

        final float wetness = params.getWetness();
        final float erase = erase(wetness);
        final float smear = smear(wetness);

        final Capsule capsule = new Capsule(
            fromX, fromY, toX, toY, params.getSize() / 2f, width, height
        );

        /*
         * Where the chalk is dragged from: straight back along the way the hand came.
         * A sponge set down without moving has no direction to drag along, and only
         * lifts.
         */
        final float axisX = toX - fromX;
        final float axisY = toY - fromY;
        final float travelled = (float) Math.sqrt(axisX * axisX + axisY * axisY);

        final float reach = Math.min(travelled, MAX_SMEAR);
        final float reachX = travelled == 0f ? 0f : (axisX / travelled) * reach;
        final float reachY = travelled == 0f ? 0f : (axisY / travelled) * reach;

        /*
         * The sponge reads pixels it is also writing, so it reads them from a copy
         * taken first. Reading the canvas live would let a pixel pick up what the
         * pixel before it had just been given, and every one of those would feed the
         * next: the smear would run the whole width of the sweep, and which way it
         * ran would depend on which way the loop below happened to go.
         *
         * Local, not a field. Tools are single instances shared by the client and the
         * server thread, and this is the only state either of them has.
         */
        final int margin = (int) Math.ceil(MAX_SMEAR) + 1;
        final int readX = Math.max(0, capsule.minX - margin);
        final int readY = Math.max(0, capsule.minY - margin);
        final int readWidth = Math.min(width - 1, capsule.maxX + margin) - readX + 1;
        final int readHeight = Math.min(height - 1, capsule.maxY + margin) - readY + 1;

        final int[] before = new int[readWidth * readHeight];

        for (int y = 0; y < readHeight; y++) {
            for (int x = 0; x < readWidth; x++) {
                before[y * readWidth + x] = canvasData.getColorAt((readY + y) * width + readX + x);
            }
        }

        double totalWork = 0;

        for (int x = capsule.minX; x <= capsule.maxX; x++) {
            for (int y = capsule.minY; y <= capsule.maxY; y++) {
                final float pixelX = x + 0.5f;
                final float pixelY = y + 0.5f;

                final double proximity = capsule.proximity(pixelX, pixelY);

                if (proximity == 0) {
                    continue;
                }

                final int canvasColor = before[(y - readY) * readWidth + (x - readX)];
                final int draggedColor = sample(before, readX, readY, readWidth, readHeight, pixelX - reachX, pixelY - reachY);

                // Bare slate with bare slate behind it: nothing to lift and nothing to drag
                if (Color.getAlpha(canvasColor) == 0 && Color.getAlpha(draggedColor) == 0) {
                    continue;
                }

                final double face = Math.min(1d, proximity * FACE_EDGE);
                final float lane = laneWeight(capsule.perpendicular(pixelX, pixelY), wetness);

                final float grip = (float) (face * lane);

                // Drag first, then lift what is left: the sponge picks chalk up before it carries it
                final int smearedColor = Color.lerpPremultiplied(canvasColor, draggedColor, smear * grip);
                final int liftedColor = Color.withAlpha(
                    smearedColor,
                    Math.round(Color.getAlpha(smearedColor) * (1f - erase * grip))
                );

                this.pixelChange(canvasData, params, liftedColor, y * width + x, (float) face);

                totalWork += face * lane;
            }
        }

        /*
         * Water goes where the cleaning goes, and smearing is what a sponge does when
         * it has none: a dry one spends almost nothing pushing chalk around.
         */
        return (int) Math.round(totalWork * erase);
    }

    /**
     * A pixel of the copy taken before this sweep, held inside it. Chalk dragged from
     * off the edge of that copy is chalk from beyond anything the sponge touched, and
     * the nearest edge is the closest thing to it.
     *
     * @param before
     * @param readX
     * @param readY
     * @param readWidth
     * @param readHeight
     * @param posX
     * @param posY
     * @return
     */
    private static int sample(int[] before, int readX, int readY, int readWidth, int readHeight, float posX, float posY) {
        final int x = Math.min(readWidth - 1, Math.max(0, (int) Math.floor(posX) - readX));
        final int y = Math.min(readHeight - 1, Math.max(0, (int) Math.floor(posY) - readY));

        return before[y * readWidth + x];
    }

    /**
     * How hard the sponge bites, from the water left in it.
     *
     * Cubic rather than straight, because a sponge does not get steadily better as it
     * wets — it is either damp enough to lift chalk or it is not.
     *
     * @param wetness
     * @return
     */
    private static float erase(float wetness) {
        return DRY_ERASE + (WET_ERASE - DRY_ERASE) * damp(wetness);
    }

    /**
     * How much it drags instead. The same curve read the other way round, so what the
     * sponge stops lifting it starts pushing.
     *
     * @param wetness
     * @return
     */
    private static float smear(float wetness) {
        return WET_SMEAR + (DRY_SMEAR - WET_SMEAR) * (1f - damp(wetness));
    }

    /**
     * Cubic, because a sponge does not get steadily better as it wets — it is either
     * damp enough to lift chalk or it is not. Most of the range sits near the top,
     * half wet is already most of the way to soaked, and the whole falloff is bunched
     * at the dry end.
     *
     * @param wetness
     * @return
     */
    private static float damp(float wetness) {
        final float dryness = 1f - wetness;

        return 1f - dryness * dryness * dryness;
    }

    /**
     * How much of the erase this pixel's lane takes.
     *
     * The lane comes from how far across the stroke the pixel sits, so the lanes run
     * along the direction of travel and a pixel keeps the same one for as long as the
     * stroke holds its course. A soaked sponge has no lanes at all — it meets the
     * board evenly — so wetness flattens them out.
     *
     * @param perpendicular
     * @param wetness
     * @return
     */
    private static float laneWeight(float perpendicular, float wetness) {
        final float lane = LANES[Math.floorMod((int) Math.floor(perpendicular), LANES.length)];

        return lane + (1f - lane) * wetness;
    }
}
