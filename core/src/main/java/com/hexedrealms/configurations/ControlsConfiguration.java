package com.hexedrealms.configurations;

import com.badlogic.gdx.Input;

public enum ControlsConfiguration {
    MOVE_FORWARD(Input.Keys.W),
    MOVE_BACKWARD(Input.Keys.S),
    MOVE_LEFT(Input.Keys.A),
    MOVE_RIGHT(Input.Keys.D),
    JUMP(Input.Keys.SPACE),
    EVENT(Input.Keys.E),
    ATACK(Input.Buttons.LEFT),
    RELOAD(Input.Keys.R),
    FAST_GUN(Input.Keys.CONTROL_LEFT),
    FAST_SAVE(Input.Keys.F5),
    FAST_LOAD(Input.Keys.F9),
    FOV(115f),
    MOUSE_SENSIVITY(0.2f);

    private Object value;
    private Object defaultKey;

    ControlsConfiguration(Object value) {
        this.value = value;
        this.defaultKey = value;
    }

    public Object getValue() {
        return value;
    }
    public Object getDefaultKey() {
        return defaultKey;
    }
    public void setValue(Object value){ this.value = value; }
}
