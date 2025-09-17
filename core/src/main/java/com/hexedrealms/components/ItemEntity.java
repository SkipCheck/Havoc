package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.configurations.PlayerConfiguration;
import com.hexedrealms.engine.*;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.Enemy;
import com.hexedrealms.weapons.BalisticWeapon;
import com.hexedrealms.weapons.ShootWeapon;
import com.hexedrealms.weapons.Weapon;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

import java.util.function.Consumer;

public class ItemEntity extends Trigger implements Disposable {
    public String name;
    public Entity entity;
    public String iconPath;
    public int quantity;
    public TriggerType triggerType;
    public float glowIntensity;
    public Vector3 position;

    public ItemEntity(String name, String iconPath, int quantity, TriggerType triggerType, float glowIntensity, Color color) {
        super(null, false, null);

        this.name = name;
        this.quantity = quantity;
        this.iconPath = iconPath;
        this.triggerType = triggerType;
        this.glowIntensity = glowIntensity;
    }

    public void init(Vector3 position, Environment environment) {

        entity = EntityComponent.getInstance(null).getPool().obtain();
        entity.init(new TextureRegion(ItemsComponent.getInstance().findRegion(iconPath)), new Vector3(), new Quaternion(), false);
        entity.setPosition(position, 2f);
        entity.setFullRotation(true);
        entity.mulScale(0.7f);
        this.position = position;

        EntityComponent.getInstance(null).addEntity(entity);

        triggerZone = new BoundingBox(entity.boundingBox);
        triggerZone.max.y += PlayerConfiguration.PLAYER_HEIGHT.getValue() / 2f;
        triggerZone.min.y -= PlayerConfiguration.PLAYER_HEIGHT.getValue() / 2f;
        triggerZone.min.x -= 1f;
        triggerZone.min.z -= 1f;
        triggerZone.max.x += 1f;
        triggerZone.max.z += 1f;

        triggerType.setPosition(position);
        triggerType.putItemEntity(this);
        action = triggerType;

        TriggerComponent.getInstance().putTrigger(this);
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getName() {
        return name;
    }

    public void free() {
        this.deactivate();
        ItemsComponent.getInstance().removeItem(this);
        EntityComponent.getInstance(null).removeEntity(entity);
    }

    @Override
    public void dispose() {
        // Dispose the entity
        if (entity != null) {
            entity.dispose();
            entity = null;
        }

        // Clear references
        position = null;
        triggerType = null;
        iconPath = null;
        name = null;
    }

    // Базовый класс для типов триггеров
    public static abstract class TriggerType implements Runnable {
        public abstract void execute(String target, SoundBuffer soundBuffer);
        public abstract void setPosition(Vector3 position);
        public abstract void putItemEntity(ItemEntity itemEntity);
    }

    /**
     * Создает глубокую копию текущего объекта ItemEntity с использованием пулов
     * @return новая копия ItemEntity, полученная из пула
     */
    public ItemEntity copy() {

        ItemEntity copy = new ItemEntity(
            this.name,
            this.iconPath,
            this.quantity,
            copyTriggerType(this.triggerType), // Копируем триггер
            this.glowIntensity, Color.WHITE
        );

        // Копируем entity
        if (this.entity != null) {
            copy.entity = new Entity();
            copy.entity.init(
                new TextureRegion(this.entity.mRegion),
                new Vector3(this.entity.getPosition()),
                new Quaternion(this.entity.getQuaternion()),
                false
            );
            copy.entity.calcScale(this.entity.scale);
        }

        // Копируем триггерную зону
        if (this.triggerZone != null) {
            copy.triggerZone = new BoundingBox(this.triggerZone);
        }

        return copy;
    }

    // Вспомогательный метод для копирования триггера
    private TriggerType copyTriggerType(TriggerType original) {
        if (original == null) return null;

        if (original instanceof WeaponTrigger) {
            WeaponTrigger orig = (WeaponTrigger) original;
            WeaponTrigger copy = new WeaponTrigger();
            copy.execute(orig.target, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        } else if (original instanceof HealthTrigger) {
            HealthTrigger orig = (HealthTrigger) original;
            HealthTrigger copy = new HealthTrigger();
            copy.execute(null, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        } else if (original instanceof BulletsTrigger) {
            BulletsTrigger orig = (BulletsTrigger) original;
            BulletsTrigger copy = new BulletsTrigger();
            copy.execute(orig.target, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        } else if (original instanceof KeyTrigger) {
            KeyTrigger orig = (KeyTrigger) original;
            KeyTrigger copy = new KeyTrigger();
            copy.execute(orig.target, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        } else if (original instanceof ArmorTrigger) {
            ArmorTrigger orig = (ArmorTrigger) original;
            ArmorTrigger copy = new ArmorTrigger();
            copy.execute(orig.target, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        } else if (original instanceof StarTrigger) {
            StarTrigger orig = (StarTrigger) original;
            StarTrigger copy = new StarTrigger();
            copy.execute(orig.target, orig.soundBuffer);
            copy.setPosition(orig.position != null ? new Vector3(orig.position) : new Vector3());
            return copy;
        }
        return null;
    }

    public static class KeyTrigger extends TriggerType {
        public String target;
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {

            if (Level.getInstance().getPlayer().containsKey(target)) return;
            if (target != null) {
                Level.getInstance().getPlayer().putKey(target);
            }
            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.armor);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.target = target;
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }
    }

    public static class ArmorTrigger extends TriggerType {
        public String target;
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {
            float health = 0;
            float resistence = 0;



            switch (target){
                case "small_armor": {
                    health = 50;
                    resistence = 0.3f;
                    break;
                }
                case "big_armor": {
                    health = 100;
                    resistence = 0.5f;
                    break;
                }
                case "heavy_armor": {
                    health = 150;
                    resistence = 0.7f;
                    break;
                }
            }

            if(Level.getInstance().getPlayer().getPlayerBody().getArmor() > resistence) return;
            if(Level.getInstance().getPlayer().armorHealth > health) return;

            Level.getInstance().getPlayer().setArmor(health, resistence);

            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.armor);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.target = target;
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }
    }

    // Реализация триггера для оружия
    public static class WeaponTrigger extends TriggerType {
        public String target;
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {
            Weapon weapon = ResourcesLoader.getWeapon(target);
            if (Level.getInstance().getPlayer().getmWeaponContainer().contains(weapon)) return;
            if (weapon != null) {
                Level.getInstance().getPlayer().addWeapon(weapon);
            }
            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.weapon);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.target = target;
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }
    }

    public static class StarTrigger extends TriggerType {
        public String target;
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.star);

            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.target = target;
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }
    }

    public static class BulletsTrigger extends TriggerType {
        public String target;
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {
            Weapon weapon = ResourcesLoader.getWeapon(target);

            if (!Level.getInstance().getPlayer().getmWeaponContainer().contains(weapon)) return;

            if(weapon instanceof ShootWeapon){
                ShootWeapon shootWeapon = (ShootWeapon) weapon;
                if(shootWeapon.getTotalBullets() >= shootWeapon.getMaxBullets()) return;
                shootWeapon.addTotalBullets(entity.quantity);
            }else if(weapon instanceof BalisticWeapon){
                BalisticWeapon shootWeapon = (BalisticWeapon) weapon;
                if(shootWeapon.getCurrentBullets() >= shootWeapon.getMaxBullets()) return;
                shootWeapon.addBullets(entity.quantity);
            }

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.bullet);

            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.target = target;
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }
    }

    public static class HealthTrigger extends TriggerType {
        public SoundBuffer soundBuffer;
        public Vector3 position;
        public ItemEntity entity;

        @Override
        public void run() {
            Player player =  Level.getInstance().getPlayer();
            int health = player.getEnemy().getCurrentHealth();
            if (health >= 100) return;

            player.addHealth(entity.quantity);

            OverlaysComponent component = OverlaysComponent.getInstance();
            component.uploadOverlay(component.health);

            entity.free();
            soundBuffer.play3D(AudioConfiguration.SOUND.getValue(), position);
        }

        @Override
        public void execute(String target, SoundBuffer soundBuffer) {
            this.soundBuffer = soundBuffer;
        }

        @Override
        public void setPosition(Vector3 position) {
            this.position = position;
        }

        @Override
        public void putItemEntity(ItemEntity itemEntity) {
            this.entity = itemEntity;
        }

    }
}
