package me.dantaeusb.zetter.painting.parameters;

import java.security.InvalidParameterException;

public class SpongeParameters extends AbstractToolParameters implements SizeParameterHolder, WetnessParameterHolder {
    public static final float MIN_SIZE = 4f;
    public static final float MAX_SIZE = 20f;

    public static final float DEFAULT_SIZE = 12f;

    public SpongeParameters() {
        this(DEFAULT_SIZE, 1f);
    }

    public SpongeParameters(Float size, Float wetness) {
        this.values.put(SizeParameterHolder.PARAMETER_CODE, size);
        this.values.put(WetnessParameterHolder.PARAMETER_CODE, wetness);
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

    public float getWetness() {
        return Math.min(1f, Math.max(0f, (float) this.values.get(WetnessParameterHolder.PARAMETER_CODE)));
    }

    public void setWetness(float wetness) {
        if (wetness < 0f || wetness > 1f) {
            throw new InvalidParameterException("Wetness out of bounds");
        }

        this.values.put(WetnessParameterHolder.PARAMETER_CODE, wetness);
    }
}
