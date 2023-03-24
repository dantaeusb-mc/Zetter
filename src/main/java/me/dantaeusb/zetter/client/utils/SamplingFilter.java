package me.dantaeusb.zetter.client.utils;
import java.nio.ByteBuffer;

public class SamplingFilter {
    // Calculate the sinc function
    private static double sinc(double x) {
        if (x == 0) {
            return 1.0;
        }
        return Math.sin(Math.PI * x) / (Math.PI * x);
    }

    // Calculate the lanczos function
    private static double lanczos(double x, double a) {
        if (x == 0) {
            return 1.0;
        }
        if (Math.abs(x) >= a) {
            return 0.0;
        }
        return sinc(x) * sinc(x / a);
    }

    // Resample the image using the sinc/lanczos algorithm
    public static BufferedImage resample(ByteBuffer image, int width, int height, double a) {
        BufferedImage input = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, image.width(), image.height(), image.array(), 0, image.width());

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = x * 1.0 / width * input.getWidth();
                double v = y * 1.0 / height * input.getHeight();

                double r = 0, g = 0, b = 0, wsum = 0;

                int x0 = Math.max(0, (int) Math.floor(u - a));
                int x1 = Math.min(input.getWidth() - 1, (int) Math.ceil(u + a));
                int y0 = Math.max(0, (int) Math.floor(v - a));
                int y1 = Math.min(input.getHeight() - 1, (int) Math.ceil(v + a));

                for (int j = y0; j <= y1; j++) {
                    for (int i = x0; i <= x1; i++) {
                        double dx = u - i;
                        double dy = v - j;
                        double w = lanczos(dx, a) * lanczos(dy, a);
                        int rgb = input.getRGB(i, j);
                        double r1 = (rgb >> 16) & 0xFF;
                        double g1 = (rgb >> 8) & 0xFF;
                        double b1 = rgb & 0xFF;
                        r += r1 * w;
                        g += g1 * w;
                        b += b1 * w;
                        wsum += w;
                    }
                }

                if (wsum > 0) {
                    int nr = Math.min(Math.max((int) Math.round(r / wsum), 0), 255);
                    int ng = Math.min(Math.max((int) Math.round(g / wsum), 0), 255);
                    int nb = Math.min(Math.max((int) Math.round(b / wsum), 0), 255);
                    output.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
                } else {
                    output.setRGB(x, y, input.getRGB((int) u, (int) v));
                }
            }
        }

        return output;
    }
}
