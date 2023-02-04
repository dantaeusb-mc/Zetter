package me.dantaeusb.zetter.painting.pipes;

import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.DitheringParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * Dithering is a method of mixing colors with limited color
 * space that is often used in pixel art
 */
public class DitheringPipe implements Pipe {

    @Override
    public boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params) {
        if (params instanceof DitheringParameterHolder) {
            return ((DitheringParameterHolder) params).getDithering() != DitheringOption.NO_DITHERING;
        }

        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity) {
        if (params instanceof DitheringParameterHolder) {
            if (((DitheringParameterHolder) params).getDithering() == DitheringOption.DENSE_DITHERING) {
                int posX = index % canvas.getWidth();
                int posY = index / canvas.getWidth();

                if (posY % 2 == 0) {
                    return posX % 2 == 0 ? canvas.getColorAt(index) : color;
                } else {
                    return posX % 2 == 1 ? canvas.getColorAt(index) : color;
                }
            }
        }

        return color;
    }

    public enum DitheringOption {
        NO_DITHERING(1, 0, new TranslationTextComponent("container.zetter.painting.dithering.no")),
        DENSE_DITHERING(2, 0, new TranslationTextComponent("container.zetter.painting.dithering.dense"));

        public static final DitheringOption DEFAULT = NO_DITHERING;

        public final int size;
        public final int shift;

        public final ITextComponent translatableComponent;

        DitheringOption(int size, int shift, ITextComponent translatableComponent) {
            this.size = size;
            this.shift = shift;
            this.translatableComponent = translatableComponent;
        }
    }
}
