package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;

import java.security.InvalidParameterException;

public class BrushParameters extends AbstractToolParameters implements SizeParameterHolder, IntensityParameterHolder, BlendingParameterHolder, DitheringParameterHolder {
    public static final float MIN_SIZE = 1f;
    public static final float MAX_SIZE = 6f;

    public BrushParameters() {
        this(3f, 1f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DEFAULT);
    }

    public BrushParameters(Float size, Float intensity, BlendingPipe.BlendingOption blending, DitheringPipe.DitheringOption dithering) {
        this.values.put(SizeParameterHolder.PARAMETER_CODE, size);
        this.values.put(IntensityParameterHolder.PARAMETER_CODE, intensity);
        this.values.put(BlendingParameterHolder.PARAMETER_CODE, blending.name());
        this.values.put(DitheringParameterHolder.PARAMETER_CODE, dithering.name());
    }

    public float getSize() {
        return (float) this.values.get(SizeParameterHolder.PARAMETER_CODE);
    }

    public void setSize(float size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidParameterException("Incorrect size");
        }

        this.values.put(SizeParameterHolder.PARAMETER_CODE, size);
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

    public DitheringPipe.DitheringOption getDithering() {
        return DitheringPipe.DitheringOption.valueOf((String) this.values.get(DitheringParameterHolder.PARAMETER_CODE));
    }

    public void setDithering(DitheringPipe.DitheringOption dithering) {
        this.values.put(DitheringParameterHolder.PARAMETER_CODE, dithering.name());
    }
}
