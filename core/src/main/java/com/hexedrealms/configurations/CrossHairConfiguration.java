package com.hexedrealms.configurations;


import com.badlogic.gdx.graphics.Color;

public enum CrossHairConfiguration {
    DISTANCE(10f),
    SIZE(15f),
    THICKNESS(2f),
    COLOR(new Color(Color.WHITE));

    private Object value;

    CrossHairConfiguration(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
    public void setValue(Object value){ this.value = value; }
}
