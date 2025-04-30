package me.dantaeusb.zetter.core.tools;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * AWT + additional functions
 * Don't use AWT to keep macs happy
 */
public class Color {
  private int argb = 0xFFFFFFFF;
  private Vector3f rgb = new Vector3f(1.0f, 1.0f, 1.0f);
  private Vector3f hsl;
  private Vector3f okHsl;

  public Color(int rgb) {
    this.argb = 0xff000000 | rgb;
    this.rgb = Color.argbToRgb(rgb);
  }

  public Color(Vector3f color, Mode mode) {
    switch (mode) {
      case RGB:
        this.rgb = color;
        break;
      case HSL:
        this.hsl = color;
        this.rgb = Color.hslToRgb(color);
        break;
      case OKHSL:
        this.okHsl = color;
        this.rgb = Color.okhslToRgb(color);
        break;
    }

    this.argb = Color.rgbToArgb(this.rgb);
  }

  public static Color fromRgb(Vector3f rgb) {
    return new Color(rgb, Mode.RGB);
  }

  public static Color fromHsl(Vector3f hsl) {
    return new Color(hsl, Mode.HSL);
  }

  public static Color fromOkHsl(Vector3f okHsl) {
    return new Color(okHsl, Mode.OKHSL);
  }

  public int getARGB() {
    return this.argb;
  }

  public float[] getRGBfloat() {
    return new float[]{this.getRed() / 255F, this.getGreen() / 255F, this.getBlue() / 255F};
  }

  public int getRed() {
    return (this.getARGB() >> 16) & 0xFF;
  }

  /**
   * Returns the green component in the range 0-255 in the default sRGB
   * space.
   *
   * @return the green component.
   * @see #getARGB
   */
  public int getGreen() {
    return (this.getARGB() >> 8) & 0xFF;
  }

  /**
   * Returns the blue component in the range 0-255 in the default sRGB
   * space.
   *
   * @return the blue component.
   * @see #getARGB
   */
  public int getBlue() {
    return (this.getARGB()) & 0xFF;
  }

  public Vector3f getHsl() {
    if (this.hsl == null) {
      this.hsl = Color.rgbToHsl(this.rgb);
    }

    return this.hsl;
  }

  public Vector3f getOkHsl() {
    if (this.okHsl == null) {
      this.okHsl = Color.rgbToOkHsl(this.rgb);
    }

    return this.okHsl;
  }

  public static int rgbToArgb(Vector3f rgb) {
    return (0xFF << 24) | ((int) (rgb.x * 255.0f) << 16) | ((int) (rgb.y * 255.0f) << 8) | (int) (rgb.z * 255.0f);
  }

  public static Vector3f argbToRgb(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = (argb) & 0xFF;
    return new Vector3f(r / 255.0f, g / 255.0f, b / 255.0f);
  }

  // Color conversion helpers

  /**
   * This is not needed for anything but export. Work should be done in linear RGB space.
   * OpenGL will do the conversion.
   * However, this does not apply to shaders, which will need to do the conversion manually.
   * <p>
   *
   * @param linearRgb
   * @return
   */
  public static Vector3f rgbToSrgb(Vector3f linearRgb) {
    Vector3f srgb = new Vector3f();
    for (int i = 0; i < 3; i++) {
      float v = linearRgb.get(i);
      if (v <= 0.0031308f) {
        v = 12.92f * v;
      } else {
        v = 1.055f * (float) Math.pow(v, 1.0 / 2.4) - 0.055f;
      }
      srgb.setComponent(i, v);
    }
    return srgb;
  }

  /**
   * This is not needed for anything but export. Work should be done in linear RGB space.
   * OpenGL will do the conversion.
   * However, this does not apply to shaders, which will need to do the conversion manually.
   * <p>
   *
   * @param color
   * @return
   */
  public static int rgbToSrgb(int color) {
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = (color) & 0xFF;

    Vector3f linearRgb = new Vector3f(r / 255.0f, g / 255.0f, b / 255.0f);
    Vector3f srgb = rgbToSrgb(linearRgb);
    r = (int) (srgb.x * 255.0f);
    g = (int) (srgb.y * 255.0f);
    b = (int) (srgb.z * 255.0f);

    return (0xFF << 24) | (r << 16) | (g << 8) | b;
  }

  public static Vector3f rgbToHsl(Vector3f rgb) {
    float hue, saturation, brightness;
    Vector3f hsbvals = new Vector3f();
    float cmax = Math.max(rgb.x, rgb.y);
    if (rgb.z > cmax) cmax = rgb.z;
    float cmin = Math.min(rgb.x, rgb.y);
    if (rgb.z < cmin) cmin = rgb.z;

    brightness = cmax;
    if (cmax != 0)
      saturation = (cmax - cmin) / cmax;
    else
      saturation = 0;
    if (saturation == 0)
      hue = 0;
    else {
      float redc = (cmax - rgb.x) / (cmax - cmin);
      float greenc = (cmax - rgb.y) / (cmax - cmin);
      float bluec = (cmax - rgb.z) / (cmax - cmin);

      if (rgb.x == cmax)
        hue = bluec - greenc;
      else if (rgb.y == cmax)
        hue = 2.0f + redc - bluec;
      else
        hue = 4.0f + greenc - redc;
      hue = hue / 6.0f;
      if (hue < 0)
        hue = hue + 1.0f;
    }

    hsbvals.x = hue;
    hsbvals.y = saturation;
    hsbvals.z = brightness;

    return hsbvals;
  }

  /**
   * @param hsl
   * @return
   * @todo: [URG] Incorrect, it's HSV to RGB, not HSL to RGB
   */
  public static Vector3f hslToRgb(Vector3f hsl) {
    float r = 0.0f, g = 0.0f, b = 0.0f;
    if (hsl.y == 0) {
      r = g = b = hsl.z;
    } else {
      float q = hsl.z < 0.5f ? hsl.z * (1.0f + hsl.y) : hsl.z + hsl.y - hsl.z * hsl.y;
      float p = 2.0f * hsl.z - q;
      r = hueToRgb(p, q, hsl.x + 1.0f / 3.0f);
      g = hueToRgb(p, q, hsl.x);
      b = hueToRgb(p, q, hsl.x - 1.0f / 3.0f);
    }
    return new Vector3f(r, g, b);
  }

  private static float hueToRgb(float p, float q, float t) {
    if (t < 0.0f) t += 1.0f;
    if (t > 1.0f) t -= 1.0f;
    if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
    if (t < 1.0f / 2.0f) return q;
    if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
    return p;
  }

  public static Vector3f rgbToOkHsl(Vector3f rgb) {
    Vector3f lab = rgbToOklab(rgb);
    float C = (float) Math.sqrt(lab.y * lab.y + lab.z * lab.z);
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    float L = lab.x;
    float h = (float) (0.5 + 0.5 * Math.atan2(-lab.z, -lab.y) / Math.PI);

    Vector3f cs = getCs(L, a_, b_);
    float C_0 = cs.x;
    float C_mid = cs.y;
    float C_max = cs.z;

    float s;
    if (C < C_mid) {
      float k_0 = 0.0f;
      float k_1 = 0.8f * C_0;
      float k_2 = (1.0f - k_1 / C_mid);

      float t = (C - k_0) / (k_1 + k_2 * (C - k_0));
      s = t * 0.8f;
    } else {
      float k_0 = C_mid;
      float k_1 = 0.2f * C_mid * C_mid * 1.25f * 1.25f / C_0;
      float k_2 = (1 - (k_1) / (C_max - C_mid));

      float t = (C - k_0) / (k_1 + k_2 * (C - k_0));
      s = 0.8f + 0.2f * t;
    }

    float l = toe(L);
    return new Vector3f(h, s, l);
  }

  public static Vector3f okhslToRgb(Vector3f hsl) {
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;

    if (l == 1.0f) {
      return new Vector3f(1.0f, 1.0f, 1.0f);
    } else if (l == 0.0f) {
      return new Vector3f(0.0f, 0.0f, 0.0f);
    }

    float a = (float) Math.cos(2.0 * Math.PI * h);
    float b = (float) Math.sin(2.0 * Math.PI * h);
    float L = toeInv(l);

    Vector3f cs = getCs(L, a, b);
    float C_0 = cs.x;
    float C_mid = cs.y;
    float C_max = cs.z;

    float mid = 0.8f;
    float midInv = 1.25f;

    float C, t, k_0, k_1, k_2;

    if (s < mid) {
      t = midInv * s;
      k_1 = mid * C_0;
      k_2 = (1.0f - k_1 / C_mid);
      C = t * k_1 / (1.0f - k_2 * t);
    } else {
      t = (s - mid) / (1.0f - mid);
      k_0 = C_mid;
      k_1 = (1.0f - mid) * C_mid * C_mid * midInv * midInv / C_0;
      k_2 = (1.0f - (k_1) / (C_max - C_mid));
      C = k_0 + t * k_1 / (1.0f - k_2 * t);
    }

    return oklabToRgb(new Vector3f(L, C * a, C * b));
  }

  public static Vector3f rgbToOklab(Vector3f rgb) {
    double l = (float) (0.4122214708 * rgb.x + 0.5363325363 * rgb.y + 0.0514459929 * rgb.z);
    double m = (float) (0.2119034982 * rgb.x + 0.6806995451 * rgb.y + 0.1073969566 * rgb.z);
    double s = (float) (0.0883024619 * rgb.x + 0.2817188376 * rgb.y + 0.6299787005 * rgb.z);

    double l_ = Math.cbrt(l);
    double m_ = Math.cbrt(m);
    double s_ = Math.cbrt(s);

    return new Vector3f(
        (float) (0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_),
        (float) (1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_),
        (float) (0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_)
    );
  }

  public static Vector3f oklabToRgb(Vector3f okLab) {
    float L_ = okLab.x + 0.3963377774f * okLab.y + 0.2158037573f * okLab.z;
    float M_ = okLab.x - 0.1055613458f * okLab.y - 0.0638541728f * okLab.z;
    float S_ = okLab.x - 0.0894841775f * okLab.y - 1.2914855480f * okLab.z;

    float L = L_ * L_ * L_;
    float M = M_ * M_ * M_;
    float S = S_ * S_ * S_;

    float r = 4.0767416621f * L - 3.3077115913f * M + 0.2309699292f * S;
    float g = -1.2684380046f * L + 2.6097574011f * M - 0.3413193965f * S;
    float b = -0.0041960863f * L - 0.7034186147f * M + 1.7076147010f * S;

    return new Vector3f(r, g, b);
  }

  /*
   * okHSL/okLCH helper section
   */

  private static Vector3f getCs(float L, float a, float b) {
    Vector2f cusp = findCusp(a, b);

    float C_max = findGamutIntersection(a, b, L, 1.0f, L, cusp);
    Vector2f ST_max = toST(cusp);

    float k = C_max / Math.min((L * ST_max.x), (1.0f - L) * ST_max.y);

    float C_a;
    float C_b;

    float C_mid;
    Vector2f ST_mid = getStMid(a, b);

    // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
    C_a = L * ST_mid.x;
    C_b = (1.0f - L) * ST_mid.y;
    C_mid = 0.9f * k * (float) Math.sqrt(Math.sqrt(1.0f / (1.0f / (C_a * C_a * C_a * C_a) + 1.0f / (C_b * C_b * C_b * C_b))));

    float C_0;
    // for C_0, the shape is independent of hue, so Vector2f are constant. Values picked to roughly be the average values of Vector2f.
    C_a = L * 0.4f;
    C_b = (1.0f - L) * 0.8f;

    // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
    C_0 = (float) Math.sqrt(1.0f / (1.0f / (C_a * C_a) + 1.0f / (C_b * C_b)));

    return new Vector3f(C_0, C_mid, C_max);
  }

  private static Vector2f findCusp(float a, float b) {
    float SCusp = computeMaxSaturation(a, b);
    Vector3f rgbAtMax = oklabToRgb(new Vector3f(1.0f, SCusp * a, SCusp * b));
    float LCusp = (float) Math.cbrt(1.0f / Math.max(Math.max(rgbAtMax.x, rgbAtMax.y), rgbAtMax.z));
    float CCusp = SCusp * LCusp;

    return new Vector2f(LCusp, CCusp);
  }

  private static float computeMaxSaturation(float a, float b) {
    // Max saturation will be when one of r, g or b goes below zero.

    // Select different coefficients depending on which component goes below zero first
    float k0, k1, k2, k3, k4, wl, wm, ws;

    if (-1.88170328f * a - 0.80936493f * b > 1.0f) {
      // Red component
      k0 = 1.19086277f;
      k1 = 1.76576728f;
      k2 = 0.59662641f;
      k3 = 0.75515197f;
      k4 = 0.56771245f;
      wl = 4.0767416621f;
      wm = -3.3077115913f;
      ws = 0.2309699292f;
    } else if (1.81444104f * a - 1.19445276f * b > 1.0f) {
      // Green component
      k0 = 0.73956515f;
      k1 = -0.45954404f;
      k2 = 0.08285427f;
      k3 = 0.12541070f;
      k4 = 0.14503204f;
      wl = -1.2684380046f;
      wm = 2.6097574011f;
      ws = -0.3413193965f;
    } else {
      // Blue component
      k0 = 1.35733652f;
      k1 = -0.00915799f;
      k2 = -1.15130210f;
      k3 = -0.50559606f;
      k4 = 0.00692167f;
      wl = -0.0041960863f;
      wm = -0.7034186147f;
      ws = 1.7076147010f;
    }

    // Approximate max saturation using a polynomial:
    float S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b;

    // Do one step Halley's method to get closer
    // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
    // this should be sufficient for most applications, otherwise do two/three steps

    float k_l = 0.3963377774f * a + 0.2158037573f * b;
    float k_m = -0.1055613458f * a - 0.0638541728f * b;
    float k_s = -0.0894841775f * a - 1.2914855480f * b;

    float l_ = 1.0f + S * k_l;
    float m_ = 1.0f + S * k_m;
    float s_ = 1.0f + S * k_s;

    float l = l_ * l_ * l_;
    float m = m_ * m_ * m_;
    float s = s_ * s_ * s_;

    float l_dS = 3.0f * k_l * l_ * l_;
    float m_dS = 3.0f * k_m * m_ * m_;
    float s_dS = 3.0f * k_s * s_ * s_;

    float l_dS2 = 6.0f * k_l * k_l * l_;
    float m_dS2 = 6.0f * k_m * k_m * m_;
    float s_dS2 = 6.0f * k_s * k_s * s_;

    float f = wl * l + wm * m + ws * s;
    float f1 = wl * l_dS + wm * m_dS + ws * s_dS;
    float f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2;

    S = S - f * f1 / (f1 * f1 - 0.5f * f * f2);

    return S;
  }

  private static float findGamutIntersection(float a, float b, float L1, float C1, float L0, Vector2f cusp) {
    // Find the intersection for upper and lower half seprately
    float t;
    if (((L1 - L0) * cusp.y - (cusp.x - L0) * C1) <= 0.0) {
      // Lower half

      t = cusp.y * L0 / (C1 * cusp.x + cusp.y * (L0 - L1));
    } else {
      // Upper half

      // First intersect with triangle
      t = cusp.y * (L0 - 1.0f) / (C1 * (cusp.x - 1.0f) + cusp.y * (L0 - L1));

      // Then one step Halley's method
      float dL = L1 - L0;
      float dC = C1;

      float k_l = +0.3963377774f * a + 0.2158037573f * b;
      float k_m = -0.1055613458f * a - 0.0638541728f * b;
      float k_s = -0.0894841775f * a - 1.2914855480f * b;

      float l_dt = dL + dC * k_l;
      float m_dt = dL + dC * k_m;
      float s_dt = dL + dC * k_s;


      // If higher accuracy is required, 2 or 3 iterations of the following block can be used:
      float L = L0 * (1.0f - t) + t * L1;
      float C = t * C1;

      float l_ = L + C * k_l;
      float m_ = L + C * k_m;
      float s_ = L + C * k_s;

      float l = l_ * l_ * l_;
      float m = m_ * m_ * m_;
      float s = s_ * s_ * s_;

      float ldt = 3.0f * l_dt * l_ * l_;
      float mdt = 3.0f * m_dt * m_ * m_;
      float sdt = 3.0f * s_dt * s_ * s_;

      float ldt2 = 6.0f * l_dt * l_dt * l_;
      float mdt2 = 6.0f * m_dt * m_dt * m_;
      float sdt2 = 6.0f * s_dt * s_dt * s_;

      float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s - 1.0f;
      float r1 = 4.0767416621f * ldt - 3.3077115913f * mdt + 0.2309699292f * sdt;
      float r2 = 4.0767416621f * ldt2 - 3.3077115913f * mdt2 + 0.2309699292f * sdt2;

      float u_r = r1 / (r1 * r1 - 0.5f * r * r2);
      float t_r = -r * u_r;

      float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s - 1.0f;
      float g1 = -1.2684380046f * ldt + 2.6097574011f * mdt - 0.3413193965f * sdt;
      float g2 = -1.2684380046f * ldt2 + 2.6097574011f * mdt2 - 0.3413193965f * sdt2;

      float u_g = g1 / (g1 * g1 - 0.5f * g * g2);
      float t_g = -g * u_g;

      b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s - 1.0f;
      float b1 = -0.0041960863f * ldt - 0.7034186147f * mdt + 1.7076147010f * sdt;
      float b2 = -0.0041960863f * ldt2 - 0.7034186147f * mdt2 + 1.7076147010f * sdt2;

      float u_b = b1 / (b1 * b1 - 0.5f * b * b2);
      float t_b = -b * u_b;

      t_r = u_r >= 0.0f ? t_r : 10000.0f;
      t_g = u_g >= 0.0f ? t_g : 10000.0f;
      t_b = u_b >= 0.0f ? t_b : 10000.0f;

      t += Math.min(t_r, Math.min(t_g, t_b));
    }

    return t;
  }

  private static Vector2f toST(Vector2f cusp) {
    float L = cusp.x;
    float C = cusp.y;
    return new Vector2f(C / L, C / (1.0f - L));
  }

  private static Vector2f getStMid(float a_, float b_) {
    float S = 0.11516993f + 1.0f / (
        7.44778970f + 4.15901240f * b_
            + a_ * (-2.19557347f + 1.75198401f * b_
            + a_ * (-2.13704948f - 10.02301043f * b_
            + a_ * (-4.24894561f + 5.38770819f * b_ + 4.69891013f * a_
        )))
    );

    float T = 0.11239642f + 1.0f / (
        1.61320320f - 0.68124379f * b_
            + a_ * (+0.40370612f + 0.90148123f * b_
            + a_ * (-0.27087943f + 0.61223990f * b_
            + a_ * (+0.00299215f - 0.45399568f * b_ - 0.14661872f * a_
        )))
    );

    return new Vector2f(S, T);
  }

  private static final float TOE_K1 = 0.206f;
  private static final float TOE_K2 = 0.03f;
  private static final float TOE_K3 = (1.0f + TOE_K1) / (1.0f + TOE_K2);

  private static float toe(float x) {
    return 0.5f * (TOE_K3 * x - TOE_K1 + (float) Math.sqrt((TOE_K3 * x - TOE_K1) * (TOE_K3 * x - TOE_K1) + 4 * TOE_K2 * TOE_K3 * x));
  }

  private static float toeInv(float x) {
    return (x * x + TOE_K1 * x) / (TOE_K3 * (x + TOE_K2));
  }

  /*
   * Constants
   */

  public static final Color WHITE = new Color(0xFFFFFF);
  public static final Color SCREEN_GRAY = new Color(0xC6C6C6);
  public static final Color LIGHT_GRAY = new Color(0xC0C0C0);
  public static final Color GRAY = new Color(0x808080);
  public static final Color DARK_GRAY = new Color(0x404040);
  public static final Color BLACK = new Color(0x000000);
  public static final Color RED = new Color(0xFF0000);
  public static final Color PINK = new Color(0xFFAFAF);
  public static final Color ORANGE = new Color(0xFFC800);
  public static final Color YELLOW = new Color(0xFFFF00);
  public static final Color GREEN = new Color(0x00FF00);
  public static final Color MAGENTA = new Color(0xFF00FF);
  public static final Color CYAN = new Color(0x00FFFF);
  public static final Color BLUE = new Color(0x0000FF);

  public enum Mode {
    RGB,
    HSL,
    OKHSL
  }
}
