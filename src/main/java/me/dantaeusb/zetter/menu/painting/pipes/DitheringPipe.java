package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.menu.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;

import java.util.HashMap;

/**
 * Dithering is a method of mixing colors with limited color
 * space that is often used in pixel art
 */
public class DitheringPipe implements Pipe {

    @Override
    public boolean shouldUsePipe(HashMap<String, AbstractToolParameter> params) {
        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, HashMap<String, AbstractToolParameter> params, int color, int index) {
        // @todo: do dithering
        return color;
    }

    public enum DitheringOption {
        NO_DITHERING(1, 0),
        DENSE_DITHERING(2, 0),
        DENSE_SHIFTED_DITHERING(2, 1),
        SPARSE_DITHERING(4, 0),
        SPARSE_SHIFTED_DITHERING(4, 2);

        public final int size;
        public final int shift;

        DitheringOption(int size, int shift) {
            this.size = size;
            this.shift = shift;
        }
    }
}
