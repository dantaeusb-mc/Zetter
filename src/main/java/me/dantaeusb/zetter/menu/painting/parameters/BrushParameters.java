package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;

import java.security.InvalidParameterException;

public class BrushParameters extends AbstractToolParameters implements IntensityInterface, BlendingInterface, DitheringInterface {
    private float size = 1f;
    private float intensity = 1f;
    private BlendingPipe.BlendingOption blending;
    private DitheringPipe.DitheringOption dithering;

    public BrushParameters() {
        this(1f, 1f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DEFAULT);
    }

    public BrushParameters(float size, float intensity, BlendingPipe.BlendingOption blending, DitheringPipe.DitheringOption dithering) {
        this.size = size;
        this.intensity = intensity;
        this.blending = blending;
        this.dithering = dithering;
    }

    public float getSize() {
        return this.size;
    }

    public void setSize(float size) {
        if (size < 1f || size > 5f) {
            throw new InvalidParameterException("Incorrect size");
        }

        this.size = size;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        if (intensity < 0f || intensity > 1f) {
            throw new InvalidParameterException("Intensity out of bounds");
        }

        this.intensity = intensity;
    }

    public BlendingPipe.BlendingOption getBlending() {
        return blending;
    }

    public void setBlending(BlendingPipe.BlendingOption blending) {
        this.blending = blending;
    }

    public DitheringPipe.DitheringOption getDithering() {
        return dithering;
    }

    public void setDithering(DitheringPipe.DitheringOption dithering) {
        this.dithering = dithering;
    }
}
