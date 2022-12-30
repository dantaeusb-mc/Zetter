package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;

public interface BlendingParameterHolder {
    String PARAMETER_CODE = "Blending";

    BlendingPipe.BlendingOption getBlending();

    void setBlending(BlendingPipe.BlendingOption blending);
}
