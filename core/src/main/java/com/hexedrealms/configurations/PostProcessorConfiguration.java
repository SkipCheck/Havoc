package com.hexedrealms.configurations;

public enum PostProcessorConfiguration {
    BLOOM(true),
    MOTION_BLUR(false),
    QUALITY(2),
    BRIGHTNESS(0f);

    private Object value;
    private Object defaultValue;

    PostProcessorConfiguration(Object value) {
        this.value = value;
        this.defaultValue = value;
    }

    public Object getValue() {
        return value;
    }
    public void setValue(Object value){ this.value = value; }
    public Object getDefaultValue(){
        return defaultValue;
    }
}
