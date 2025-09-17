package com.hexedrealms.configurations;

public enum GraphicConfiguration {
    DIMENSION(0),
    FREQUENCY(0),
    VSYNC(true),
    FPS(0);

    private Object value;
    private Object defaultValue;

    GraphicConfiguration(Object value) {
        this.value = value;
        this.defaultValue = value;
    }

    public Object getValue() {
        return value;
    }
    public Object getDefaultValue(){ return defaultValue; }
    public void setValue(Object value){ this.value = value; }
}
