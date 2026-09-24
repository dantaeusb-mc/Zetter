package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.painting.tools.brush.Bezier;
import me.dantaeusb.zetter.painting.tools.brush.Capsule;
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
     * Paints the capsule swept by the brush travelling from one point to the other.
     * The shape and the falloff are {@link Capsule}'s; what the brush adds is the
     * curve it eases them with and the paint it spends doing it.
     *
     * @param canvasData
     * @param params
     * @param color
     * @param fromX
     * @param fromY
     * @param toX
     * @param toY
     * @return
     */
    private int useSegment(CanvasData canvasData, BrushParameters params, int color, float fromX, float fromY, float toX, float toY) {
        final int width = canvasData.getWidth();

        final float intensity = params.getIntensity();

        final Capsule capsule = new Capsule(
            fromX, fromY, toX, toY, params.getSize() / 2f, width, canvasData.getHeight()
        );

        double totalDamage = 0;

        for (int x = capsule.minX; x <= capsule.maxX; x++) {
            for (int y = capsule.minY; y <= capsule.maxY; y++) {
                final double proximity = capsule.proximity(x + 0.5f, y + 0.5f);

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
}
