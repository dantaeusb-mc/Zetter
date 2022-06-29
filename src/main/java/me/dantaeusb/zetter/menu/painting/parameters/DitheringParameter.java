package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;

public class DitheringParameter extends AbstractToolParameter<Integer> {
    public static final String CODE = "dithering";

    public DitheringParameter() {
        super(DitheringParameter.CODE, DitheringPipe.DitheringOption.NO_DITHERING.ordinal());
    }
}
