package me.dantaeusb.zetter.core.tools;

/**
 * AWT + additional functions
 * Don't use AWT to keep macs happy
 */
public class Color {

    public static final Color white     = new Color(255, 255, 255);
    public static final Color WHITE = white;

    public static final Color screenGray = new Color(198, 198, 198);

    public static final Color SCREEN_GRAY = screenGray;

    public static final Color lightGray = new Color(192, 192, 192);

    public static final Color LIGHT_GRAY = lightGray;

    public static final Color gray      = new Color(128, 128, 128);

    public static final Color GRAY = gray;

    public static final Color darkGray  = new Color(64, 64, 64);

    public static final Color DARK_GRAY = darkGray;

    public static final Color black     = new Color(0, 0, 0);

    public static final Color BLACK = black;

    public static final Color red       = new Color(255, 0, 0);

    public static final Color RED = red;

    public static final Color pink      = new Color(255, 175, 175);

    public static final Color PINK = pink;

    public static final Color orange    = new Color(255, 200, 0);

    public static final Color ORANGE = orange;

    public static final Color yellow    = new Color(255, 255, 0);

    public static final Color YELLOW = yellow;

    public static final Color green     = new Color(0, 255, 0);

    public static final Color GREEN = green;

    public static final Color magenta   = new Color(255, 0, 255);

    public static final Color MAGENTA = magenta;

    public static final Color cyan      = new Color(0, 255, 255);

    public static final Color CYAN = cyan;

    public static final Color blue      = new Color(0, 0, 255);

    public static final Color BLUE = blue;

    private int value;

    public Color(int rgb) {
        this.value = 0xff000000 | rgb;
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 0xFF);
    }

    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
    }

    public Color(float r, float g, float b) {
        this( (int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    public Color(float r, float g, float b, float a) {
        this((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255));
    }

    public int getRGB() {
        return value;
    }

    public float[] getRGBfloat() {
        return new float[] { this.getRed() / 255F, this.getGreen() / 255F, this.getBlue() / 255F };
    }

    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    /**
     * Returns the green component in the range 0-255 in the default sRGB
     * space.
     * @return the green component.
     * @see #getRGB
     */
    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    /**
     * Returns the blue component in the range 0-255 in the default sRGB
     * space.
     * @return the blue component.
     * @see #getRGB
     */
    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    /**
     * Returns the alpha component in the range 0-255.
     * @return the alpha component.
     * @see #getRGB
     */
    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }


    public float[] toHSB() {
        return Color.RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue(), null);
    }

    public float[] toHSB(float[] hsbvals) {
        return Color.RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue(), hsbvals);
    }

    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }
}