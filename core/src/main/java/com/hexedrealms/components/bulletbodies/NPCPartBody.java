package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.Entity;
import com.hexedrealms.engine.EntityComponent;
import com.hexedrealms.engine.ParticlesComponent;
import com.hexedrealms.engine.PhysicComponent;

public class NPCPartBody extends btRigidBody implements Pool.Poolable {
    public static float MAX_LIVE = 4f;
    private Entity entity;
    private ParticleEffect effect;
    private float timer;
    private float width, height;

    public NPCPartBody(btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }

    public void init(btBoxShape shape, TextureRegion region, Vector3 position, ParticleEffect effect, float width, float height){
        setCollisionShape(shape);
        translate(position);

        this.width = width;
        this.height = height;
        this.effect = effect;

        entity = EntityComponent.getInstance(null).getPool().obtain();
        entity.init(region, position, getOrientation(), false);
        EntityComponent.getInstance(null).addEntity(entity);
    }

    private void remove(){
        ParticlesComponent.getInstance(null).removeParticle(effect);
        EntityComponent.getInstance(null).removeEntity(entity);
        PhysicComponent.getInstance().disposeRigidBody(this);
    }

    public void update(float delta){
        entity.setPosition(getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain()), width);
        effect.setTransform(getWorldTransform());

        if(getLinearVelocity().len() < 12f)
            timer += delta;

        timer += delta;

        if(timer > MAX_LIVE)
            remove();
    }

    @Override
    public void reset() {
        entity = null;
        effect = null;
        timer = 0;
        width = height = 0;
        setWorldTransform(new Matrix4());
    }
}
