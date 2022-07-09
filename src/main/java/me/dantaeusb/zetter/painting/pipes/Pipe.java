package me.dantaeusb.zetter.painting.pipes;

import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;

public interface Pipe {

    boolean shouldUsePipe(AbstractTool tool, AbstractToolParameters params);

    int applyPipe(CanvasData canvas, AbstractToolParameters params, int color, int index, float localIntensity);
}