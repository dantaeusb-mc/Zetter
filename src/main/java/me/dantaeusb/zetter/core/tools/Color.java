package me.dantaeusb.zetter.core.tools;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * AWT + additional functions
 * Don't use AWT to keep macs happy
 */
public class Color {
  private int argb = 0xFFFFFFFF;

  private Vector3f hsl = new Vector3f(0.0f, 1.0f, 0.5f);

  private Vector3f okHsl = new Vector3f(0.0f, 1.0f, 0.5f);

  public Color(int rgb) {
    this.argb = 0xff000000 | rgb;

    this.hsl = Color.RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue());
  }

  public Color(int r, int g, int b) {
    this(r, g, b, 0xFF);
  }

  public Color(int r, int g, int b, int a) {
    this(
        ((a & 0xFF) << 24) |
            ((r & 0xFF) << 16) |
            ((g & 0xFF) << 8) |
            ((b & 0xFF))
    );
  }

  public Color(float r, float g, float b) {
    this((int) (r * 255), (int) (g * 255), (int) (b * 255));
  }

  public Color(float r, float g, float b, float a) {
    this((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
  }

  public static Color fromHsl(float hue, float saturation, float lightness) {
    return new Color(Color.HSBtoRGB(hue, saturation, lightness));
  }

  public static Color fromOkHsl(float hue, float saturation, float lightness) {
    return new Color(Color.HSBtoRGB(hue, saturation, lightness));
  }

  public int getARGB() {
    return argb;
  }

  public float[] getRGBfloat() {
    return new float[]{this.getRed() / 255F, this.getGreen() / 255F, this.getBlue() / 255F};
  }

  public int getRed() {
    return (getARGB() >> 16) & 0xFF;
  }

  /**
   * Returns the green component in the range 0-255 in the default sRGB
   * space.
   *
   * @return the green component.
   * @see #getARGB
   */
  public int getGreen() {
    return (getARGB() >> 8) & 0xFF;
  }

  /**
   * Returns the blue component in the range 0-255 in the default sRGB
   * space.
   *
   * @return the blue component.
   * @see #getARGB
   */
  public int getBlue() {
    return (getARGB() >> 0) & 0xFF;
  }

  /**
   * Returns the alpha component in the range 0-255.
   *
   * @return the alpha component.
   * @see #getARGB
   */
  public int getAlpha() {
    return (getARGB() >> 24) & 0xff;
  }

  public Vector3f getHsl() {
    return this.hsl;
  }

  public void setHsl(Vector3f hsl) {
    this.hsl = hsl;
    // @todo: Add conversion to sRGB
  }

  public Vector3f getOkHsl() {
    return this.okHsl;
  }

  public Vector3f toHSB() {
    return Color.RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue());
  }

  public static Vector3f RGBtoHSB(int r, int g, int b) {
    float hue, saturation, brightness;
    Vector3f hsbvals = new Vector3f();
    int cmax = Math.max(r, g);
    if (b > cmax) cmax = b;
    int cmin = Math.min(r, g);
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
    hsbvals.x = hue;
    hsbvals.y = saturation;
    hsbvals.z = brightness;
    return hsbvals;
  }

  public static int HSBtoRGB(float hue, float saturation, float brightness) {
    int r = 0, g = 0, b = 0;
    if (saturation == 0) {
      r = g = b = (int) (brightness * 255.0f + 0.5f);
    } else {
      float h = (hue - (float) Math.floor(hue)) * 6.0f;
      float f = h - (float) java.lang.Math.floor(h);
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
    return 0xff000000 | (r << 16) | (g << 8) | (b);
  }

  // Color conversion helpers

  public static Vector3f okhslToSrgb(Vector3f hsl) {
    Vector3f linearRgb = okhslToLinearRgb(hsl);

    Vector3f srgb = linearRgbToSrgb(linearRgb);

    return srgb;
  }

  private static Vector3f okhslToLinearRgb(Vector3f hsl) {
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

    return oklabToLinearSrgb(new Vector3f(L, C * a, C * b));
  }

  public static Vector3f oklabToLinearSrgb(Vector3f okLab) {
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

  // Helper method to convert linear RGB to sRGB
  public static Vector3f linearRgbToSrgb(Vector3f linearRgb) {
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
    Vector3f rgbAtMax = oklabToLinearSrgb(new Vector3f(1.0f, SCusp * a, SCusp * b));
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

  private static float toeInv(float x) {
    final float K1 = 0.206f;
    final float K2 = 0.03f;
    final float K3 = (1.0f + K1) / (1.0f + K2);
    return (x * x + K1 * x) / (K3 * (x + K2));
  }

  public static final Color white = new Color(255, 255, 255);
  public static final Color WHITE = white;
  public static final Color screenGray = new Color(198, 198, 198);
  public static final Color SCREEN_GRAY = screenGray;
  public static final Color lightGray = new Color(192, 192, 192);
  public static final Color LIGHT_GRAY = lightGray;
  public static final Color gray = new Color(128, 128, 128);
  public static final Color GRAY = gray;
  public static final Color darkGray = new Color(64, 64, 64);
  public static final Color DARK_GRAY = darkGray;
  public static final Color black = new Color(0, 0, 0);
  public static final Color BLACK = black;
  public static final Color red = new Color(255, 0, 0);
  public static final Color RED = red;
  public static final Color pink = new Color(255, 175, 175);
  public static final Color PINK = pink;
  public static final Color orange = new Color(255, 200, 0);
  public static final Color ORANGE = orange;
  public static final Color yellow = new Color(255, 255, 0);
  public static final Color YELLOW = yellow;
  public static final Color green = new Color(0, 255, 0);
  public static final Color GREEN = green;
  public static final Color magenta = new Color(255, 0, 255);
  public static final Color MAGENTA = magenta;
  public static final Color cyan = new Color(0, 255, 255);
  public static final Color CYAN = cyan;
  public static final Color blue = new Color(0, 0, 255);
  public static final Color BLUE = blue;
}
