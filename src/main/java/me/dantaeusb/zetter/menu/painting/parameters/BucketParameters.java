package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;

import java.security.InvalidParameterException;

public class BucketParameters extends AbstractToolParameters implements IntensityInterface, BlendingInterface {
    private float intensity = 1f;
    private BlendingPipe.BlendingOption blending;

    public BucketParameters() {
        this(1f, BlendingPipe.BlendingOption.DEFAULT);
    }

    public BucketParameters(float intensity, BlendingPipe.BlendingOption blending) {
        this.intensity = intensity;
        this.blending = blending;
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
}
