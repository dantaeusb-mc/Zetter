package me.dantaeusb.zetter.menu.painting.pipes;

import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.menu.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;

import java.util.HashMap;

public interface Pipe {

    boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params);

    int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity);
}