package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.painting.tools.brush.Bezier;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class Brush extends AbstractTool<BrushParameters> {
    public static final int HOTKEY = GLFW.GLFW_KEY_B;

    private final Component translatableComponent = Component.translatable("container.zetter.painting.tools.brush");

    private final Bezier brushBezier = new Bezier(0.455, 0.030, 0.515, 0.955);

    public Brush() {
        super(new ArrayList<>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
    }

    @Override
    public ToolShape getShape(BrushParameters params) {
        return null;
    }

    @Override
    public Component getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public boolean shouldAddAction(CanvasData canvasData, BrushParameters params, float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        int maxActiveDistance = (int) Math.ceil(params.getSize() / 2);

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

    /**
     * The gap back to the point the stroke came from is painted as one shape rather
     * than as a row of separate dabs: every pixel is measured against the whole
     * segment, so it is touched once and the band comes out even, with no dabs to
     * space out and no beading where they would have overlapped.
     *
     * @param canvasData
     * @param params
     * @param color
     * @param posX
     * @param posY
     * @param lastPosX
     * @param lastPosY
     * @return
     */
    @Override
    public int apply(CanvasData canvasData, BrushParameters params, int color, float posX, float posY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        return this.useSegment(
            canvasData, params, color,
            lastPosX == null ? posX : lastPosX,
            lastPosY == null ? posY : lastPosY,
            posX, posY
        );
    }

    /**
     * A point on its own is a segment that goes nowhere, which leaves a round dab
     */
    @Override
    public int useTool(CanvasData canvasData, BrushParameters params, int color, float posX, float posY) {
        return this.useSegment(canvasData, params, color, posX, posY, posX, posY);
    }

    /**
     * Paints the capsule swept by the brush travelling from one point to the other:
     * a pixel's weight comes from its distance to the nearest point of the segment,
     * which is the same measure a single dab uses, only against a line rather than
     * a point.
     *
     * The falloff runs from the middle of the brush to half a pixel past its radius.
     * Measuring it against the diameter, as this did before, left the edge of the
     * brush at half weight where the sampled box cut it off, and every dab carried
     * that rim.
     *
     * The half pixel is the pixel itself: distance is measured to its centre, but it
     * covers a square around that centre, so the brush reaches it as soon as it comes
     * within half a pixel of the middle. Without it the smallest brush lands between
     * four pixel centres and paints none of them.
     *
     * Both ends of that range matter. Dividing by the radius alone would spread the
     * falloff over a shorter distance than it actually reaches, which goes unnoticed
     * on a wide brush but saturates a narrow one: at the smallest size the radius is
     * half a pixel, so every pixel it touches would come out at full weight and a
     * brush meant to be finer than a pixel would lay down four solid ones.
     */
    private int useSegment(CanvasData canvasData, BrushParameters params, int color, float fromX, float fromY, float toX, float toY) {
        final int width = canvasData.getWidth();
        final int height = canvasData.getHeight();

        final float intensity = params.getIntensity();
        final float radius = params.getSize() / 2f;

        final int minX = (int) Brush.clamp(Math.floor(Math.min(fromX, toX) - radius), 0d, width - 1);
        final int maxX = (int) Brush.clamp(Math.ceil(Math.max(fromX, toX) + radius), 0d, width - 1);
        final int minY = (int) Brush.clamp(Math.floor(Math.min(fromY, toY) - radius), 0d, height - 1);
        final int maxY = (int) Brush.clamp(Math.ceil(Math.max(fromY, toY) + radius), 0d, height - 1);

        final float axisX = toX - fromX;
        final float axisY = toY - fromY;
        final float axisLengthSqr = axisX * axisX + axisY * axisY;

        double totalDamage = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                final float pixelX = x + 0.5f;
                final float pixelY = y + 0.5f;

                // Where the pixel falls along the segment, held between its ends so
                // that anything past them measures against the cap instead
                final float along = axisLengthSqr == 0f ? 0f : (float) Brush.clamp(
                    ((pixelX - fromX) * axisX + (pixelY - fromY) * axisY) / axisLengthSqr, 0d, 1d
                );

                final double offsetX = pixelX - (fromX + axisX * along);
                final double offsetY = pixelY - (fromY + axisY * along);

                final double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
                final double proximity = Brush.clamp((radius + 0.5d - distance) / (radius + 0.5d), 0d, 1d);

                if (proximity == 0) {
                    continue;
                }

                final double localIntensity = this.brushBezier.solve(proximity, 0.001d);

                this.pixelChange(canvasData, params, color, y * width + x, (float) localIntensity);

                totalDamage += localIntensity;
            }
        }

        return (int) Math.round(totalDamage * intensity);
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
