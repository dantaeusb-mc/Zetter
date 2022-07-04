package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.menu.painting.parameters.DitheringInterface;
import me.dantaeusb.zetter.menu.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.HashMap;

/**
 * Dithering is a method of mixing colors with limited color
 * space that is often used in pixel art
 */
public class DitheringPipe implements Pipe {

    @Override
    public boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params) {
        if (params instanceof DitheringInterface) {
            return ((DitheringInterface) params).getDithering() != DitheringOption.NO_DITHERING;
        }

        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity) {
        if (params instanceof DitheringInterface) {
            if (((DitheringInterface) params).getDithering() == DitheringOption.DENSE_DITHERING) {
                int posX = index % canvas.getHeight();
                int posY = index / canvas.getHeight();

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
        NO_DITHERING(1, 0, new TranslatableComponent("container.zetter.painting.dithering.no")),
        DENSE_DITHERING(2, 0, new TranslatableComponent("container.zetter.painting.dithering.dense"));

        public static final DitheringOption DEFAULT = NO_DITHERING;

        public final int size;
        public final int shift;

        public final TranslatableComponent translatableComponent;

        DitheringOption(int size, int shift, TranslatableComponent translatableComponent) {
            this.size = size;
            this.shift = shift;
            this.translatableComponent = translatableComponent;
        }
    }
}
