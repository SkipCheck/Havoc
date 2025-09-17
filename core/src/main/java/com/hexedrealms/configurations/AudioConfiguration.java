package com.hexedrealms.configurations;

public enum AudioConfiguration {
    SOUND(0.5f),
    MUSIC(0.5f),
    COMMON(1f);

    private float value;

    AudioConfiguration(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }
    public void setValue(float value){ this.value = value; }
}
