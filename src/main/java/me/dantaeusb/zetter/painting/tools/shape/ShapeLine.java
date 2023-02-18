package me.dantaeusb.zetter.painting.tools.shape;

public class ShapeLine {
    public final LineDirection direction;
    public final int posX;
    public final int posY;
    public final int length;

    public ShapeLine(LineDirection direction, int posX, int posY, int length) {
        this.direction = direction;
        this.posX = posX;
        this.posY = posY;
        this.length = length;
    }

    public enum LineDirection {
        HORIZONTAL(0, "Horizontal"),
        VERTICAL(1, "Vertical");

        private final int step;
        private final String name;

        LineDirection(int step, String name) {
            this.step = step;
            this.name = name;
        }
    }
}