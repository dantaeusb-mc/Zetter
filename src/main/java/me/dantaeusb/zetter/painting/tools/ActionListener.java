package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.storage.CanvasData;

@FunctionalInterface
public interface ActionListener {
    public void useToolCallback(CanvasData canvas, AbstractTool tool, AbstractToolParameters parameters, int color, float posX, float posY);
}
