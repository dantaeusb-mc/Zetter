package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.storage.CanvasData;

import java.util.HashMap;

public interface Pipe {
    /**
     * Get action through pipe
     *
     * @param posX
     * @param posY
     * @param newColor
     * @param oldColor
     *
     * @return Resulting color
     */
    int process(float posX, float posY, int newColor, int oldColor);

    boolean shouldUsePipe(HashMap<String, AbstractToolParameter> params);

    int applyPipe(CanvasData canvas, HashMap<String, AbstractToolParameter> params, int color, int index);
}