package com.hexedrealms.components;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.engine.PhysicComponent;

public class Entity implements Pool.Poolable, Disposable {

    protected TextureRegion mRegion;
    protected Decal mDecal;
    protected DecalAnimation animation;
    protected Quaternion quaternion;
    protected Array<TextureAtlas.AtlasRegion> atlasRegions;
    protected BoundingBox boundingBox;
    protected btCollisionObject object;

    protected float height, width, scale = 0.1f, speed = 1f;
    protected boolean isStatic, isViewed = true, isFullRotation;
    public Entity(){
        mRegion = new TextureRegion();
        quaternion = new Quaternion();
        mDecal = Decal.newDecal(1, 1, mRegion, true);
    }

    public void init(Array<TextureAtlas.AtlasRegion> atlasRegions, Vector3 pPosition, Quaternion quaternion, boolean isRigid){
        if(atlasRegions != null) {
            this.atlasRegions = atlasRegions;
            mRegion.setRegion(atlasRegions.get(0));

            if(atlasRegions.size > 1)
                animation = new DecalAnimation(atlasRegions.size);
        }

        configureDecal(pPosition, isRigid, quaternion);
    }

    public void setFullRotation(boolean fullRotation){
        isFullRotation = fullRotation;
    }

    public void init(TextureRegion region, Vector3 pPosition, Quaternion quaternion, boolean isRigid){
        this.mRegion.setRegion(region);
        configureDecal(pPosition, isRigid, quaternion);
    }

    public void mulScale(float scale){
        this.scale *= scale;
        height = mRegion.getRegionHeight() * this.scale;
        width = (height * mRegion.getRegionWidth()) / mRegion.getRegionHeight();
        mDecal.setScale(width, height);
    }

    private void configureDecal(Vector3 pPosition, boolean isRigid, Quaternion quaternion){
        this.quaternion.set(quaternion);

        mDecal.setTextureRegion(mRegion);
        calcScale(scale);

        PhysicComponent instance = PhysicComponent.getInstance();

        boundingBox = new BoundingBox();
        boundingBox.set(
            instance.getVectorPool().obtain().set(pPosition.x - 0.3f, pPosition.y, pPosition.z - 0.3f),
            instance.getVectorPool().obtain().set(pPosition.x + 0.3f, pPosition.y + height * 0.5f, pPosition.z + 0.3f)
        );

        if(isRigid)
            object = instance.uploadEntityBody(boundingBox, pPosition);

        boundingBox.min.sub(0,height*0.5f,0);
        mDecal.setPosition(pPosition);
        mDecal.setRotation(this.quaternion);
    }


    public Vector3 getPosition(){
        return mDecal.getPosition();
    }

    public void calcScale(float scale){
        this.scale = scale;
        height = mRegion.getRegionHeight() * scale;
        width = (height * mRegion.getRegionWidth()) / mRegion.getRegionHeight();
        mDecal.setScale(width, height);
    }

    @Override
    public void reset(){
        mRegion = new TextureRegion();
        mDecal = Decal.newDecal(1, 1, mRegion, true);
        quaternion = new Quaternion();
        boundingBox = null;
        height = 0f;
        width = 0f;
        speed = 1f;
        if(atlasRegions != null)
            atlasRegions.clear();
        if(animation != null)
            animation.clearTimer();
    }

    public void setFinalled(boolean finalled){
        getAnimation().setFinalled(finalled);
    }

    public DecalAnimation getAnimation() {
        return animation;
    }

    public void setSpeed(float speed){
        this.speed = speed;
    }

    public void setStatic(boolean isStatic){
        this.isStatic = isStatic;
    }

    private void processingAnimation(float delta){
        animation.UpdateDecal(delta * speed);
        mRegion = atlasRegions.get(animation.getFrame());
        mDecal.setTextureRegion(mRegion);
    }

    public BoundingBox getBoundingBox(){
        return boundingBox;
    }

    public void setPosition(Vector3 pPosition, float radius){
        mDecal.setPosition(pPosition);
        calcScale(scale);
        // Сохраняем текущую высоту boundingBox
        float boxHeight = boundingBox.max.y - boundingBox.min.y;

        // Центрируем boundingBox относительно декала
        mDecal.setPosition(pPosition);

        // Обновляем позицию boundingBox с учетом высоты
        boundingBox.min.set(
            pPosition.x - radius * 0.5f,
            pPosition.y,
            pPosition.z - radius * 0.5f
        );

        boundingBox.max.set(
            pPosition.x + radius * 0.5f,
            pPosition.y + boxHeight,
            pPosition.z + radius * 0.5f
        );
    }

    public void setRotation(float angle){
        quaternion.setFromAxis(Vector3.Y, angle);
    }

    public float getAngle(){
        return quaternion.getAngle();
    }
    public Quaternion getQuaternion(){
        return quaternion;
    }

    public void setViewed(boolean isViewed){
        this.isViewed = isViewed;
    }

    public void render(DecalBatch batch, Camera camera, float delta, boolean isPaused){

        if(!camera.frustum.boundsInFrustum(boundingBox) || !isViewed) return;
        if(atlasRegions != null && atlasRegions.size > 1 && !isPaused) processingAnimation(delta);
        if(isStatic) return;
        if(isFullRotation) { mDecal.lookAt(camera.position, Vector3.Y); batch.add(mDecal); return; }

        Vector3 direction = camera.direction.cpy().scl(-1);
        direction.y = 0;
        float angle = MathUtils.atan2(direction.x, direction.z);
        quaternion.setEulerAngles(angle * MathUtils.radDeg, 0, 0);
        mDecal.setRotation(quaternion);

        batch.add(mDecal);
    }

    public void setRegions(Array<TextureAtlas.AtlasRegion> atlasRegions){
        if(!this.atlasRegions.equals(atlasRegions)) {
            this.atlasRegions = atlasRegions;
            animation.setSize(atlasRegions.size);
        }
    }

    public void setRegion(TextureAtlas.AtlasRegion atlasRegion) {
        mRegion.setRegion(atlasRegion);
        mDecal.setTextureRegion(mRegion);
    }

    public void setColor(Color color){
        mDecal.setColor(color);
    }

    public Array<TextureAtlas.AtlasRegion> getAtlasRegions() {
        return atlasRegions;
    }

    public Texture getTexture() {
        return mRegion.getTexture();
    }

    @Override
    public void dispose() {
        // Clear the decal
        if (mDecal != null) {
            mDecal = null; // Decals don't need explicit disposal in LibGDX
        }

        // Clear texture references
        mRegion = null;

        // Clear animation
        if (animation != null) {
            animation = null;
        }

        // Clear atlas regions
        if (atlasRegions != null) {
            atlasRegions.clear();
            atlasRegions = null;
        }

        // Clear physics object
        if (object != null) {
            PhysicComponent.getInstance().removeRigidBody(object);
            object = null;
        }

        // Clear other references
        quaternion = null;
        boundingBox = null;
    }
}
