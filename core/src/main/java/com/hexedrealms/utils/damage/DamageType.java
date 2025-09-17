package com.hexedrealms.utils.damage;

import java.io.Serializable;

public enum DamageType implements Serializable {

    PHYSICAL(0.1f, 0.05f),
    MAGICAL(0.15f, 0.1f);

    private float dmgCoefficient;
    private float critRatio;

    DamageType(float dmgCoefficient, float critRatio){
        this.dmgCoefficient = dmgCoefficient;
        this.critRatio = critRatio;
    }

    public float getCritRatio() {
        return critRatio;
    }

    public float getDmgCoefficient() {
        return dmgCoefficient;
    }
    private static final long serialVersionUID = 1L;
}
