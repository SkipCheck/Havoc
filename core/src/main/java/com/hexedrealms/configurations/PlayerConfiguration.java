package com.hexedrealms.configurations;

public enum PlayerConfiguration {
    PLAYER_HEIGHT(4f),
    PLAYER_WEIGHT(1f),
    MAX_SPEED(33f),
    BOOST(4f),
    ROLL_ANGLE_MAX(1.4f),
    ROLL_INCREMENT(0.24f),
    SHAKE_FREQUENCY(13f),
    SHAKE_AMPLITUDE(0.5f);

    private final float value;

    PlayerConfiguration(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }
}
