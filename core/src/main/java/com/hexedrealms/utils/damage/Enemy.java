package com.hexedrealms.utils.damage;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public interface Enemy {
    void setArmor(float armor);
    void setHealth(int health);
    void setStrength(float strength);
    void setAgility(float agility);
    void setIntelligence(float intelligence);
    void setVitality(float vitality);
    void setBasicDamage(float damage);
    void setPositionHit(Vector3 positionHit);
    void setHeight(float height);
    void setWidth(float width);
    void setDamaged(boolean damaged);
    void setCurrentHealth(int currentHealth);
    void uploadParts(Array<TextureAtlas.AtlasRegion> atlasRegions);

    float addDamage(float damage);
    float getArmor();
    int getHealth();
    float getStrength();
    float getAgility();
    float getIntelligence();
    float getVitality();
    float getBasicDamage();
    int getCurrentHealth();
    float getHeight();
    float getWidth();
    boolean hasDamage();
    Vector3 getPositionHit();
    Array<TextureAtlas.AtlasRegion> getParts();
}
