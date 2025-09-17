package com.hexedrealms.engine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.hexedrealms.components.DynamicLight;
import com.hexedrealms.components.LinearLight;
import com.hexedrealms.screens.Level;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

import java.util.concurrent.atomic.AtomicReference;

public class LightingComponent implements Disposable {

    private static LightingComponent instance;
    private final Pool<PointLightEx> pointLightPool;
    private final Array<PointLightEx> pointLightArray;
    private final Array<DynamicLight> dynamicLights;

    private LightingComponent(){
        pointLightPool = Pools.get(PointLightEx.class);
        pointLightArray = new Array<>();
        dynamicLights = new Array<>();
    }

    public Pool<PointLightEx> getPointLightPool(){
        return pointLightPool;
    }

    public Array<PointLightEx> getPointLightArray(){
        return pointLightArray;
    }

    public void putLight(PointLightEx pointLight){
        pointLightArray.add(pointLight);

        if(pointLight instanceof LinearLight) {
            dynamicLights.add((LinearLight) pointLight);
        }
        else if(pointLight instanceof DynamicLight)
            dynamicLights.add((DynamicLight) pointLight);
    }

    public void removeLight(PointLightEx pointLight){
        pointLightArray.removeValue(pointLight, true);
        pointLightPool.free(pointLight);
    }

    public void update(float delta){
        dynamicLights.forEach(dynamicLight -> {
             dynamicLight.update(delta);
        });
    }

    public static LightingComponent getInstance() {
        if(instance == null)
            instance = new LightingComponent();
        return instance;
    }

    @Override
    public void dispose() {

        pointLightArray.clear();

        // Clear dynamic lights (they're already in pointLightArray)
        dynamicLights.clear();

        // Clear the pool
        Pools.free(pointLightPool);

        // Reset the singleton instance
        instance = null;
    }
}
