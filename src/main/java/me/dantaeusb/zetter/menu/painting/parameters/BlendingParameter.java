package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;

public class BlendingParameter extends AbstractToolParameter<BlendingPipe.BlendingOption> {
    public static final String CODE = "blending";

    public BlendingParameter() {
        super(BlendingParameter.CODE, BlendingPipe.BlendingOption.RYB);
    }
}
