package com.hexedrealms.configurations;

public enum PlayerMotionConfiguration {
    FRICTION_DAMPING(0.85f),
    JUMP_IMPULSE(1700f),
    CLIMB_NORMAL_THRESHOLD(0.5f),
    CORNER_CUT_THRESHOLD(0.75f),
    CORNER_CUT_SMOOTHING(0.2f),
    AIR_CONTROL_FACTOR (0.15f),

    // BHop параметры
    HOP_SPEED_BOOST(1.5f),
    HOP_IMPULSE(8f),
    HOP_DURATION(0.8f),
    AIR_CONTROL(0.3f),
    MAX_BHOP_SPEED(50f),
    BHOPSPEED_MAINTAIN(0.95f),

    // Другие параметры
    SLOPE_STICK_FORCE(50f),
    SLOPE_FRICTION(1.5f),
    DEFAULT_FRICTION(0.5f);

    private final float value;

    PlayerMotionConfiguration(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }
}
