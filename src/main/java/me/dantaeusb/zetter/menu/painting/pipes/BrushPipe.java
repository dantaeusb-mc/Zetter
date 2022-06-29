package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.menu.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;

import java.util.HashMap;

public class BrushPipe implements Pipe {
    @Override
    public int process(float posX, float posY, int newColor, int oldColor) {
        return 0x00000000;
    }

    @Override
    public boolean shouldUsePipe(HashMap<String, AbstractToolParameter> params) {
        return false;
    }

    @Override
    public int applyPipe(CanvasData canvas, HashMap<String, AbstractToolParameter> params, int color, int index) {
        return color;
    }
}

