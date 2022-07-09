package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.DitheringPipe;

public interface DitheringInterface {
    DitheringPipe.DitheringOption getDithering();

    void setDithering(DitheringPipe.DitheringOption blending);
}
