package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;

import java.security.InvalidParameterException;

public class PencilParameters extends AbstractToolParameters implements IntensityInterface, BlendingInterface, DitheringInterface {

    private int size;

    private float intensity;

    private BlendingPipe.BlendingOption blending;
    private DitheringPipe.DitheringOption dithering;

    public PencilParameters() {
        this(1, 1f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DEFAULT);
    }

    public PencilParameters(int size, float intensity, BlendingPipe.BlendingOption blending, DitheringPipe.DitheringOption dithering) {
        this.size = size;
        this.intensity = intensity;
        this.blending = blending;
        this.dithering = dithering;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        if (size < 1 || size > 5) {
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
