package me.dantaeusb.zetter.painting.pipes;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.BlendingParameterHolder;
import me.dantaeusb.zetter.painting.parameters.IntensityParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.painting.tools.Brush;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.TriFunction;
import org.joml.Vector3f;

/**
 * Mixes the color the tool carries into the color already on the canvas. How much
 * of the new color lands is the intensity, what route the mix takes between the two
 * is the blending option, see docs/painting-blending.md
 */
public class BlendingPipe implements Pipe {
    /**
     * Below this much chroma a color reads as gray, and a gray has no meaningful
     * hue to mix towards
     */
    private static final float ACHROMATIC_CHROMA = 0.0001f;

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    @Override
    public boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params) {
        if (tool instanceof Brush) {
            return true;
        }

        // We do not blend on max intensity
        if (params instanceof IntensityParameterHolder) {
            return ((IntensityParameterHolder) params).getIntensity() < 1f;
        }

        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity) {
        final int originalColor = canvas.getColorAt(index);

        float intensity = 1f;
        if (params instanceof IntensityParameterHolder) {
            intensity = ((IntensityParameterHolder) params).getIntensity();
        }

        intensity *= localIntensity;
        intensity = Math.min(1f, Math.max(0f, intensity));

        BlendingOption blending = BlendingOption.DEFAULT;
        if (params instanceof BlendingParameterHolder) {
            blending = ((BlendingParameterHolder) params).getBlending();
        }

        return blending.blendingFunction.apply(color, originalColor, intensity);
    }

    /**
     * Mixing light: channels are averaged where they stand, so two opposite colors
     * meet in the middle as a gray, the way two lamps pointed at one spot would.
     *
     * @param newColor
     * @param oldColor
     * @param intensity
     * @return
     */
    public static int blendAdditive(int newColor, int oldColor, float intensity) {
        final Vector3f mixed = Color.argbToRgb(newColor).lerp(Color.argbToRgb(oldColor), 1f - intensity);

        return Color.fromRgb(mixed).getARGB();
    }

    /**
     * Mixing pigment: the hue travels around the color wheel rather than across it,
     * so working yellow into blue passes through green instead of washing out to
     * gray, and the mix keeps the saturation the two colors started with.
     *
     * Done in OkLCh, the polar form of the Oklab space okHSL is built on. okHSL
     * itself fits every color into the RGB gamut, and that fitting is what makes
     * mixing predictable for a picker but loses accuracy on deep blues, which would
     * show up here as a stroke landing on the wrong color.
     *
     * @param newColor
     * @param oldColor
     * @param intensity
     * @return
     */
    public static int blendSubtractive(int newColor, int oldColor, float intensity) {
        final Vector3f newLab = Color.rgbToOklab(Color.argbToRgb(newColor));
        final Vector3f oldLab = Color.rgbToOklab(Color.argbToRgb(oldColor));

        final float newChroma = (float) Math.sqrt(newLab.y * newLab.y + newLab.z * newLab.z);
        final float oldChroma = (float) Math.sqrt(oldLab.y * oldLab.y + oldLab.z * oldLab.z);

        final float lightness = lerp(oldLab.x, newLab.x, intensity);
        final float chroma = lerp(oldChroma, newChroma, intensity);
        final float hue = lerpHue(
            (float) Math.atan2(oldLab.z, oldLab.y), oldChroma,
            (float) Math.atan2(newLab.z, newLab.y), newChroma,
            intensity
        );

        return Color.fromRgb(Color.oklabToRgb(new Vector3f(
            lightness,
            (float) (chroma * Math.cos(hue)),
            (float) (chroma * Math.sin(hue))
        ))).getARGB();
    }

    private static float lerp(float from, float to, float intensity) {
        return from + (to - from) * intensity;
    }

    /**
     * Takes the shorter of the two ways around the wheel. Hues exactly opposite each
     * other are the same distance either way, and the tie is always broken the same
     * direction: actions get replayed to rebuild the painting, so the same two colors
     * have to give the same mix every time.
     *
     * @param fromHue
     * @param fromChroma
     * @param toHue
     * @param toChroma
     * @param intensity
     * @return
     */
    private static float lerpHue(float fromHue, float fromChroma, float toHue, float toChroma, float intensity) {
        // A gray sitting at either end would otherwise drag the mix towards whatever
        // hue fell out of the conversion, so it takes the hue of the other side
        if (fromChroma <= ACHROMATIC_CHROMA) {
            return toHue;
        }

        if (toChroma <= ACHROMATIC_CHROMA) {
            return fromHue;
        }

        float delta = toHue - fromHue;

        if (delta > Math.PI) {
            delta -= TWO_PI;
        } else if (delta < -Math.PI) {
            delta += TWO_PI;
        }

        return fromHue + delta * intensity;
    }

    /**
     * Listed in the order their buttons appear in the widgets texture
     */
    public enum BlendingOption {
        SUBTRACTIVE(BlendingPipe::blendSubtractive, Component.translatable("container.zetter.painting.blending.subtractive")),
        ADDITIVE(BlendingPipe::blendAdditive, Component.translatable("container.zetter.painting.blending.additive"));

        public static final BlendingOption DEFAULT = SUBTRACTIVE;

        public final TriFunction<Integer, Integer, Float, Integer> blendingFunction;

        public final Component translatableComponent;

        /**
         * Parameters are carried by actions that come in from clients, and an action
         * naming a mode we do not have would otherwise throw halfway through a replay
         *
         * @param name
         * @return
         */
        public static BlendingOption byName(Object name) {
            for (BlendingOption option : values()) {
                if (option.name().equals(name)) {
                    return option;
                }
            }

            return DEFAULT;
        }

        BlendingOption(TriFunction<Integer, Integer, Float, Integer> blendingFunction, Component translatableComponent) {
            this.blendingFunction = blendingFunction;
            this.translatableComponent = translatableComponent;
        }
    }
}
