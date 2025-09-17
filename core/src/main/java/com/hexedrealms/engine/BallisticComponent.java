package com.hexedrealms.engine;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.hexedrealms.components.BallisticBullet;

import java.io.Serializable;

public class BallisticComponent implements Disposable {

    private static BallisticComponent instance;
    private Pool<BallisticBullet> bulletPool;
    private Array<BallisticBullet> bullets;
    private Camera camera;

    private BallisticComponent(Camera camera){
        this.camera = camera;
        bullets = new Array<>();
        bulletPool = Pools.get(BallisticBullet.class);
    }

    public void addBallisticBullet(BallisticBullet bullet){
        bullets.add(bullet);
    }

    public static BallisticComponent getInstance(Camera camera) {
        if(instance == null)
            instance = new BallisticComponent(camera);
        return instance;
    }

    public Pool<BallisticBullet> getBulletPool() {
        return bulletPool;
    }

    public void Update(float delta){
        int length = bullets.size;
        for(int i = length; --i >= 0;){
            BallisticBullet bullet = bullets.get(i);
            PhysicComponent.CustomCallback customCallback = PhysicComponent.getInstance().getCallback();
            btRigidBody body = bullet.getRigidBody();

            PhysicComponent.getInstance().getPhysicWorld().contactTest(body, customCallback);
            if(bullet.update(customCallback, delta, camera.position)){
                bullets.removeIndex(i);
                bulletPool.free(bullet);
            }

            customCallback.Clear();
        }
    }

    @Override
    public void dispose() {
        // Dispose all bullets in the active array
        for (BallisticBullet bullet : bullets) {
            if (bullet != null) {
                // Dispose the bullet's rigid body if it exists
                btRigidBody body = bullet.getRigidBody();
                if (body != null) {
                    PhysicComponent.getInstance().disposeRigidBody(body);
                }
                // Return bullet to pool
                bulletPool.free(bullet);
            }
        }
        bullets.clear();

        // Clear the bullet pool
        if (bulletPool != null) {
            Pools.free(bulletPool);
            bulletPool = null;
        }

        // Clear camera reference
        camera = null;

        // Reset singleton instance
        instance = null;
    }
}
