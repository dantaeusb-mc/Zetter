package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;

import java.security.InvalidParameterException;

public class BucketParameters extends AbstractToolParameters implements IntensityInterface, BlendingInterface {
    public BucketParameters() {
        this(1f, BlendingPipe.BlendingOption.DEFAULT);
    }

    public BucketParameters(float intensity, BlendingPipe.BlendingOption blending) {
        this.values.put("intensity", intensity);
        this.values.put("blending", blending.name());
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
}
