package com.hexedrealms.components;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class CustomAttribute extends Attribute {
    public static final String ShininessAlias = "shininess";
    public static final long Shininess = register(ShininessAlias);

    public static FloatAttribute createShininess (float value) {
        return new FloatAttribute(Shininess, value);
    }

    public static final String AlphaTestAlias = "alphaTest";
    public static final long AlphaTest = register(AlphaTestAlias);

    public static FloatAttribute createAlphaTest (float value) {
        return new FloatAttribute(AlphaTest, value);
    }

    public static final String BrightnessAlias = "brightness";
    public static final long Brightness = register(BrightnessAlias);

    public static FloatAttribute createBrighntess (float value) {
        return new FloatAttribute(AlphaTest, value);
    }

    public float value;

    public CustomAttribute (long type) {
        super(type);
    }

    public CustomAttribute (long type, float value) {
        super(type);
        this.value = value;
    }

    @Override
    public Attribute copy () {
        return new FloatAttribute(type, value);
    }

    @Override
    public int hashCode () {
        int result = super.hashCode();
        result = 977 * result + NumberUtils.floatToRawIntBits(value);
        return result;
    }

    @Override
    public int compareTo (Attribute o) {
        if (type != o.type) return (int)(type - o.type);
        final float v = ((FloatAttribute)o).value;
        return MathUtils.isEqual(value, v) ? 0 : value < v ? -1 : 1;
    }
}
