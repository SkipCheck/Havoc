package com.hexedrealms.components;

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.bulletbodies.*;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.DamageComponent;
import com.hexedrealms.engine.ParticlesComponent;
import com.hexedrealms.engine.PhysicComponent;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.utils.damage.Enemy;
import de.pottgames.tuningfork.SoundBuffer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BallisticBullet implements Pool.Poolable {
    private btRigidBody body, sphereBody;
    private ParticleEffect targetEffect, targetEnd;
    private String effectName, effectEndName;
    private SoundBuffer soundEnd;
    private Vector2 shaking;
    private DamageType damageType;
    private float speedTranslation;
    private float basicDamage;
    private Set<Enemy> hitEnemies = new HashSet<>(); // Для отслеживания пораженных NPC

    public void addEffects(String effectName, String effectEndName) {
        this.effectName = effectName;
        this.effectEndName = effectEndName;
    }

    public void init(Vector3 position, Vector3 direction, SoundBuffer soundEnd, float speedTranslation, float radius, float basicDamage, DamageType damageType) {
        this.soundEnd = soundEnd;
        this.speedTranslation = speedTranslation;
        this.basicDamage = basicDamage;
        this.damageType = damageType;
        this.hitEnemies.clear(); // Очищаем при инициализации

        direction.nor();

        targetEffect = ParticlesComponent.getInstance(null).getEffectByName(effectName);
        targetEffect.init();
        targetEffect.start();
        targetEffect.getControllers().get(0).transform.setTranslation(position);

        body = PhysicComponent.getInstance().getPool().obtain();
        body.setWorldTransform(targetEffect.getControllers().get(0).transform);
        body.setLinearVelocity(direction);

        sphereBody = PhysicComponent.getInstance().createSphereShape(radius);

        PhysicComponent.getInstance().addRigidBody(body);
        ParticlesComponent.getInstance(null).addEffect(targetEffect);
    }

    public void setShaking(Vector2 shaking) {
        this.shaking = shaking;
    }

    public btRigidBody getRigidBody() {
        return body;
    }

    public boolean update(PhysicComponent.CustomCallback customCallback, float delta, Vector3 positionPlayer) {
        boolean isCollide = false;

        // Для магического урона NPC не являются препятствием
        if (damageType != DamageType.MAGICAL) {
            if (customCallback.getFirst() instanceof MeshBody
                || customCallback.getSecond() instanceof MeshBody
                || customCallback.getFirst() instanceof DoorBody
                || customCallback.getSecond() instanceof DoorBody
                || customCallback.getFirst() instanceof NPCBody
                || customCallback.getSecond() instanceof NPCBody) {
                isCollide = true;
            }
        } else {
            // Только твердые объекты останавливают магический снаряд
            if (customCallback.getFirst() instanceof MeshBody
                || customCallback.getSecond() instanceof MeshBody
                || customCallback.getFirst() instanceof DoorBody
                || customCallback.getSecond() instanceof DoorBody) {
                isCollide = true;
            }

            // Для магического урона наносим урон NPC при первом столкновении
            if (customCallback.getFirst() instanceof NPCBody || customCallback.getSecond() instanceof NPCBody) {
                btCollisionObject npc = customCallback.getFirst() instanceof NPCBody ?
                    customCallback.getFirst() : customCallback.getSecond();

                if (npc instanceof Enemy && !(npc instanceof PlayerBody)) {
                    Enemy enemy = (Enemy) npc;
                    if (!hitEnemies.contains(enemy)) {
                        enemy.setPositionHit(body.getWorldTransform().getTranslation(new Vector3()));
                        checkDamage(enemy);
                        hitEnemies.add(enemy);
                    }
                }
            }
        }

        if (!isCollide) {
            body.translate(body.getLinearVelocity().scl(delta * speedTranslation));
            body.getWorldTransform(targetEffect.getControllers().get(0).transform);

            if (body.getWorldTransform().getTranslation(new Vector3()).dst(positionPlayer) > 350f)
                return true;

        } else if (targetEnd == null && targetEffect.getControllers().get(0).emitter.percent < 1) {
            Matrix4 matrix4 = new Matrix4();
            targetEffect.getControllers().get(0).getTransform(matrix4);
            ParticlesComponent.getInstance(null).removeParticle(targetEffect);

            targetEnd = ParticlesComponent.getInstance(null).getEffectByName(effectEndName);
            targetEnd.setTransform(matrix4);
            targetEnd.init();
            targetEnd.start();
            ParticlesComponent.getInstance(null).addEffect(targetEnd);

            if (soundEnd != null) {
                soundEnd.play3D(AudioConfiguration.SOUND.getValue(),
                    targetEnd.getControllers().get(0).transform.getTranslation(new Vector3()));
                soundEnd = null;
            }
            if (shaking.x != 0 && shaking.y != 0)
                Level.getInstance().getPlayer().startShake(shaking.x, shaking.y);

            sphereBody.setCenterOfMassTransform(matrix4);
            HashMap<btCollisionObject, Vector3> collisions = PhysicComponent.getInstance().consumeCollisionBomb(sphereBody, null);

            if (!collisions.isEmpty()) {
                collisions.keySet().forEach(key -> {
                    if (key instanceof Enemy) {
                        Enemy enemy = (Enemy) key;
                        if (damageType == DamageType.MAGICAL && enemy instanceof PlayerBody) return;
                        enemy.setPositionHit(collisions.get(key));
                        checkDamage(enemy);
                    }
                    if (key instanceof TriggerBody) {
                        TriggerBody triggerBody = (TriggerBody) key;
                        triggerBody.activateTrigger();
                        triggerBody.disposeTrigger();
                    }
                });
            }

            if (sphereBody != null) {
                PhysicComponent.getInstance().disposeRigidBody(sphereBody);
                sphereBody = null;
            }

        } else {
            if (targetEnd != null && targetEnd.isComplete())
                return true;
        }
        return false;
    }

    protected void checkDamage(Enemy enemy) {
        if (enemy == null)
            return;

        PlayerBody playerBody = (PlayerBody) PhysicComponent.getInstance().getPlayerBody();
        playerBody.setBasicDamage(basicDamage);
        DamageComponent.getInstance().checkDamage(playerBody, enemy, damageType);
    }

    @Override
    public void reset() {
        hitEnemies.clear(); // Очищаем список пораженных NPC

        if (targetEffect != null) {
            ParticlesComponent.getInstance(null).removeParticle(targetEffect);
            targetEffect.reset();
            targetEffect = null;
        }
        if (targetEnd != null) {
            ParticlesComponent.getInstance(null).removeParticle(targetEnd);
            targetEnd.reset();
            targetEnd = null;
        }
        shaking = null;

        if (body != null) {
            PhysicComponent.getInstance().disposeRigidBody(body);
            body = null;
        }
    }
}
