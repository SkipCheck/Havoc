package com.hexedrealms.engine;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.DecalEntity;

import java.io.Serializable;

public class DecalComponent implements Disposable, Serializable {
    private static final long serialVersionUID = 1L; // контроль версии

    private static DecalComponent instance;
    private Pool<DecalEntity> decalPools;
    private Array<DecalEntity> decals;

    private DecalComponent(Camera camera){
        decals = new Array<>();
        decalPools = new Pool<DecalEntity>() {
            @Override
            protected DecalEntity newObject() {
                return new DecalEntity();
            }
        };
    }


    public static DecalComponent getInstance(Camera camera){
        if(instance == null)
            instance = new DecalComponent(camera);
        return instance;
    }

    public Pool<DecalEntity> getPool(){
        return decalPools;
    }

    public int getCount(){
        return decals.size;
    }

    public void putDecal(DecalEntity decal){
        decals.add(decal);
    }

    public void putDecal(Quaternion quaternion, Vector3 position, TextureRegion region){

        DecalEntity entity = decalPools.obtain();
        entity.init(quaternion, position, region);

        decals.add(entity);
        if(decals.size > 50) {
            decalPools.free(decals.get(0));
            decals.removeIndex(0);
        }
    }

    public void render(float delta, Frustum frustum){
        if(decals.isEmpty()) return;
        for (DecalEntity decal : decals)
            decal.Render(EntityComponent.getInstance(null).getDecalBatch(), delta, frustum);
    }

    @Override
    public void dispose() {
        for(DecalEntity decal : decals){
            decal.dispose();
        }
        decalPools.clear();
        decals.clear();

        decalPools = null;
        decals = null;
        instance = null;
    }
}
