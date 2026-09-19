package me.dantaeusb.zetter.client.gui.painting.util;

import com.mojang.blaze3d.platform.NativeImage;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Background of a slider that only accepts a limited number of values, like the
 * size of the pencil: a notch is painted for every value.
 */
public class SliderTrackTexture {
    private static final int BACKGROUND_COLOR = 0xFF8B8B8B;
    private static final int NOTCH_COLOR = 0xFF000000;

    private final ResourceLocation location;
    private final int width;
    private final int height;

    private @Nullable DynamicTexture texture;
    private int notches;

    public SliderTrackTexture(ResourceLocation location, int width, int height) {
        this.location = location;
        this.width = width;
        this.height = height;
    }

    public ResourceLocation get(int notches) {
        if (this.texture == null || this.notches != notches) {
            this.paint(notches);
        }

        return this.location;
    }

    private void paint(int notches) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, this.width, this.height, true);
        image.fillRect(0, 0, this.width, this.height, BACKGROUND_COLOR);

        for (int i = 0; i < notches; i++) {
            // Single value slider would only have a notch where it starts
            float value = notches > 1 ? (float) i / (notches - 1) : 0.0f;
            int offset = SliderWidget.getHorizontalHandlerOffset(value);

            if (offset < 0 || offset >= this.width) {
                continue;
            }

            for (int y = 0; y < this.height; y++) {
                image.setPixelRGBA(offset, y, NOTCH_COLOR);
            }
        }

        this.texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(this.location, this.texture);

        this.notches = notches;
    }
}
