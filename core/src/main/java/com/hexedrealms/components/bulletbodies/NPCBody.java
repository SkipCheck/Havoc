package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.hexedrealms.utils.damage.Enemy;

public class NPCBody extends btRigidBody implements Enemy {

    private int maxHealth;
    private int currentHealth;
    private int previousHealth;
    private float maxArmor;
    private float strength;
    private float agility;
    private float intelegence;
    private float vitality;
    private float basicDamage;
    private float height;
    private float width;
    private boolean damaged;
    private Vector3 positionHit;
    private Array<TextureAtlas.AtlasRegion> regions;

    public NPCBody(btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
    @Override
    public void setArmor(float armor) {
        this.maxArmor = armor;
    }

    @Override
    public void setHealth(int health) {
        this.maxHealth = health;
        this.currentHealth = health;
    }

    @Override
    public void setStrength(float strength) {
        this.strength = strength;
    }

    @Override
    public void setAgility(float agility) {
        this.agility = agility;
    }

    @Override
    public void setIntelligence(float intelligence) {
        this.intelegence = intelligence;
    }

    @Override
    public void setVitality(float vitality) {
        this.vitality = vitality;
    }

    @Override
    public void setBasicDamage(float damage) {
        this.basicDamage = damage;
    }

    @Override
    public void setPositionHit(Vector3 positionHit) {
        this.positionHit = positionHit;
    }

    @Override
    public void setHeight(float height) {
        this.height = height;
    }

    @Override
    public void setWidth(float width) {
        this.width = width;
    }

    @Override
    public void setDamaged(boolean damaged) {
        this.damaged = damaged;
    }

    @Override
    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    @Override
    public void uploadParts(Array<TextureAtlas.AtlasRegion> atlasRegions) {
        regions = atlasRegions;
    }

    @Override
    public float addDamage(float damage) {
        currentHealth -= damage;
        return currentHealth;
    }

    @Override
    public float getArmor() {
        return maxArmor;
    }

    @Override
    public int getHealth() {
        return maxHealth;
    }

    @Override
    public float getStrength() {
        return strength;
    }

    @Override
    public float getAgility() {
        return agility;
    }

    @Override
    public float getIntelligence() {
        return intelegence;
    }

    @Override
    public float getVitality() {
        return vitality;
    }

    @Override
    public float getBasicDamage() {
        return basicDamage;
    }

    @Override
    public int getCurrentHealth() {
        return currentHealth;
    }


    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public boolean hasDamage() {
        return damaged;
    }

    @Override
    public Vector3 getPositionHit() {
        return positionHit;
    }

    @Override
    public Array<TextureAtlas.AtlasRegion> getParts() {
        return regions;
    }
}
