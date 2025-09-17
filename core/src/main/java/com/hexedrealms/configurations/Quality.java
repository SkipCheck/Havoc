package com.hexedrealms.configurations;

public enum Quality {
    LOW_QUALITY(0.75f),
    MIDDLE_QUALITY(0.85f),
    HIGH_QUALITY(1f);

    private final float value;

    Quality(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }
}
