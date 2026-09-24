package me.dantaeusb.zetter.painting.tools.brush;

/**
 * The shape a round tool sweeps out travelling from one point to another.
 *
 * Every pixel is measured against the whole segment rather than against a row of
 * separate dabs, so it is touched once and the band comes out even, with no dabs to
 * space out and no beading where they would have overlapped.
 *
 * Kept apart from the tools that use it because the measurement is fiddly, two of
 * them need it, and getting it wrong is invisible until a stroke beads or the
 * smallest brush paints nothing at all.
 */
public class Capsule {
    private final float fromX;
    private final float fromY;

    private final float axisX;
    private final float axisY;
    private final float axisLengthSqr;

    private final float radius;

    /**
     * Pixels that could possibly be touched, held inside the canvas
     */
    public final int minX;
    public final int maxX;
    public final int minY;
    public final int maxY;

    public Capsule(float fromX, float fromY, float toX, float toY, float radius, int canvasWidth, int canvasHeight) {
        this.fromX = fromX;
        this.fromY = fromY;

        this.axisX = toX - fromX;
        this.axisY = toY - fromY;
        this.axisLengthSqr = this.axisX * this.axisX + this.axisY * this.axisY;

        this.radius = radius;

        this.minX = (int) clamp(Math.floor(Math.min(fromX, toX) - radius), 0d, canvasWidth - 1);
        this.maxX = (int) clamp(Math.ceil(Math.max(fromX, toX) + radius), 0d, canvasWidth - 1);
        this.minY = (int) clamp(Math.floor(Math.min(fromY, toY) - radius), 0d, canvasHeight - 1);
        this.maxY = (int) clamp(Math.ceil(Math.max(fromY, toY) + radius), 0d, canvasHeight - 1);
    }

    /**
     * How far inside the capsule a pixel sits: nothing outside it, all of it in the
     * middle. The same measure a single dab uses, only against a line rather than a
     * point.
     *
     * The falloff runs from the middle of the tool to half a pixel past its radius.
     * Measuring it against the diameter, as this did before, left the edge at half
     * weight where the sampled box cut it off, and every dab carried that rim.
     *
     * The half pixel is the pixel itself: distance is measured to its centre, but it
     * covers a square around that centre, so the tool reaches it as soon as it comes
     * within half a pixel of the middle. Without it the smallest brush lands between
     * four pixel centres and paints none of them.
     *
     * Both ends of that range matter. Dividing by the radius alone would spread the
     * falloff over a shorter distance than it actually reaches, which goes unnoticed
     * on a wide tool but saturates a narrow one: at the smallest size the radius is
     * half a pixel, so every pixel it touches would come out at full weight and a
     * brush meant to be finer than a pixel would lay down four solid ones.
     *
     * @param pixelX
     * @param pixelY
     * @return
     */
    public double proximity(float pixelX, float pixelY) {
        final float along = this.along(pixelX, pixelY);

        final double offsetX = pixelX - (this.fromX + this.axisX * along);
        final double offsetY = pixelY - (this.fromY + this.axisY * along);

        final double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);

        return clamp((this.radius + 0.5d - distance) / (this.radius + 0.5d), 0d, 1d);
    }

    /**
     * How far across the direction of travel a pixel sits, signed, in canvas pixels.
     *
     * It is measured from the line the tool travelled along, so for as long as a
     * stroke keeps going the same way a pixel keeps the same value, and anything
     * driven by it runs in lanes parallel to the stroke rather than wandering.
     *
     * Zero for a capsule that goes nowhere, which has no direction to be across.
     *
     * @param pixelX
     * @param pixelY
     * @return
     */
    public float perpendicular(float pixelX, float pixelY) {
        if (this.axisLengthSqr == 0f) {
            return 0f;
        }

        return (float) (
            ((pixelX - this.fromX) * this.axisY - (pixelY - this.fromY) * this.axisX)
                / Math.sqrt(this.axisLengthSqr)
        );
    }

    /**
     * Where the pixel falls along the segment, held between its ends so that anything
     * past them measures against the cap instead
     *
     * @param pixelX
     * @param pixelY
     * @return
     */
    private float along(float pixelX, float pixelY) {
        if (this.axisLengthSqr == 0f) {
            return 0f;
        }

        return (float) clamp(
            ((pixelX - this.fromX) * this.axisX + (pixelY - this.fromY) * this.axisY) / this.axisLengthSqr,
            0d, 1d
        );
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
