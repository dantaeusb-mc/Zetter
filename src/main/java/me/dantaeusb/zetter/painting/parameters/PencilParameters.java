package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;

import java.security.InvalidParameterException;

public class PencilParameters extends AbstractToolParameters implements SizeInterface, IntensityInterface, BlendingInterface, DitheringInterface {
    public static final float MIN_SIZE = 1f;
    public static final float MAX_SIZE = 6f;

    public PencilParameters() {
        this(1f, 1f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DEFAULT);
    }

    public PencilParameters(float size, float intensity, BlendingPipe.BlendingOption blending, DitheringPipe.DitheringOption dithering) {
        this.values.put("size", size);
        this.values.put("intensity", intensity);
        this.values.put("blending", blending.name());
        this.values.put("dithering", dithering.name());
    }

    public float getSize() {
        return (float) this.values.get("size");
    }

    public void setSize(float size) {
        if (size < 1f || size > 6f) {
            throw new InvalidParameterException("Incorrect size");
        }

        this.values.put("size", size);
    }

    public float getIntensity() {
        return (float) this.values.get("intensity");
    }

    public void setIntensity(float intensity) {
        if (intensity < 0f || intensity > 1f) {
            throw new InvalidParameterException("Intensity out of bounds");
        }

        this.values.put("intensity", intensity);
    }

    public BlendingPipe.BlendingOption getBlending() {
        return BlendingPipe.BlendingOption.valueOf((String) this.values.get("blending"));
    }

    public void setBlending(BlendingPipe.BlendingOption blending) {
        this.values.put("blending", blending.name());
    }

    public DitheringPipe.DitheringOption getDithering() {
        return DitheringPipe.DitheringOption.valueOf((String) this.values.get("dithering"));
    }

    public void setDithering(DitheringPipe.DitheringOption dithering) {
        this.values.put("dithering", dithering.name());
    }
}
