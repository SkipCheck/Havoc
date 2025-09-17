package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.hexedrealms.Main;
import com.hexedrealms.components.ItemEntity;
import com.hexedrealms.components.NPC;
import com.hexedrealms.components.Trigger;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.NPCSteering.WalkerState;
import com.hexedrealms.utils.savedata.*;
import com.hexedrealms.utils.security.SecurityComponent;
import com.hexedrealms.weapons.BalisticWeapon;
import com.hexedrealms.weapons.ShootWeapon;
import com.hexedrealms.weapons.Weapon;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LoaderComponent {
    public static final String SAVES_DIR = "saves/";
    public static final String SAVE_EXTENSION = ".dat";
    public static final String BACKUP_EXTENSION = ".bak";

    public void saveGame(String slot) {
        SecurityComponent crypto = new SecurityComponent();
        String saveFileName = slot + SAVE_EXTENSION;
        String backupFileName = slot + BACKUP_EXTENSION;
        String savePath = SAVES_DIR + saveFileName;
        String backupPath = SAVES_DIR + backupFileName;

        // Создаем директорию для сохранений, если ее нет
        FileHandle savesDir = Gdx.files.local(SAVES_DIR);
        if (!savesDir.exists()) {
            savesDir.mkdirs();
        }

        // Подготовка данных для сохранения
        LevelData levelData = prepareLevelData();

        try {
            // Сериализация данных
            byte[] serializedData = serializeLevelData(levelData);

            // Создание HMAC для проверки целостности
            byte[] hmacDigest = createHmacDigest(crypto, serializedData);

            // Шифрование данных
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, crypto.getEncryptionKey());
            byte[] iv = cipher.getIV();

            // Сначала сохраняем во временный файл
            String tempPath = SAVES_DIR + "temp_" + saveFileName;
            writeSaveFile(tempPath, iv, hmacDigest, serializedData, cipher);

            // Создаем резервную копию текущего сохранения (если есть)
            if (Gdx.files.local(savePath).exists()) {
                Files.move(Paths.get(savePath), Paths.get(backupPath),
                    StandardCopyOption.REPLACE_EXISTING);
            }

            // Перемещаем временный файл в основной
            Files.move(Paths.get(tempPath), Paths.get(savePath),
                StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
            e.printStackTrace();
            // Попытка восстановить из резервной копии при ошибке
            restoreFromBackup(savePath, backupPath);
        }
    }

    private LevelData prepareLevelData() {
        List<ItemData> itemDataArray = new ArrayList<>();
        Array<ItemEntity> itemEntities = ItemsComponent.getInstance().getItems();
        processingItems(itemDataArray, itemEntities);

        List<WeaponData> weaponDataArray = new ArrayList<>();
        Array<Weapon> weapons = Level.getInstance().getPlayer().getmWeaponContainer().getWeapons();
        processingWeapons(weaponDataArray, weapons);

        Array<NPC> npces = NPCComponent.getInstance(null, null).npces;
        List<NPCData> npcDatas = new ArrayList<>();
        processingNPCes(npcDatas, npces);

        List<TriggerData> triggerDataArray = new ArrayList<>();
        List<Trigger> triggers = TriggerComponent.getInstance().getTriggers();
        for(Trigger trigger : triggers){
            TriggerData triggerData = new TriggerData();
            triggerData.indetificator = trigger.toString();
            triggerDataArray.add(triggerData);
        }

        Player player = Level.getInstance().getPlayer();
        PlayerBody playerBody = player.getPlayerBody();
        PlayerSaveData data = new PlayerSaveData();

        List<String> keys = new ArrayList<>();

        for(String key : player.keys){
            keys.add(key);
        }

        data.keys = keys;
        data.transform = playerBody.getWorldTransform();
        data.cameraUp = player.getCamera().up;
        data.cameraDirection = player.getCamera().direction;
        data.armor = playerBody.getArmor();
        data.health = playerBody.getCurrentHealth();
        data.agility = playerBody.getAgility();
        data.intelligence = playerBody.getIntelligence();
        data.strength = playerBody.getStrength();
        data.vitality = playerBody.getVitality();
        data.armorHealth = Level.getInstance().getPlayer().armorHealth;

        Level level = Level.getInstance();
        LevelData levelData = new LevelData();
        levelData.itemDataArray = itemDataArray;
        levelData.weaponDataArray = weaponDataArray;
        levelData.playerSaveData = data;
        levelData.triggerDataArray = triggerDataArray;
        levelData.levelID = level.levelId;
        levelData.slotID = level.slotId;
        levelData.difficulty = level.multiplier;
        levelData.npcDataArray = npcDatas;

        return levelData;
    }

    private byte[] serializeLevelData(LevelData levelData) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(byteStream)) {
            oos.writeObject(levelData);
        }
        return byteStream.toByteArray();
    }

    private byte[] createHmacDigest(SecurityComponent crypto, byte[] data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(crypto.getHmacKey());
        return hmac.doFinal(data);
    }

    private void writeSaveFile(String path, byte[] iv, byte[] hmacDigest,
                               byte[] data, Cipher cipher) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(path);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut);
             DataOutputStream dataOut = new DataOutputStream(gzipOut)) {

            // Записываем заголовок
            dataOut.writeInt(iv.length);
            dataOut.write(iv);
            dataOut.writeInt(hmacDigest.length);
            dataOut.write(hmacDigest);

            // Записываем зашифрованные данные
            try (CipherOutputStream cos = new CipherOutputStream(dataOut, cipher)) {
                cos.write(data);
            }
        }
    }

    private void restoreFromBackup(String savePath, String backupPath) {
        try {
            if (Gdx.files.local(backupPath).exists()) {
                Files.copy(Paths.get(backupPath), Paths.get(savePath),
                    StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Восстановлено из резервной копии");
            }
        } catch (IOException ex) {
            System.err.println("Не удалось восстановить из резервной копии: " + ex.getMessage());
        }
    }

    public LevelData loadMetaData(String slot) {
        SecurityComponent crypto = new SecurityComponent();
        String savePath = SAVES_DIR + slot + SAVE_EXTENSION;
        String backupPath = SAVES_DIR + slot + BACKUP_EXTENSION;

        try {
            // Загружаем метаданные
            LevelData levelData = loadFromFile(savePath, crypto);

            // Если не удалось загрузить из основного файла, пробуем резервный
            if (levelData == null) {
                levelData = loadFromFile(backupPath, crypto);
            }

            return levelData;
        } catch (Exception e) {
            return null;
        }
    }

    public String getFormattedSaveDate(String slot) {
        Date saveDate = getSaveFileDate(slot);
        if (saveDate == null) {
            return "Дата неизвестна";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return dateFormat.format(saveDate);
    }

    // Новый метод для получения даты сохранения
    public Date getSaveFileDate(String slot) {
        String savePath = SAVES_DIR + slot + SAVE_EXTENSION;
        String backupPath = SAVES_DIR + slot + BACKUP_EXTENSION;

        FileHandle saveFile = Gdx.files.local(savePath);
        if (!saveFile.exists()) {
            saveFile = Gdx.files.local(backupPath);
            if (!saveFile.exists()) {
                return null;
            }
        }

        // Получаем время последнего изменения файла
        long lastModified = saveFile.lastModified();
        return new Date(lastModified);
    }

    public Level loadGame(String slot) {
        SecurityComponent crypto = new SecurityComponent();
        String savePath = SAVES_DIR + slot + SAVE_EXTENSION;
        String backupPath = SAVES_DIR + slot + BACKUP_EXTENSION;

        try {
            LevelData levelData = loadFromFile(savePath, crypto);
            return applyLevelData(levelData);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки основного файла: " + e.getMessage());
            try {
                System.err.println("Попытка загрузить резервную копию");
                LevelData levelData = loadFromFile(backupPath, crypto);
                return applyLevelData(levelData);
            } catch (Exception ex) {
                System.err.println("Не удалось загрузить резервную копию: " + ex.getMessage());
                return null;
            }
        }
    }

    private Level applyLevelData(LevelData levelData) {
        if (levelData == null) return null;

        Level level = Level.getInstance(ResourcesLoader.getLevel(levelData.levelID));

        // Применяем все данные к уровню
        level.slotId = levelData.slotID;
        level.multiplier = levelData.difficulty;
        level.levelId = levelData.levelID;

        // Применяем данные игрока
        Player player = level.getPlayer();
        if (player != null && levelData.playerSaveData != null) {
            PlayerSaveData playerData = levelData.playerSaveData;

            for(String key : playerData.keys)
                player.putKey(key);

            Matrix4 transform = playerData.transform;
            player.initBody(transform);
            player.applyRotation(playerData.cameraUp, playerData.cameraDirection);

            PlayerBody playerBody = player.getPlayerBody();

            if (playerBody != null) {
                player.setArmor(playerData.armorHealth, playerData.armor);
                playerBody.setCurrentHealth((int) playerData.health);
                playerBody.setAgility(playerData.agility);
                playerBody.setIntelligence(playerData.intelligence);
                playerBody.setStrength(playerData.strength);
                playerBody.setVitality(playerData.vitality);
                player.setPreviousHealth((int) playerData.health);
            }
        }

        List<WeaponData> weaponDataArray = levelData.weaponDataArray;
        for(WeaponData weaponData : weaponDataArray){
            Weapon weapon = ResourcesLoader.getWeapon(weaponData.indentificator);

            if(weapon instanceof ShootWeapon){
                ShootWeapon shootWeapon = ((ShootWeapon) weapon);
                shootWeapon.setCurrentBullets(weaponData.currentBullets);
                shootWeapon.setTotalBullets(weaponData.totalBullets - weaponData.currentBullets);
            } else if (weapon instanceof BalisticWeapon) {
                BalisticWeapon balisticWeapon = (BalisticWeapon) weapon;
                balisticWeapon.setCurrentBullets(weaponData.currentBullets);
            }

            player.addWeapon(weapon);
        }

        List<ItemData> itemDataArray = levelData.itemDataArray;
        if(itemDataArray != null){
            // Создаем список для предметов, которые нужно удалить
            Array<ItemEntity> itemsToRemove = new Array<>();

            // Проверяем все текущие предметы
            for (ItemEntity item : ItemsComponent.getInstance().getItems()) {
                boolean found = false;

                // Ищем предмет в сохраненных данных
                for (ItemData itemData : itemDataArray) {
                    // Более надежное сравнение - например, по имени и позиции
                    if (item.getIconPath().equals(itemData.name) &&
                        item.position.equals(itemData.position)) {
                        found = true;
                        break;
                    }
                }

                // Если предмет не найден в сохраненных данных, помечаем для удаления
                if (!found) {
                    itemsToRemove.add(item);
                }
            }

            // Удаляем все помеченные предметы
            for (ItemEntity item : itemsToRemove) {
                item.free();
            }
        }

        List<TriggerData> triggerDataArray = levelData.triggerDataArray;
        if(triggerDataArray != null) {
            Array<Trigger> triggersToRemove = new Array<>();

            for (Trigger trigger : TriggerComponent.getInstance().getTriggers()) {
                boolean found = false;

                for (TriggerData triggerData : triggerDataArray) {
                    if (trigger.toString().equals(triggerData.indetificator)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    triggersToRemove.add(trigger);
                }
            }

            for (Trigger trigger : triggersToRemove) {
                if(NPCComponent.triggerArrayMap.keySet().contains(trigger)){
                    NPCComponent.triggerArrayMap.remove(trigger);
                }
                trigger.deactivate();
            }
        }

        NPCComponent npcComponent = NPCComponent.getInstance(null, null);

        if(levelData.npcDataArray != null && !levelData.npcDataArray.isEmpty()) {
            for (NPCData npcData : levelData.npcDataArray) {
                NPC npc = ResourcesLoader.getBot(npcData.name);
                npc.setPosition(npcComponent.findNearestNode(npcData.position), 0.5f);
                npc.setNodeGraph(npcComponent.getGraph());
                npc.init();
                npc.removeTeleport();
                Vector3 position = npc.getPosition();
                npc.getInstance().transform.setTranslation(position.x, position.y + 2f, position.z);
                npc.setBody(PhysicComponent.getInstance().addBotBody(npc.getInstance()));

                if (npc.aiType instanceof WalkerState) {
                    npc.getStateMachine().changeState((WalkerState) npcData.state);
                }

                EntityComponent.getInstance(null).addEntity(npc.getEntity());

                npc.setHealth(npcData.health);

                npcComponent.npces.add(npc);
            }
            AudioComponent.getInstance().targetState = AudioComponent.MusicState.COMBAT;
        }

        return level;
    }

    private LevelData loadFromFile(String path, SecurityComponent crypto) throws Exception {
        try (DataInputStream in = new DataInputStream(
            new GZIPInputStream(new FileInputStream(path)))) {

            // Чтение заголовка
            byte[] iv = new byte[in.readInt()];
            in.readFully(iv);
            byte[] storedHmac = new byte[in.readInt()];
            in.readFully(storedHmac);

            // Дешифровка
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, crypto.getEncryptionKey(), new IvParameterSpec(iv));

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    byteStream.write(buffer, 0, bytesRead);
                }
            }
            byte[] decryptedData = byteStream.toByteArray();

            // Проверка HMAC
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(crypto.getHmacKey());
            byte[] calculatedHmac = hmac.doFinal(decryptedData);

            if (!MessageDigest.isEqual(storedHmac, calculatedHmac)) {
                throw new SecurityException("Обнаружена подделка сохранения!");
            }

            // Десериализация
            try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(decryptedData))) {
                return (LevelData) ois.readObject();
            }
        }
    }

    public boolean saveExists(String slot) {
        String savePath = SAVES_DIR + slot + SAVE_EXTENSION;
        String backupPath = SAVES_DIR + slot + BACKUP_EXTENSION;
        return Gdx.files.local(savePath).exists() ||
            Gdx.files.local(backupPath).exists();
    }

    private void processingNPCes(List<NPCData> list, Array<NPC> npces) {
        for (NPC npc : npces) {
            NPCData npcData = new NPCData();
            npcData.health = npc.getCurrentHealth();
            npcData.name = npc.getName();
            npcData.position = npc.getBodyPosition();

            npcData.state = npc.getStateMachine().getCurrentState();

            list.add(npcData);
        }
    }


    private void processingWeapons(List<WeaponData> list, Array<Weapon> weapons) {
        for (Weapon weapon : weapons) {
            WeaponData data = new WeaponData();
            data.indentificator = weapon.getNameObject();
            if (weapon instanceof ShootWeapon) {
                data.currentBullets = ((ShootWeapon) weapon).getCurrentBullets();
                data.totalBullets = ((ShootWeapon) weapon).getTotalBullets();
            } else if (weapon instanceof BalisticWeapon) {
                data.currentBullets = ((BalisticWeapon) weapon).getCurrentBullets();
            } else {
                data.currentBullets = 0;
            }
            list.add(data);
        }
    }

    private void processingItems(List<ItemData> list, Array<ItemEntity> itemEntities) {
        for (ItemEntity entity : itemEntities) {

            ItemData data = new ItemData();
            data.name = entity.iconPath;
            data.position = entity.position;
            list.add(data);
        }
    }
}
