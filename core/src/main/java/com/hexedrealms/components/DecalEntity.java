package com.hexedrealms.components;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

public class DecalEntity implements Pool.Poolable, Disposable {

    protected TextureRegion mRegion;
    protected Decal mDecal;
    protected Array<TextureAtlas.AtlasRegion> regions;
    protected DecalAnimation animation;
    protected Renderable renderable;
    protected float speed;
    public DecalEntity(){
        mRegion = new TextureRegion();
        mDecal = Decal.newDecal(1f, 1f, mRegion, true);
    }

    public void init(Quaternion quaternion, Vector3 pPosition, TextureRegion texture){

        mRegion.setRegion(texture);
        mDecal.setTextureRegion(mRegion);
        mDecal.setPosition(pPosition);
        mDecal.setRotation(quaternion);
    }

    public void init(Vector3 direction, Vector3 pPosition, Array<TextureAtlas.AtlasRegion> regions, float speed){
        this.regions = regions;
        this.speed = speed;

        animation = new DecalAnimation(regions.size);
        mRegion = regions.get(0);
        mDecal = Decal.newDecal(1f, 1f, mRegion, true);
        mDecal.setPosition(pPosition);
        mDecal.setRotation(direction, Vector3.Y);
    }

    public void reset(){
        mRegion = new TextureRegion();
        mDecal = Decal.newDecal(1f, 1f, mRegion, true);
        regions = null;
        animation = null;
    }

    public void Render(DecalBatch batch, float delta, Frustum frustum){

        if(regions != null && !regions.isEmpty()){
            animation.UpdateDecal(delta * speed);
            mRegion = regions.get(animation.getFrame());
            mDecal.setTextureRegion(mRegion);
        }

        if(frustum.pointInFrustum(mDecal.getPosition()))
            batch.add(mDecal);
    }

    @Override
    public void dispose() {
        // Just clear references without disposing textures that might be shared
        mDecal = null;
        mRegion = null;

        if (regions != null) {
            regions.clear();
            regions = null;
        }

        animation = null;
        renderable = null;
    }
}
