package me.dantaeusb.zetter.menu.painting.tools.brush;

/**
 * Using WebKit animation beziers for reference
 *
 * @see {https://chromium.googlesource.com/chromium/blink/+/master/Source/platform/animation/UnitBezier.h}
 */
public class Bezier {
    private final double ax;
    private final double bx;
    private final double cx;

    private final double ay;
    private final double by;
    private final double cy;

    private final double startGradient;
    private final double endGradient;

    public Bezier(double p1x, double p1y, double p2x, double p2y) {
        this.cx = 3.0 * p1x;
        this.bx = 3.0 * (p2x - p1x) - this.cx;
        this.ax = 1.0 - this.cx - this.bx;
        this.cy = 3.0 * p1y;
        this.by = 3.0 * (p2y - p1y) - this.cy;
        this.ay = 1.0 - this.cy - this.by;

        if (p1x > 0) {
            this.startGradient = p1y / p1x;
        } else if (p1y == 0 && p2x > 0) {
            this.startGradient = p2y / p2x;
        } else {
            this.startGradient = 0;
        }

        if (p2x < 1) {
            this.endGradient = (p2y - 1) / (p2x - 1);
        } else if (p2x == 1 && p1x < 1) {
            this.endGradient = (p1y - 1) / (p1x - 1);
        } else {
            this.endGradient = 0;
        }
    }

    public double sampleCurveX(double t)
    {
        // `ax t^3 + bx t^2 + cx t' expanded using Horner's rule.
        return ((this.ax * t + this.bx) * t + this.cx) * t;
    }
    public double sampleCurveY(double t)
    {
        return ((this.ay * t + this.by) * t + this.cy) * t;
    }
    public double sampleCurveDerivativeX(double t)
    {
        return (3.0 * this.ax * t + 2.0 * this.bx) * t + this.cx;
    }
    // Given an x value, find a parametric value it came from.
    public double solveCurveX(double x, double epsilon)
    {
        double t0;
        double t1;
        double t2;
        double x2;
        double d2;
        int i;

        // First try a few iterations of Newton's method -- normally very fast.
        for (t2 = x, i = 0; i < 8; i++) {
            x2 = sampleCurveX(t2) - x;
            if (Math.abs(x2) < epsilon) {
                return t2;
            }

            d2 = sampleCurveDerivativeX(t2);
            if (Math.abs(d2) < 1e-6) {
                break;
            }

            t2 = t2 - x2 / d2;
        }

        // Fall back to the bisection method for reliability.
        t0 = 0.0;
        t1 = 1.0;
        t2 = x;

        while (t0 < t1) {
            x2 = sampleCurveX(t2);
            if (Math.abs(x2 - x) < epsilon) {
                return t2;
            }

            if (x > x2) {
                t0 = t2;
            } else {
                t1 = t2;
            }

            t2 = (t1 - t0) * .5 + t0;
        }

        // Failure.
        return t2;
    }

    // Evaluates y at the given x. The epsilon parameter provides a hint as to the required
    // accuracy and is not guaranteed.
    public double solve(double x, double epsilon)
    {
        if (x < 0.0) {
            return 0.0 + this.startGradient * x;
        }

        if (x > 1.0) {
            return 1.0 + this.endGradient * (x - 1.0);
        }

        return sampleCurveY(solveCurveX(x, epsilon));
    }
}
