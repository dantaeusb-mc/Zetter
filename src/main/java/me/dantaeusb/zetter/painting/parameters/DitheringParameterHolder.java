package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.DitheringPipe;

public interface DitheringParameterHolder {
    String PARAMETER_CODE = "Dithering";

    DitheringPipe.DitheringOption getDithering();

    void setDithering(DitheringPipe.DitheringOption blending);
}
