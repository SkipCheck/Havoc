package com.hexedrealms.engine;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.bulletbodies.NPCBody;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.utils.damage.Enemy;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class DamageComponent implements Disposable {
    private static DamageComponent instance;
    private static final float MIN_BOUND = 0.8f;
    private static final float MAX_BOUND = 1.3f;
    private static final float HEAD_RESISTANCE = 1.5f;
    private static final float BODY_RESISTANCE = 1f;
    private static final float LEGS_RESISTANCE = 0.5f;
    private static final float HEAD_PERCENT = 0.2f;
    private static final float BODY_PERCENT = 0.4f;

    private final ThreadLocalRandom random;
    private final SoundBuffer [] bloodBomb;

    private int countHead;
    private int countBody;
    private int countLegs;

    private DamageComponent(){
        random = ThreadLocalRandom.current();
        bloodBomb = new SoundBuffer[2];
        bloodBomb[0] = SoundLoader.load(Gdx.files.internal("audio/sfx/blood_bomb.wav"));
        bloodBomb[1] = SoundLoader.load(Gdx.files.internal("audio/sfx/blood_bomb_1.wav"));
    }

    public void checkDamage(Enemy killer, Enemy enemy, DamageType type){

        float baseDamage = calcBaseDamage(killer, type);
        float hitResistance = calcHitResistance(enemy);
        float armorResistance = enemy.getArmor();
        float vitalityResistance = calcVitalityResistance(enemy);

        float finalDamage = baseDamage
            * hitResistance
            * (1 - armorResistance)
            * (1 - vitalityResistance);

        if(enemy instanceof PlayerBody){
            Level.getInstance().getPlayer().deacreaseArmor(baseDamage * 1-armorResistance);
        }

        enemy.addDamage(finalDamage);

        if(enemy instanceof NPCBody) {
            bloodEffect(enemy.getPositionHit());
            if (enemy.getCurrentHealth() < 0){
                bloodDeath(((NPCBody) enemy).getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain()), enemy.getParts(), enemy.getWidth());
                enemy.setCurrentHealth(0);
            }
        }
    }

    private float calcHitResistance(Enemy enemy){
        Vector3 position = PhysicComponent.getInstance().getVectorPool().obtain();
        btRigidBody body = (btRigidBody) enemy;
        body.getWorldTransform().getTranslation(position);

        // Полная высота врага
        float totalHeight = enemy.getHeight();
        position.y += totalHeight;

        // Высота зон (можно настроить)
        float headHeight = totalHeight * HEAD_PERCENT;
        float bodyHeight = totalHeight * BODY_PERCENT;

        // Точка попадания относительно врага
        Vector3 hitPoint = enemy.getPositionHit();
        float hitY = Math.abs(hitPoint.y - position.y);

        // Определение зоны попадания
        if (hitY <= headHeight) {
            countHead++;
            return HEAD_RESISTANCE;
        } else if (hitY > headHeight && hitY <= (headHeight + bodyHeight)) {
            countBody++;
            return BODY_RESISTANCE;
        } else {
            countLegs++;
            return LEGS_RESISTANCE;
        }
    }

    private float calcVitalityResistance(Enemy enemy) {
        float maxVitality = enemy.getHealth(); // Максимальное значение живучести
        float currentVitality = enemy.getVitality(); // Предполагаем, что у Enemy есть метод getVitality()

        // Коэффициент сопротивляемости от 0 до 0.5 (можно изменить)
        float maxResistance = 0.9f;

        // Расчет сопротивляемости
        return Math.min(maxResistance, currentVitality / maxVitality * maxResistance);
    }

    private float calcBaseDamage(Enemy killer, DamageType type){
        float damage = 0;
        switch (type){
            case PHYSICAL : {
                damage = (killer.getBasicDamage() * (1 + killer.getStrength() * type.getDmgCoefficient() ))
                    * (1 + killer.getAgility() * type.getCritRatio()) * random.nextFloat(MIN_BOUND, MAX_BOUND) * Level.getInstance().multiplier;
                break;
            }
            case MAGICAL : {
                damage = (killer.getBasicDamage() * (1 + killer.getIntelligence() * type.getDmgCoefficient() ))
                    * (1 + killer.getAgility() * type.getCritRatio()) * random.nextFloat(MIN_BOUND, MAX_BOUND) * Level.getInstance().multiplier;
                break;
            }
        }
        return damage;
    }

    private void bloodDeath(Vector3 position, Array<TextureAtlas.AtlasRegion> regions, float weight){
        bloodBomb[ThreadLocalRandom.current().nextInt(0, bloodBomb.length)].play3D(AudioConfiguration.SOUND.getValue(), position);
        PhysicComponent.getInstance().addDynamicEntity(position,weight, regions, "blood_bomb.pfx");
    }

    private void bloodEffect(Vector3 position){
        ParticleEffect effect = ParticlesComponent.getInstance(null).findEffect("blood.pfx");
        effect.translate(position);
        effect.init();
        effect.start();
        ParticlesComponent.getInstance(null).addDynamicEffect(effect);
    }

    public static DamageComponent getInstance(){
        if(instance == null)
            instance = new DamageComponent();
        return instance;
    }

    @Override
    public void dispose() {
        // Dispose all sound buffers
        for (SoundBuffer sound : bloodBomb) {
            if (sound != null) {
                sound.dispose();
            }
        }

        // Reset counters
        countHead = 0;
        countBody = 0;
        countLegs = 0;

        // Reset singleton instance
        instance = null;
    }
}
