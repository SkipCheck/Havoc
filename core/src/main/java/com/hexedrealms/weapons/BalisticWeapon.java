package com.hexedrealms.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.hexedrealms.components.BallisticBullet;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.BallisticComponent;
import com.hexedrealms.utils.damage.DamageType;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;

public class BalisticWeapon extends Weapon{
    protected Color shootColor;
    protected Array<TextureAtlas.AtlasRegion> regions;

    protected Vector3 position;
    protected Vector2 shaking;
    protected String nameEffect, nameEndEffect;
    protected SoundBuffer soundBullet;
    protected int maxBullets, currentBullets, shopBullets;
    protected boolean isRiffle;
    protected float speedBullet, radius;

    public BalisticWeapon(String filename, String soundOfShoot, String soundOfSelect, String soundBullet, float speedChangeFrame, float scaleViewed, int maxBullets,
                          Color shootColor, String group, String nameEffect, String nameEndEffect, DamageType damageType, float basicDamage, float speedBullet,
                          int currentBullets, Vector2 shaking, boolean isRiffle, float radius) {

        super(filename, soundOfShoot, soundOfSelect, null, null, speedChangeFrame, scaleViewed, 0, damageType, basicDamage);

        this.shaking = shaking;
        this.speedBullet = speedBullet;
        this.maxBullets = maxBullets;
        this.currentBullets = currentBullets;
        this.shootColor = shootColor;
        this.nameEffect = nameEffect;
        this.isRiffle = isRiffle;
        this.radius = radius;
        this.nameEndEffect = nameEndEffect;
        this.soundBullet = SoundLoader.load(Gdx.files.internal(soundBullet));

        regions = atlas.findRegions(group);

    }

    public void setCurrentBullets(int count){
        currentBullets = count;
    }

    public Vector2 getDurationShaker() {
        return shaking;
    }

    public boolean isRiffle() {
        return isRiffle;
    }

    public void addBullets(int count){
        currentBullets = Math.min(currentBullets + count, maxBullets);
    }

    public int getMaxBullets() {
        return maxBullets;
    }

    public float getPercent() {
        return (float) currentBullets / maxBullets;
    }

    public void Shoot(Camera camera){
        if(currentBullets > 0) {
            if (!isShooting) {
                sound.play(AudioConfiguration.SOUND.getValue());
            }

            isShooting = true;
            mAnimation.startAnimation();
            currentBullets--;

            BallisticBullet bullet = BallisticComponent.getInstance(null).getBulletPool().obtain();
            bullet.addEffects(nameEffect, nameEndEffect);
            bullet.setShaking(shaking);
            bullet.init(camera.position.cpy().add(0,-0.5f,0).add(camera.direction.cpy().scl(1.02f)), camera.direction.cpy(), soundBullet, 80f * speedBullet, radius, basicDamage, damageType);
            BallisticComponent.getInstance(null).addBallisticBullet(bullet);
        }
    }

    public void Render(float delta){
        super.Render(delta);
    }

    public int getCurrentBullets(){
        return currentBullets;
    }

    public Color getShootColor(){
        return shootColor;
    }
}
