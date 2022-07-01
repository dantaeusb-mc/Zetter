package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.storage.CanvasData;

import java.util.HashMap;

public interface Pipe {

    boolean shouldUsePipe(HashMap<String, AbstractToolParameter> params);

    int applyPipe(CanvasData canvas, HashMap<String, AbstractToolParameter> params, int color, int index);
}