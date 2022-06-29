package me.dantaeusb.zetter.menu.painting.parameters;

public class SizeParameter extends AbstractToolParameter<Integer> {
    public static final String CODE = "size";

    public SizeParameter() {
        super(SizeParameter.CODE, 1);
    }
}
