package me.dantaeusb.zetter.menu.painting.parameters;

public class OpacityParameter extends AbstractToolParameter<Float> {
    public static final String CODE = "opacity";

    public OpacityParameter() {
        super(OpacityParameter.CODE, 1.0F);
    }
}
