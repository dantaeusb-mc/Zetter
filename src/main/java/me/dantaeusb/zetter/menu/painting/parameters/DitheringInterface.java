package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;

public interface DitheringInterface {
    DitheringPipe.DitheringOption getDithering();

    void setDithering(DitheringPipe.DitheringOption blending);
}
