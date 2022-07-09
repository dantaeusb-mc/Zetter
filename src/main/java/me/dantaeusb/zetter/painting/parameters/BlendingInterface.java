package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;

public interface BlendingInterface {
    BlendingPipe.BlendingOption getBlending();

    void setBlending(BlendingPipe.BlendingOption blending);
}
