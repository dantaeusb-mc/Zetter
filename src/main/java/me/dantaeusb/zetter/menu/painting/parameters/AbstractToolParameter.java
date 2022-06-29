package me.dantaeusb.zetter.menu.painting.parameters;

public abstract class AbstractToolParameter<T> {
    // Parameter name
    public final String code;

    // Parameter value
    public final T value;

    public AbstractToolParameter(String code, T value) {
        this.code = code;
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }
}
