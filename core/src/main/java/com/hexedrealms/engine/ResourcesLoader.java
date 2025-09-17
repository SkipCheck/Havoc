package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.NPC;
import com.hexedrealms.components.ItemEntity;
import com.hexedrealms.utils.NPCSteering.WalkerState;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.utils.savedata.Level;
import de.pottgames.tuningfork.SoundLoader;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResourcesLoader {
    // Кэши для быстрого доступа
    private static final Map<String, com.hexedrealms.weapons.Weapon> weaponsCache = new HashMap<>();
    private static final Map<String, Pool<ItemEntity>> itemsCache = new HashMap<>();
    private static final Map<String, Pool<NPC>> botsCache = new HashMap<>();
    private static final Map<Integer, Level> levelsCache = new HashMap<>();

    private static final Array<com.hexedrealms.weapons.Weapon> weaponsList = new Array<>();
    private static final Array<com.hexedrealms.components.ItemEntity> itemsList = new Array<>();
    private static final Array<com.hexedrealms.components.NPC> botsList = new Array<>();

    private ResourcesLoader() {}

    public static void loadResources() {
        clearCache();

        FileHandle file = Gdx.files.internal("data/resources.bin");

        try {
            GameData gameData = loadAndParseGameData(file);
            processLoadedData(gameData);
        } catch (Exception e) {
            Gdx.app.error("ResourcesLoader", "Error loading resources", e);
            throw new RuntimeException("Failed to load game resources", e);
        }
    }

    private static GameData loadAndParseGameData(FileHandle file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new CustomObjectInputStream(file.read())) {
            Object loadedData = ois.readObject();
            return parseGameData(loadedData);
        }
    }

    private static GameData parseGameData(Object loadedData) throws IOException {
        if (loadedData instanceof GameData) {
            GameData gameData = (GameData) loadedData;
            // Обеспечиваем обратную совместимость
            if (gameData.levels == null) gameData.levels = new ArrayList<>();
            if (gameData.bots == null) gameData.bots = new ArrayList<>();
            return gameData;
        } else if (loadedData instanceof ArrayList) {
            return new GameData((ArrayList<Weapon>) loadedData, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        throw new IOException("Unknown data format");
    }

    private static void processLoadedData(GameData gameData) {
        clearCache();

        // Загрузка оружия
        for (Weapon weapon : gameData.weapons) {
            com.hexedrealms.weapons.Weapon weaponEntity = createWeaponEntity(weapon);
            if (weaponEntity != null) {
                registerWeapon(weaponEntity);
            }
        }

        // Загрузка предметов
        for (Item item : gameData.items) {
            com.hexedrealms.components.ItemEntity itemEntity = createItemEntity(item);
            if (itemEntity != null) {
                registerItem(itemEntity);
            }
        }

        // Загрузка ботов
        for (Bot bot : gameData.bots) {
            com.hexedrealms.components.NPC botEntity = createBotEntity(bot);
            if (botEntity != null) {
                registerBot(botEntity);
            }
        }

        // Загрузка уровней
        for (Level level : gameData.levels) {
            levelsCache.put(gameData.levels.indexOf(level), level);
        }
    }

    // ========== Оружие ==========
    private static com.hexedrealms.weapons.Weapon createWeaponEntity(Weapon weapon) {
        if (weapon instanceof ShootWeapon) {
            return createShootWeapon((ShootWeapon) weapon);
        } else if (weapon instanceof BalisticWeapon) {
            return createBalisticWeapon((BalisticWeapon) weapon);
        }
        return createBasicWeapon(weapon);
    }

    private static com.hexedrealms.weapons.ShootWeapon createShootWeapon(ShootWeapon shootWeapon) {
        Color color = shootWeapon.shootColor;
        String[] shaking = ((String) shootWeapon.shaking).split(",");

        com.hexedrealms.weapons.ShootWeapon weaponEntity = new com.hexedrealms.weapons.ShootWeapon(
            shootWeapon.atlas,
            shootWeapon.shootSound,
            shootWeapon.selectSound,
            shootWeapon.reloadSound,
            shootWeapon.hitTexture,
            shootWeapon.endParticle,
            convertColor(color),
            shootWeapon.shootCount,
            (int) shootWeapon.scaleRay,
            shootWeapon.spreadCount,
            shootWeapon.maxBullets,
            shootWeapon.shopBullets,
            shootWeapon.speedChangeFrame,
            shootWeapon.intensityReload,
            shootWeapon.scaleViewed,
            shootWeapon.scaleViewReload,
            new Vector2(Float.parseFloat(shaking[0]), Float.parseFloat(shaking[1])),
            shootWeapon.dynamicSpread,
            shootWeapon.damageType,
            shootWeapon.basicDamage,
            shootWeapon.currentBullets,
            shootWeapon.currentShops,
            shootWeapon.isRiffle
        );

        configureWeapon(weaponEntity, shootWeapon);
        return weaponEntity;
    }

    private static com.hexedrealms.weapons.BalisticWeapon createBalisticWeapon(BalisticWeapon balisticWeapon) {
        Color color = balisticWeapon.shootColor;
        String[] shaking = ((String) balisticWeapon.shaking).split(",");

        com.hexedrealms.weapons.BalisticWeapon weaponEntity = new com.hexedrealms.weapons.BalisticWeapon(
            balisticWeapon.atlas,
            balisticWeapon.shootSound,
            balisticWeapon.selectSound,
            balisticWeapon.bulletSound,
            balisticWeapon.speedChangeFrame,
            balisticWeapon.scaleViewed,
            balisticWeapon.maxBullets,
            convertColor(color),
            balisticWeapon.atlasGroup,
            balisticWeapon.effectName,
            balisticWeapon.endEffectName,
            balisticWeapon.damageType,
            balisticWeapon.basicDamage,
            balisticWeapon.speedBullet,
            balisticWeapon.currentBullets,
            new Vector2(Float.parseFloat(shaking[0]), Float.parseFloat(shaking[1])),
            balisticWeapon.isRiffle,
            balisticWeapon.radius
        );

        configureWeapon(weaponEntity, balisticWeapon);
        return weaponEntity;
    }

    private static com.hexedrealms.weapons.Weapon createBasicWeapon(Weapon weapon) {
        com.hexedrealms.weapons.Weapon weaponEntity = new com.hexedrealms.weapons.Weapon(
            weapon.atlas, weapon.shootSound, weapon.selectSound, weapon.hitSound,
            weapon.hitTexture, weapon.speedChangeFrame, weapon.scaleViewed,
            weapon.scaleRay, weapon.damageType, weapon.basicDamage
        );

        configureWeapon(weaponEntity, weapon);
        return weaponEntity;
    }

    private static void configureWeapon(com.hexedrealms.weapons.Weapon weaponEntity, Weapon source) {
        weaponEntity.setName(source.name);
        weaponEntity.setIcon(source.iconPath);
        weaponEntity.addCustomOffset((int) source.offsetX, (int) source.offsetY);
    }

    private static void registerWeapon(com.hexedrealms.weapons.Weapon weapon) {
        weaponsList.add(weapon);
        weaponsCache.put(weapon.getNameObject(), weapon);
    }

    // ========== Предметы ==========
    private static com.hexedrealms.components.ItemEntity createItemEntity(Item item) {
        ItemEntity.TriggerType triggerType = parseTriggerType(item.triggerType);
        triggerType.execute(item.target, SoundLoader.load(Gdx.files.internal(item.pickupSound)));

        com.hexedrealms.components.ItemEntity entity = new com.hexedrealms.components.ItemEntity(
            item.name,
            item.iconPath,
            item.quantity,
            triggerType,
            item.glowIntensity,
            convertColor(item.glowColor)
        );

        return entity;
    }

    public static ItemEntity findItem(String name){
        String nameItem = "";
        int min = Integer.MAX_VALUE;
        for (ItemEntity item : itemsList){
            if(item.triggerType instanceof ItemEntity.BulletsTrigger){
                ItemEntity.BulletsTrigger targetTrigger = (ItemEntity.BulletsTrigger) item.triggerType;
                if(targetTrigger.target.equals(name)){
                    if(item.quantity < min){
                        min = item.quantity;
                        nameItem = item.iconPath;
                    }
                }
            }
        }
        return getItem(nameItem);
    }

    private static com.hexedrealms.components.ItemEntity.TriggerType parseTriggerType(String triggerType) {
        if(triggerType.equals("WEAPON_TRIGGER")){
            return new ItemEntity.WeaponTrigger();
        } else if(triggerType.equals("HP_TRIGGER")){
            return new ItemEntity.HealthTrigger();
        } else if (triggerType.equals("BULLET_TRIGGER")) {
            return new ItemEntity.BulletsTrigger();
        } else if (triggerType.equals("KEY_TRIGGER")) {
            return new ItemEntity.KeyTrigger();
        } else if (triggerType.equals("ARMOR_TRIGGER")) {
            return new ItemEntity.ArmorTrigger();
        } else if (triggerType.equals("STAR_TRIGGER")){
            return new ItemEntity.StarTrigger();
        }
        return null;
    }

    public static void registerItem(com.hexedrealms.components.ItemEntity item) {
        itemsList.add(item);
        itemsCache.put(item.getIconPath(), new Pool<ItemEntity>() {
            @Override
            protected ItemEntity newObject() {
                return item.copy();
            }
        });
    }

    // ========== Боты ==========
    private static com.hexedrealms.components.NPC createBotEntity(Bot bot) {

        State<NPC> npcState = null;
        switch (bot.aiType){
            case WALKER : {
                npcState = WalkerState.SPAWN;
                break;
            }
        }

        com.hexedrealms.components.NPC botEntity = new com.hexedrealms.components.NPC(
            bot.name,
            bot.atlas,
            bot.maxSpeed,
            bot.damageType,
            npcState,
            (int) bot.baseDamage,
            bot.baseArmor,
            bot.baseHealth,
            bot.agility,
            bot.strength,
            bot.intelligence,
            bot.vitality
        );

        // Устанавливаем звуки
        botEntity.setFirstSeenSound(SoundLoader.load(Gdx.files.internal(bot.firstSeenSound)));
        botEntity.setDamageTakenSound(SoundLoader.load(Gdx.files.internal(bot.damageTakenSound)));
        botEntity.setDeathSound(SoundLoader.load(Gdx.files.internal(bot.deathSound)));
        botEntity.setAttackSound(SoundLoader.load(Gdx.files.internal(bot.attackSound)));

        return botEntity;
    }

    public static void registerBot(com.hexedrealms.components.NPC bot) {
        botsList.add(bot);
        botsCache.put(bot.getName(), new Pool<NPC>() {
            @Override
            protected NPC newObject() {
                return bot.copy();
            }
        });
    }

    // ========== Утилиты ==========
    public static com.badlogic.gdx.graphics.Color convertColor(Color color) {
        return new com.badlogic.gdx.graphics.Color(
            color.getRed()/255f,
            color.getGreen()/255f,
            color.getBlue()/255f,
            1f
        );
    }

    private static void clearCache() {
        weaponsCache.clear();
        weaponsList.clear();
        itemsCache.clear();
        itemsList.clear();
        botsCache.clear();
        botsList.clear();
        levelsCache.clear();
    }

    // ========== Публичное API ==========
    public static com.hexedrealms.weapons.Weapon getWeapon(String name) {
        return weaponsCache.get(name);
    }

    public static com.hexedrealms.components.ItemEntity getItem(String name) {
        try {
            return itemsCache.get(name).obtain();
        } catch (Exception e) {
            Gdx.app.error("ResourcesLoader", "Error getting item: " + name, e);
            return null;
        }
    }

    public static NPC getBot(String name) {
        try {
            return botsCache.get(name).obtain();
        } catch (Exception e) {
            Gdx.app.error("ResourcesLoader", "Error getting bot: " + name, e);
            return null;
        }
    }

    public static Level getLevel(int index) {
        return levelsCache.get(index);
    }

    public static Array<com.hexedrealms.weapons.Weapon> getAllWeapons() {
        return weaponsList;
    }

    public static Array<com.hexedrealms.components.ItemEntity> getAllItems() {
        return itemsList;
    }

    public static Array<com.hexedrealms.components.NPC> getAllBots() {
        return botsList;
    }

    public static void freeItem(ItemEntity item) {
        if (item == null) return;

        if (item.entity != null) {
            item.entity.reset();
        }

        Pool<ItemEntity> pool = itemsCache.get(item.getIconPath());
        if (pool != null) {
            pool.free(item);
        }
    }

    public static void freeBot(NPC bot) {
        if (bot == null) return;

        Pool<NPC> pool = botsCache.get(bot);
        if (pool != null) {
            pool.free(bot);
        }
    }
}

class CustomObjectInputStream extends ObjectInputStream {
    private static final Map<String, String> CLASS_MAPPING = createClassMapping();

    public CustomObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();
        String mappedClass = CLASS_MAPPING.getOrDefault(className, className);

        try {
            return Class.forName(mappedClass);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to resolve class: " + className, e);
        }
    }

    private static Map<String, String> createClassMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("Weapon", "com.hexedrealms.engine.Weapon");
        mapping.put("ShootWeapon", "com.hexedrealms.engine.ShootWeapon");
        mapping.put("BalisticWeapon", "com.hexedrealms.engine.BalisticWeapon");
        mapping.put("DamageType", "com.hexedrealms.utils.damage.DamageType");
        mapping.put("GameData", "com.hexedrealms.engine.GameData");
        mapping.put("Item", "com.hexedrealms.engine.Item");
        mapping.put("Bot", "com.hexedrealms.engine.Bot");
        mapping.put("Level", "com.hexedrealms.utils.savedata.Level");
        mapping.put("AIType", "com.hexedrealms.engine.AIType");
        return mapping;
    }
}

class GameData implements Serializable {
    private static final long serialVersionUID = 2L;
    public List<Weapon> weapons;
    public List<Item> items;
    public List<Bot> bots;
    public List<Level> levels;

    public GameData(List<Weapon> weapons, List<Item> items, List<Level> levels, List<Bot> bots) {
        this.weapons = weapons;
        this.items = items;
        this.levels = levels;
        this.bots = bots;
    }
}

class Weapon implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String iconPath;
    String atlas;
    String shootSound;
    String selectSound;
    String hitSound;
    String hitTexture;
    float scaleRay;
    float speedChangeFrame;
    float scaleViewed;
    DamageType damageType;
    float basicDamage;
    float offsetX;
    float offsetY;

    public Weapon(String name, String iconPath, String atlas, String shootSound,
                  String selectSound, String hitSound, String hitTexture,
                  float scaleRay, float speedChangeFrame, float scaleViewed,
                  DamageType damageType, float basicDamage, float offsetX, float offsetY) {
        this.name = name;
        this.iconPath = iconPath;
        this.atlas = atlas;
        this.shootSound = shootSound;
        this.selectSound = selectSound;
        this.hitSound = hitSound;
        this.hitTexture = hitTexture;
        this.scaleRay = scaleRay;
        this.speedChangeFrame = speedChangeFrame;
        this.scaleViewed = scaleViewed;
        this.damageType = damageType;
        this.basicDamage = basicDamage;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}

class BalisticWeapon extends Weapon {
    private static final long serialVersionUID = 1L;
    String bulletSound;
    int maxBullets;
    int shopBullets;
    int currentBullets;
    int currentShops;
    float speedBullet;
    float radius;
    Color shootColor;
    String atlasGroup;
    Object shaking;
    String effectName;
    String endEffectName;
    boolean isRiffle;

    public BalisticWeapon(String name, String iconPath, String atlas, String shootSound,
                          String selectSound, String bulletSound, float speed, float scale,
                          int maxBullets, int shopBullets, Color shootColor,
                          String atlasGroup, String effectName, String endEffectName,
                          DamageType damageType, float basicDamage, float offsetX, float offsetY,
                          int currentBullets, int currentShops, float speedBullet, Object shaking, boolean isRiffle, float radius) {
        super(name, iconPath, atlas, shootSound, selectSound, null, null, 0, speed, scale,
            damageType, basicDamage, offsetX, offsetY);

        this.radius = radius;
        this.isRiffle = isRiffle;
        this.shaking = shaking;
        this.speedBullet = speedBullet;
        this.bulletSound = bulletSound;
        this.maxBullets = maxBullets;
        this.shopBullets = shopBullets;
        this.currentBullets = currentBullets;
        this.currentShops = currentShops;
        this.shootColor = shootColor;
        this.atlasGroup = atlasGroup;
        this.effectName = effectName;
        this.endEffectName = endEffectName;
    }
}

class ShootWeapon extends Weapon {
    private static final long serialVersionUID = 1L;
    String reloadSound;
    String hitTexture;
    String endParticle;
    Color shootColor;
    int shootCount;
    int scaleRay;
    int spreadCount;
    int maxBullets;
    int shopBullets;
    int currentBullets;
    int currentShops;
    float intensityReload;
    float scaleViewReload;
    Object shaking;
    boolean dynamicSpread;
    boolean isRiffle;

    public ShootWeapon(String name, String iconPath, String atlas, String shootSound,
                       String selectSound, String reloadSound, String hitTexture,
                       String endParticle, Color shootColor, int shootCount, int scaleRay,
                       int spreadCount, int maxBullets, int shopBullets, float speed,
                       float intensity, float scale, float scaleReload, Object shaking,
                       boolean dynamicSpread, DamageType damageType, float damage,
                       float offsetX, float offsetY, int currentBullets, int currentShops, boolean isRiffle) {
        super(name, iconPath, atlas, shootSound, selectSound, null, hitTexture, scaleRay,
            speed, scale, damageType, damage, offsetX, offsetY);

        this.isRiffle = isRiffle;
        this.reloadSound = reloadSound;
        this.hitTexture = hitTexture;
        this.endParticle = endParticle;
        this.shootColor = shootColor;
        this.shootCount = shootCount;
        this.scaleRay = scaleRay;
        this.spreadCount = spreadCount;
        this.maxBullets = maxBullets;
        this.shopBullets = shopBullets;
        this.currentBullets = currentBullets;
        this.currentShops = currentShops;
        this.intensityReload = intensity;
        this.scaleViewReload = scaleReload;
        this.shaking = shaking;
        this.dynamicSpread = dynamicSpread;
    }
}

class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public String iconPath;
    public int quantity;
    public String pickupSound;
    public String triggerType;
    public String target;
    public float glowIntensity;
    public Color glowColor;

    public Item(String name, String iconPath, int quantity, String pickupSound,
                String triggerType, String target, float glowIntensity, Color glowColor) {
        this.name = name;
        this.iconPath = iconPath;
        this.quantity = quantity;
        this.pickupSound = pickupSound;
        this.triggerType = triggerType;
        this.target = target;
        this.glowIntensity = glowIntensity;
        this.glowColor = glowColor;
    }
}

class Bot implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public String firstSeenSound;
    public String damageTakenSound;
    public String deathSound;
    public String attackSound;
    public String atlas;
    public float maxSpeed;
    public DamageType damageType;
    public AIType aiType;
    public float baseDamage;
    public float baseArmor;
    public float baseHealth;
    public float agility;
    public float strength;
    public float intelligence;
    public float vitality;

    public Bot(String name, String firstSeenSound, String damageTakenSound, String deathSound,
               String attackSound, String atlas, float maxSpeed, DamageType damageType,
               AIType aiType, float baseDamage, float baseArmor, float baseHealth,
               float agility, float strength, float intelligence, float vitality) {
        this.name = name;
        this.firstSeenSound = firstSeenSound;
        this.damageTakenSound = damageTakenSound;
        this.deathSound = deathSound;
        this.attackSound = attackSound;
        this.atlas = atlas;
        this.maxSpeed = maxSpeed;
        this.damageType = damageType;
        this.aiType = aiType;
        this.baseDamage = baseDamage;
        this.baseArmor = baseArmor;
        this.baseHealth = baseHealth;
        this.agility = agility;
        this.strength = strength;
        this.intelligence = intelligence;
        this.vitality = vitality;
    }
}
