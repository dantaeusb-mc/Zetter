package me.dantaeusb.zetter.painting.parameters;

/**
 * How much water the tool is carrying, none to soaked
 */
public interface WetnessParameterHolder {
    String PARAMETER_CODE = "Wetness";

    float getWetness();

    void setWetness(float wetness);
}
