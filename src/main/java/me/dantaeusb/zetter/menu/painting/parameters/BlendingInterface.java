package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;

public interface BlendingInterface {
    BlendingPipe.BlendingOption getBlending();

    void setBlending(BlendingPipe.BlendingOption blending);
}
