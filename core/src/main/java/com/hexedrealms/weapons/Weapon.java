package com.hexedrealms.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.DecalAnimation;
import com.hexedrealms.components.bulletbodies.BladeBody;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.components.bulletbodies.TriggerBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.DamageComponent;
import com.hexedrealms.engine.ItemsComponent;
import com.hexedrealms.engine.PhysicComponent;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.utils.damage.Enemy;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Weapon implements Disposable {

    private SoundBuffer [] enemyHit;
    private BladeBody blade;
    private Vector3 bladePosition = new Vector3();

    protected TextureRegion icon;
    protected TextureAtlas atlas;
    protected DamageType damageType;
    protected Array<TextureAtlas.AtlasRegion> shoot;
    protected DecalAnimation mAnimation;
    protected TextureRegion mCurrentTexture, mHitTexture;
    protected SoundBuffer sound, select, hit;
    protected float basicDamage;
    protected int mCurrentFrame, xOffsetCustom, yOffsetCustom;
    protected float speedChangeFrame, scaleViewed, prevScale, mScaleRay;
    protected boolean isShooting;
    protected String name, nameObject;


    public Weapon(String filename, String soundOfShoot, String soundOfSelect, String soundOfHit, String hitTexture,
                  float speedChangeFrame, float scaleViewed, float scaleRay, DamageType damageType, float basicDamage){

        atlas = new TextureAtlas(Gdx.files.internal(filename));
        shoot = atlas.findRegions("shoot");

        this.speedChangeFrame = speedChangeFrame;
        this.scaleViewed = scaleViewed;
        this.prevScale = scaleViewed;
        this.mScaleRay = scaleRay;
        this.damageType = damageType;
        this.basicDamage = basicDamage;

        mCurrentTexture = shoot.get(0);
        mAnimation = new DecalAnimation(shoot.size);

        sound = SoundLoader.load(Gdx.files.internal(soundOfShoot));
        select = SoundLoader.load(Gdx.files.internal(soundOfSelect));
        enemyHit = new SoundBuffer[5];
        for(int i = 1; i <= enemyHit.length; i++) {
            enemyHit[i-1] = SoundLoader.load(Gdx.files.internal("audio/sounds/weapons/sword_enemy_hit"+i+".wav"));
        }

        if(soundOfHit != null && !soundOfHit.isEmpty())
            hit = SoundLoader.load(Gdx.files.internal(soundOfHit));
        if(hitTexture != null && !hitTexture.isEmpty())
            mHitTexture = new TextureRegion(new Texture(hitTexture));
        if(this instanceof Weapon) {
            ObjLoader loader = new ObjLoader();
            blade = PhysicComponent.getInstance().configureBlade(new ModelInstance(loader.loadModel(Gdx.files.internal("models/blade.obj"))));
        }
    }

    public boolean isStatic(){
        return !isShooting;
    }

    public void playSelect(){
        select.play(AudioConfiguration.SOUND.getValue());
    }

    public void setName(String name){
        this.name = name;
    }
    public void setIcon(String name){
        nameObject = name;
        icon = ItemsComponent.getInstance().findRegion(name);
    }

    public String getNameObject(){
        return nameObject;
    }

    public TextureRegion getIcon(){
        return icon;
    }

    public void Shoot(Camera camera){
        if(!isShooting) {
            sound.play(AudioConfiguration.SOUND.getValue());
            bladePosition.set(camera.position.x, camera.position.y-0.5f,camera.position.z);

            // Получение направления камеры
            Vector3 cameraDirection = camera.direction.cpy().nor();

            if (blade != null) {
                // Создаем матрицу трансформации
                Matrix4 transform = new Matrix4();

                // Вычисляем углы Эйлера
                float pitch = (float)Math.asin(-cameraDirection.y);
                float yaw = (float)Math.atan2(cameraDirection.x,cameraDirection.z);

                // Создаем кватернион из углов Эйлера
                Quaternion rotation = new Quaternion();
                rotation.setEulerAngles(
                    (float)Math.toDegrees(yaw),
                    (float)Math.toDegrees(pitch),
                    0
                );

                // Устанавливаем позицию и поворот
                transform.set(bladePosition, rotation);
                blade.setCenterOfMassTransform(transform);

                HashMap<btCollisionObject, Vector3> collisions = PhysicComponent.getInstance().consumeCollision(blade);

                if(!collisions.isEmpty()) {
                    collisions.keySet().forEach(key -> {
                        if (key instanceof Enemy) {
                            enemyHit[ThreadLocalRandom.current().nextInt(0, enemyHit.length)].play3D(AudioConfiguration.SOUND.getValue(), collisions.get(key));
                            Enemy enemy = (Enemy) key;
                            enemy.setPositionHit(collisions.get(key));
                            checkDamage(enemy);

                        } else
                            hit.play3D(AudioConfiguration.SOUND.getValue(), collisions.get(key));

                        if(key instanceof TriggerBody){
                            TriggerBody triggerBody = (TriggerBody) key;
                            triggerBody.activateTrigger();
                            triggerBody.disposeTrigger();
                        }
                    });
                }

                PhysicComponent.getInstance().removeRigidBody(blade);
            }
        }

        isShooting = true;
        mAnimation.startAnimation();
    }

    protected void checkDamage(Enemy enemy){
        if(enemy == null)
            return;

        PlayerBody playerBody = (PlayerBody) PhysicComponent.getInstance().getPlayerBody();
        playerBody.setBasicDamage(basicDamage);
        DamageComponent.getInstance().checkDamage(playerBody, enemy, damageType);
    }

    public void addCustomOffset(int x, int y){
        xOffsetCustom = x;
        yOffsetCustom = y;
    }

    public int getxOffsetCustom(){
        return xOffsetCustom;
    }

    public int getyOffsetCustom(){
        return yOffsetCustom;
    }

    public void Render(float delta){
        if(isShooting){
            mAnimation.UpdateDecal(delta * speedChangeFrame);
            mCurrentFrame = mAnimation.getFrame();
            mCurrentTexture = shoot.get(mCurrentFrame);

            if(mAnimation.isFinalled())
                isShooting = false;
        }
    }

    public void stopAnimation(){
        mCurrentFrame = 0;
        mCurrentTexture = shoot.get(mCurrentFrame);
        mAnimation.stopAnimation();
        isShooting = false;
    }

    public float getScaleViewed() {
        return scaleViewed;
    }

    public boolean isShooting() {
        return isShooting;
    }

    public TextureRegion getFrame(){
        return mCurrentTexture;
    }

    @Override
    public void dispose() {
        shoot.forEach(s ->{
           s.getTexture().dispose();
        });

        mCurrentTexture.getTexture().dispose();
        sound.dispose();
        select.dispose();
        atlas.dispose();

        shoot.clear();
        shoot = null;
        mAnimation = null;
        mCurrentTexture = null;
        sound = null;
        select = null;
        atlas = null;
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return "Weapon{" +
            "enemyHit=" + Arrays.toString(enemyHit) +
            ", blade=" + blade +
            ", bladePosition=" + bladePosition +
            ", atlas=" + atlas +
            ", damageType=" + damageType +
            ", shoot=" + shoot +
            ", mAnimation=" + mAnimation +
            ", mCurrentTexture=" + mCurrentTexture +
            ", mHitTexture=" + mHitTexture +
            ", sound=" + sound +
            ", select=" + select +
            ", hit=" + hit +
            ", basicDamage=" + basicDamage +
            ", mCurrentFrame=" + mCurrentFrame +
            ", xOffsetCustom=" + xOffsetCustom +
            ", yOffsetCustom=" + yOffsetCustom +
            ", speedChangeFrame=" + speedChangeFrame +
            ", scaleViewed=" + scaleViewed +
            ", prevScale=" + prevScale +
            ", mScaleRay=" + mScaleRay +
            ", isShooting=" + isShooting +
            '}';
    }
}
