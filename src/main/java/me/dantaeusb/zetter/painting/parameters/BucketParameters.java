package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;

import java.security.InvalidParameterException;

public class BucketParameters extends AbstractToolParameters implements IntensityParameterHolder, BlendingParameterHolder {
    public BucketParameters() {
        this(1f, BlendingPipe.BlendingOption.DEFAULT);
    }

    public BucketParameters(float intensity, BlendingPipe.BlendingOption blending) {
        this.values.put(IntensityParameterHolder.PARAMETER_CODE, intensity);
        this.values.put(BlendingParameterHolder.PARAMETER_CODE, blending.name());
    }

    public float getIntensity() {
        return (float) this.values.get(IntensityParameterHolder.PARAMETER_CODE);
    }

    public void setIntensity(float intensity) {
        if (intensity < 0f || intensity > 1f) {
            throw new InvalidParameterException("Intensity out of bounds");
        }

        this.values.put(IntensityParameterHolder.PARAMETER_CODE, intensity);
    }

    public BlendingPipe.BlendingOption getBlending() {
        return BlendingPipe.BlendingOption.valueOf((String) this.values.get(BlendingParameterHolder.PARAMETER_CODE));
    }

    public void setBlending(BlendingPipe.BlendingOption blending) {
        this.values.put(BlendingParameterHolder.PARAMETER_CODE, blending.name());
    }
}
