package me.dantaeusb.zetter.painting.pipes;

import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.DitheringParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;

/**
 * Dithering is a method of mixing colors with limited color
 * space that is often used in pixel art
 */
public class DitheringPipe implements Pipe {

    @Override
    public boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params, int color) {
        if (params instanceof DitheringParameterHolder) {
            return ((DitheringParameterHolder) params).getDithering() != DitheringOption.NO_DITHERING;
        }

        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity) {
        if (!(params instanceof DitheringParameterHolder)) {
            return color;
        }

        final int posX = index % canvas.getWidth();
        final int posY = index / canvas.getWidth();

        if (((DitheringParameterHolder) params).getDithering().paints(posX, posY)) {
            return color;
        }

        return canvas.getColorAt(index);
    }

    /**
     * Listed in the order their buttons appear in the widgets texture
     */
    public enum DitheringOption {
        NO_DITHERING(Component.translatable("container.zetter.painting.dithering.no")),
        CHECKER(Component.translatable("container.zetter.painting.dithering.checker")),
        CHECKER_INVERTED(Component.translatable("container.zetter.painting.dithering.checker_inverted")),
        SPARSE(Component.translatable("container.zetter.painting.dithering.sparse"));

        public static final DitheringOption DEFAULT = NO_DITHERING;

        public final Component translatableComponent;

        DitheringOption(Component translatableComponent) {
            this.translatableComponent = translatableComponent;
        }

        /**
         * See {@link BlendingPipe.BlendingOption#byName}
         *
         * @param name
         * @return
         */
        public static DitheringOption byName(Object name) {
            for (DitheringOption option : values()) {
                if (option.name().equals(name)) {
                    return option;
                }
            }

            return DEFAULT;
        }

        /**
         * Whether the pixel takes the new color or keeps the one it had. Decided by
         * the position on the canvas rather than by the position under the cursor, so
         * that a pattern stays lined up across strokes instead of breaking where one
         * stroke meets the next.
         *
         * @param posX
         * @param posY
         * @return
         */
        public boolean paints(int posX, int posY) {
            return switch (this) {
                case NO_DITHERING -> true;
                case CHECKER -> (posX + posY) % 2 == 0;
                case CHECKER_INVERTED -> (posX + posY) % 2 != 0;
                case SPARSE -> posX % 2 == 0 && posY % 2 == 0;
            };
        }
    }
}
