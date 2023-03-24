package me.dantaeusb.zetter.client.utils;

import java.nio.ByteBuffer;

public class DitheringFilter {
    public static void applyFloydSteinberg(ByteBuffer data, int width, int height) {
        int[] oldPixel = new int[4];
        int[] newPixel = new int[4];
        int[] quantError = new int[4];
        int index;

        // Loop through all pixels in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                index = (y * width + x) * 4;

                // Save old pixel values
                for (int i = 0; i < 4; i++) {
                    oldPixel[i] = data.get(index + i) & 0xff;
                }

                // Calculate new pixel values after dithering
                for (int i = 0; i < 4; i++) {
                    newPixel[i] = oldPixel[i] < 128 ? 0 : 255;
                    quantError[i] = oldPixel[i] - newPixel[i];
                }

                // Apply Floyd-Steinberg dithering to neighboring pixels
                if (x < width - 1) {
                    // Right neighbor
                    index += 4;
                    for (int i = 0; i < 4; i++) {
                        data.put(index + i, (byte) ((data.get(index + i) & 0xff) + quantError[i] * 7 / 16));
                    }
                }
                if (x > 0 && y < height - 1) {
                    // Bottom-left neighbor
                    index += (width - 2) * 4;
                    for (int i = 0; i < 4; i++) {
                        data.put(index + i, (byte) ((data.get(index + i) & 0xff) + quantError[i] * 3 / 16));
                    }
                }
                if (y < height - 1) {
                    // Bottom neighbor
                    index += 4;
                    for (int i = 0; i < 4; i++) {
                        data.put(index + i, (byte) ((data.get(index + i) & 0xff) + quantError[i] * 5 / 16));
                    }
                }
                if (x < width - 1 && y < height - 1) {
                    // Bottom-right neighbor
                    index += 4;
                    for (int i = 0; i < 4; i++) {
                        data.put(index + i, (byte) ((data.get(index + i) & 0xff) + quantError[i] * 1 / 16));
                    }
                }

                // Write new pixel values
                for (int i = 0; i < 4; i++) {
                    data.put(index + i, (byte) newPixel[i]);
                }
            }
        }
    }

}
