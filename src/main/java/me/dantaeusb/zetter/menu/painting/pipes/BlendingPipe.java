package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.menu.painting.parameters.BlendingInterface;
import me.dantaeusb.zetter.menu.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.menu.painting.parameters.IntensityInterface;
import me.dantaeusb.zetter.menu.painting.tools.AbstractTool;
import me.dantaeusb.zetter.menu.painting.tools.Brush;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.function.TriFunction;


public class BlendingPipe implements Pipe {

    @Override
    public boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params) {
        if (tool instanceof Brush) {
            return true;
        }

        // We do not blend on max intensity
        if (params instanceof IntensityInterface) {
            return ((IntensityInterface) params).getIntensity() < 1f;
        }

        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity) {
        final int originalColor = canvas.getColorAt(index);

        float intensity = 1f;
        if (params instanceof IntensityInterface) {
            intensity = ((IntensityInterface) params).getIntensity();
        }

        intensity *= localIntensity;

        BlendingOption blending = BlendingOption.DEFAULT;
        if (params instanceof BlendingInterface) {
            blending = ((BlendingInterface) params).getBlending();
        }

        return blending.blendingFunction.apply(color, originalColor, intensity);
    }

    /**
     * Basic blending, in target color space so nothing fancy here
     * @param newColor
     * @param oldColor
     * @param intensity
     * @return
     */
    public static int blendRGB(int newColor, int oldColor, float intensity) {
        final Color newColorModel = new Color(newColor);
        final Color oldColorModel = new Color(oldColor);

        return new Color(
                (newColorModel.getRed() * intensity + oldColorModel.getRed() * (1f - intensity)) / 255,
                (newColorModel.getGreen() * intensity + oldColorModel.getGreen() * (1f - intensity)) / 255,
                (newColorModel.getBlue() * intensity + oldColorModel.getBlue() * (1f - intensity)) / 255
        ).getRGB();
    }

    /**
     * Blend with
     * http://nishitalab.org/user/UEI/publication/Sugita_IWAIT2015.pdf
     *
     * Problem: any clear color (black, grey, white)
     * will result in an unfixable black pixel
     *
     * @param newColor
     * @param oldColor
     * @return
     */
    public static int blendRYB(int newColor, int oldColor, float intensity) {
        intensity = Math.max(0, Math.min(1, intensity));

        // RYB doesn't work with pitch black, lighten it a bit
        final float[] newFloat = BlendingPipe.protectPitchBlack(new Color(newColor).getRGBfloat());
        final float[] oldFloat = BlendingPipe.protectPitchBlack(new Color(oldColor).getRGBfloat());

        final float[] rybNew = BlendingPipe.rgbToRyb(newFloat);
        final float[] rybOld = BlendingPipe.rgbToRyb(oldFloat);

        final float negativeIntensity = 1F - intensity;
        final float[] rybResult = new float[] {
                intensity * rybNew[0] + negativeIntensity * rybOld[0],
                intensity * rybNew[1] + negativeIntensity * rybOld[1],
                intensity * rybNew[2] + negativeIntensity * rybOld[2],
        };

        final float[] rgbResult = BlendingPipe.protectOverflow(BlendingPipe.rybToRgb(rybResult));

        return new Color(rgbResult[0], rgbResult[1], rgbResult[2]).getRGB();
    }

    /**
     * @param rgb
     * @return
     */
    private static float[] rgbToRyb(float[] rgb) {
        final float w = Math.min(Math.min(rgb[0], rgb[1]), rgb[2]);

        float rgbR = rgb[0] - w;
        float rgbG = rgb[1] - w;
        float rgbB = rgb[2] - w;

        // Can remove duplicates but I hope compiler is smart
        float rybR = rgbR - Math.min(rgbR, rgbG);
        float rybY = .5F * (rgbG + Math.min(rgbR, rgbG));
        float rybB = .5F * (rgbB + rgbG - Math.min(rgbR, rgbG));

        float n = Math.max(Math.max(rybR, rybY), rybB) / Math.max(Math.max(rgbR, rgbG), rgbB);

        if (Float.isNaN(n) || n == 0F) {
            n = 1F;
        }

        rybR /= n;
        rybY /= n;
        rybB /= n;

        float b = Math.min(Math.min(1F - rgb[0], 1F - rgb[1]), 1F - rgb[2]);

        return new float[] { rybR + b, rybY + b, rybB + b };
    }

    private static float[] rybToRgb(float[] ryb) {
        final float w = Math.min(Math.min(ryb[0], ryb[1]), ryb[2]);

        float rybR = ryb[0] - w;
        float rybY = ryb[1] - w;
        float rybB = ryb[2] - w;

        float rgbR = rybR + rybY - Math.min(rybY, rybB);
        float rgbG = rybY + Math.min(rybY, rybB);
        float rgbB = 2F * (rybB - Math.min(rybY, rybB));

        float n = Math.max(Math.max(rgbR, rgbG), rgbB) / Math.max(Math.max(rybR, rybY), rybB);
        n = n == 0F ? 1F : n;

        rgbR /= n;
        rgbG /= n;
        rgbB /= n;

        float b = Math.min(Math.min(1F - ryb[0], 1F - ryb[1]), 1F - ryb[2]);

        return new float[] { rgbR + b, rgbG + b, rgbB + b };

    }

    /**
     * Blend with
     * http://scottburns.us/fast-rgb-to-spectrum-conversion-for-reflectances/
     * and
     * http://scottburns.us/subtractive-color-mixture/
     *
     * @param newColor
     * @param oldColor
     * @return
     */
    public static int blendRGBC(int newColor, int oldColor, float intensity) {
        return newColor;
    }

    private static float[] protectPitchBlack(float[] rgb) {
        return new float[] {
                Math.max(rgb[0], 0.004F),
                Math.max(rgb[1], 0.004F),
                Math.max(rgb[2], 0.004F),
        };
    }

    private static float[] protectOverflow(float[] rgb) {
        return new float[] {
                Math.min(Math.max(rgb[0], 0F), 1F),
                Math.min(Math.max(rgb[1], 0F), 1F),
                Math.min(Math.max(rgb[2], 0F), 1F),
        };
    }

    public enum BlendingOption {
        RGB("rgb", BlendingPipe::blendRGB, new TranslatableComponent("container.zetter.painting.blending.additive")),
        RYB("ryb", BlendingPipe::blendRYB, new TranslatableComponent("container.zetter.painting.blending.subtractive")),
        RGBC("rgbc", BlendingPipe::blendRGBC, new TranslatableComponent("container.zetter.painting.blending.realistic"));

        public static final BlendingOption DEFAULT = RYB;

        public final String code;
        public final TriFunction<Integer, Integer, Float, Integer> blendingFunction;

        public final TranslatableComponent translatableComponent;

        BlendingOption(String code, TriFunction<Integer, Integer, Float, Integer> blendingFunction, TranslatableComponent translatableComponent) {
            this.code = code;
            this.blendingFunction = blendingFunction;
            this.translatableComponent = translatableComponent;
        }
    }
}

