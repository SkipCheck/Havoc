package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.configurations.PlayerConfiguration;
import com.hexedrealms.utils.damage.Enemy;
import com.hexedrealms.weapons.Weapon;
import de.pottgames.tuningfork.EaxReverb;
import de.pottgames.tuningfork.SoundEffect;
import de.pottgames.tuningfork.StreamedSoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class Player implements Disposable {

    // Основные компоненты
    private PerspectiveCamera mCamera;
    private ModelInstance mPlayer;
    private WeaponContainer mWeaponContainer;
    private HUD hudContainer;
    private PlayerBody playerBody;

    // Векторы позиционирования
    private Vector3 mPosition;
    private Vector3 mTemp;

    // Звуковые эффекты
    private StreamedSoundSource[] walkingSounds;
    private StreamedSoundSource[] jumpSound;
    private StreamedSoundSource[] playerPain;
    private StreamedSoundSource downSound;

    // Переменные состояния
    private float rollAngle, previousHealth;
    private float mAnimation;
    private int currentWalkSound;
    private int currentJumpSound;
    private boolean isMoving, isOnGround, isStaying;

    // Параметры тряски камеры
    private float shakeIntensity;
    private float shakeDuration;
    private float shakeDecay = 0.1f;
    private float shakeTime;
    public float armorHealth;
    public boolean ignoreNextMouseDelta;

    public Array<String> keys = new Array<>();

    // Конструктор
    public Player(float viewAngle, float farView) {
        // Инициализация базовых параметров
        initializeBasicParameters(viewAngle, farView);

        // Настройка звуков
        initializeSounds();

        // Настройка ввода
        setupInputControls();
    }

    public void setArmor(float armorHealth, float resistence){
        this.armorHealth = armorHealth;
        playerBody.setArmor(resistence);
    }

    public void applyFOV(float value){
        mCamera.fieldOfView = value;
        mCamera.update();
    }

    public PlayerBody getPlayerBody(){
        return playerBody;
    }

    // Инициализация базовых параметров
    private void initializeBasicParameters(float viewAngle, float farView) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        mPosition = PhysicComponent.getInstance().getVectorPool().obtain();
        mTemp = PhysicComponent.getInstance().getVectorPool().obtain();

        mCamera = new PerspectiveCamera(viewAngle, width, height);
        mCamera.near = 0.1f;
        mCamera.far = farView;
    }

    // Инициализация звуков
    private void initializeSounds() {
        // Инициализация звуков ходьбы
        walkingSounds = new StreamedSoundSource[6];
        for (int i = 0; i < walkingSounds.length; i++) {
            walkingSounds[i] = new StreamedSoundSource(
                Gdx.files.internal("audio/sounds/player/walking_" + (i + 1) + ".wav")
            );
            configureSoundSource(walkingSounds[i], 1.87f, 1f);
        }

        jumpSound = new StreamedSoundSource[2];
        for (int i = 0; i < jumpSound.length; i++) {
            jumpSound[i] = new StreamedSoundSource(
                Gdx.files.internal("audio/sounds/player/hero_jump_" + (i + 1) + ".wav")
            );
            configureSoundSource(jumpSound[i], 1f, 1f);
            jumpSound[i].detachAllEffects();
        }

        playerPain = new StreamedSoundSource[3];
        for (int i = 0; i < playerPain.length; i++) {
            playerPain[i] = new StreamedSoundSource(
                Gdx.files.internal("audio/sounds/player/hero_hurt_" + (i + 1) + ".wav")
            );
            configureSoundSource(playerPain[i], 1f, 1f);
            playerPain[i].detachAllEffects();
        }

        // Звук приземления
        downSound = new StreamedSoundSource(Gdx.files.internal("audio/sounds/player/down.wav"));
        configureSoundSource(downSound, 1f, 1f);
    }

    public boolean containsKey(String targetKey){
        if(!keys.isEmpty()){
            for(String key : keys){
                if(key.equals(targetKey)) return true;
            }
        }
        return false;
    }

    public void putKey(String key){
        keys.add(key);
        addKey(ItemsComponent.getInstance().findRegion(key));
    }

    // Конфигурация звукового источника
    private void configureSoundSource(StreamedSoundSource source, float pitch, float volume) {
        source.setRelative(true);
        source.setPitch(pitch);
        source.setVolume(volume);
        source.attachEffect(new SoundEffect(EaxReverb.generic()));
    }

    // Настройка элементов управления
    private void setupInputControls() {
        Gdx.input.setCursorCatched(true);
        Gdx.input.setCursorPosition(0, 0);
    }

    public WeaponContainer getmWeaponContainer(){
        return mWeaponContainer;
    }

    // Инициализация оружия
    public void initWeapons(Environment environment) {
        mWeaponContainer = new WeaponContainer(mCamera, environment);
        hudContainer = new HUD(new TextureAtlas(Gdx.files.internal("textures/atlases/hud.atlas")), mCamera);
    }

    public HUD getHUD(){
        return hudContainer;
    }

    public InputMultiplexer getInputMultiplexer(){
        return mWeaponContainer.getInputMultiplexer();
    }

    public HUD getHudContainer(){
        return hudContainer;
    }

    // Создание модели игрока
    public void initBody(Matrix4 transform) {
        if(playerBody != null){
            mPlayer.transform.set(transform);
            mCamera.rotate(transform);
            playerBody.setWorldTransform(transform);
            return;
        }


        ModelBuilder builder = new ModelBuilder();
        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE));

        Model model = builder.createCapsule(
            PlayerConfiguration.PLAYER_WEIGHT.getValue(),
            PlayerConfiguration.PLAYER_HEIGHT.getValue(),
            30,
            GL30.GL_TRIANGLES,
            material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        mPlayer = new ModelInstance(model);
        mPlayer.transform.set(transform);
        mCamera.rotate(transform);

        PhysicComponent.getInstance().addPlayerBody(mPlayer);

        playerBody = (PlayerBody) PhysicComponent.getInstance().getPlayerBody();
        playerBody.setHealth(100);
        playerBody.setArmor(0);
        playerBody.setStrength(10);
        playerBody.setAgility(10);
        playerBody.setVitality(1);
        playerBody.setIntelligence(10);

        previousHealth = playerBody.getCurrentHealth();
    }


    public void setPreviousHealth(int previousHealth){
        this.previousHealth = previousHealth;

        hudContainer.decreaseLive((float) playerBody.getCurrentHealth() / playerBody.getHealth(), playerBody.getCurrentHealth(), playerBody.getHealth());
        hudContainer.setHealth(String.valueOf(playerBody.getCurrentHealth()));
    }

    public Enemy getEnemy(){
        return playerBody;
    }

    public void addHealth(int health){
        playerBody.setCurrentHealth(Math.min(playerBody.getCurrentHealth() + health, playerBody.getHealth()));
        hudContainer.increaseLive((float) playerBody.getCurrentHealth() / playerBody.getHealth(), playerBody.getCurrentHealth(), playerBody.getHealth());
        hudContainer.setHealth(String.valueOf(playerBody.getCurrentHealth()));
    }

    // Поворот обзора
    public void rotateViewport() {
        if (ignoreNextMouseDelta) {
            ignoreNextMouseDelta = false;
            return; // Игнорируем первый вызов после паузы
        }

        float rx = Gdx.input.getDeltaX();
        float ry = Gdx.input.getDeltaY();

        mTemp.set(mCamera.direction).crs(mCamera.up).nor();
        float pitch = mCamera.direction.y;
        float sensivity = (Float) ControlsConfiguration.MOUSE_SENSIVITY.getValue();

        mCamera.rotate(Vector3.Y, -sensivity * rx);

        if ((ry > 0 && pitch > -0.99f) || (ry < 0 && pitch < 0.99f)) {
            mCamera.rotate(mTemp, -sensivity * ry);
        }
    }

    // Обновление состояния игрока
    public void update(float delta, boolean isPaused) {
        mCamera.fieldOfView = (float) ControlsConfiguration.FOV.getValue();
        updatePlayerMovement(delta, isPaused);
        mCamera.update();

        if(isPaused) return;

        rotateViewport();
        updatePhysics();
        updateCameraPosition(delta);
    }

    public void addDamage(float damage){
        playerBody.addDamage(damage);
    }

    // Обновление движения игрока
    private void updatePlayerMovement(float delta, boolean isPaused) {
        isOnGround = PhysicComponent.getInstance().checkOnGround();
        isStaying = PhysicComponent.getInstance().getLinearVelocity().len() > 9;
        isMoving = isPlayerMoving();

        handleJumping();
        handleWalkingSounds();
        handleWeaponContainer(delta, isPaused);
        updateRollAngle(delta);
    }

    private void handleCameraMovement(float delta){
        if(isMoving && isStaying && isOnGround){
            mAnimation += delta * PlayerConfiguration.SHAKE_FREQUENCY.getValue();
            mPosition.y += (float)(Math.sin(mAnimation) * PlayerConfiguration.SHAKE_AMPLITUDE.getValue());
            return;
        }
        mAnimation = 0f;
    }

    public void addKey(TextureRegion region){
        hudContainer.addKeySlot(region);
    }

    public void addWeapon(Weapon weapon) {
        mWeaponContainer.addWeapon(weapon);

        // Обновляем все слоты оружия в HUD
        hudContainer.clearWeaponSlots();
        Array<Weapon> weapons = mWeaponContainer.getWeapons();
        for (Weapon w : weapons) {
            hudContainer.addWeaponSlot(w.getIcon());
        }
    }

    private void handleWeaponContainer(float delta, boolean isPaused){

        if(mWeaponContainer.isEmpty()) return;
        float amplitude = isMoving && isOnGround && isStaying ? 1f : 0.1f;


        mWeaponContainer.setJumping(!isOnGround);
        mWeaponContainer.movementCondition(delta, amplitude, isPaused || ignoreNextMouseDelta);
        hudContainer.setBullets(mWeaponContainer.getBullets(), mWeaponContainer.getPercent());

        TextureRegion region = mWeaponContainer.getWeaponIcon();
        if(region != null)
            hudContainer.addWeaponIcon(mWeaponContainer.getWeaponIcon());
    }

    // Обработка прыжков
    private void handleJumping() {

        if (Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.JUMP.getValue()) && isOnGround) {

            if (!jumpSound[currentJumpSound].isPlaying()) {
                currentJumpSound = ThreadLocalRandom.current().nextInt(0, 2);
                jumpSound[currentJumpSound].setVolume(AudioConfiguration.SOUND.getValue());
                jumpSound[currentJumpSound].play();
            }
        }

        if (PhysicComponent.getInstance().getPlayerMotion().isDropped()) {
            downSound.setVolume(AudioConfiguration.SOUND.getValue());
            downSound.play();
        }
    }

    // Обработка звуков ходьбы
    private void handleWalkingSounds() {

        if (isMoving && isOnGround && isStaying) {
            if (!walkingSounds[currentWalkSound].isPlaying()) {
                if((currentWalkSound > 0 && walkingSounds[currentWalkSound-1].isPlaying())
                    || (currentWalkSound == 0 && walkingSounds[walkingSounds.length-1].isPlaying()))
                    return;

                walkingSounds[currentWalkSound].setVolume(AudioConfiguration.SOUND.getValue());
                walkingSounds[currentWalkSound].play();
                currentWalkSound = (currentWalkSound + 1) % walkingSounds.length;
            }
        }
    }

    // Проверка движения игрока
    private boolean isPlayerMoving() {
        return Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_FORWARD.getValue()) ||
               Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_BACKWARD.getValue()) ||
               Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_LEFT.getValue()) ||
               Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_RIGHT.getValue());
    }

    // Обновление угла наклона
    private void updateRollAngle(float delta) {
        float rollSpeed = PlayerConfiguration.ROLL_INCREMENT.getValue() * delta * 60f;
        float maxRoll = PlayerConfiguration.ROLL_ANGLE_MAX.getValue();

        if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_LEFT.getValue())) {
            rollAngle = MathUtils.clamp(rollAngle - rollSpeed, -maxRoll, maxRoll);
        } else if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_RIGHT.getValue())) {
            rollAngle = MathUtils.clamp(rollAngle + rollSpeed, -maxRoll, maxRoll);
        } else {
            resetRoll(delta);
        }

        mCamera.up.set(Vector3.Y);
        mCamera.rotate(mCamera.direction, rollAngle);
    }

    private void resetRoll(float delta) {
        float resetSpeed = 0.1f * delta * 60f; // Плавность сброса также зависит от delta
        rollAngle = MathUtils.lerp(rollAngle, 0, resetSpeed);
    }

    // Обработка тряски камеры
    private void handleCameraShake(float delta) {
        if (shakeTime < shakeDuration) {
            float offsetX = MathUtils.random(-shakeIntensity, shakeIntensity);
            float offsetY = MathUtils.random(-shakeIntensity, shakeIntensity);

            mPosition.add(offsetX, offsetY, 0);
            shakeTime += delta;
            shakeIntensity = Math.max(0, shakeIntensity - shakeDecay * delta);
        } else {
            shakeIntensity = 0f;
            shakeTime = 0f;
        }
    }

    // Обновление физики
    private void updatePhysics() {
        PhysicComponent.getInstance().updateCondition(mCamera);
    }

    // Обновление позиции камеры
    private void updateCameraPosition(float delta) {
        mPlayer.transform.getTranslation(mPosition);
        mPosition.y += PlayerConfiguration.PLAYER_HEIGHT.getValue() / 2.3f;

        handleCameraMovement(delta);
        handleCameraShake(delta);

        mCamera.position.set(mPosition);
    }

    public void deacreaseArmor(float damage){
        if(armorHealth > 0) {
            armorHealth -= damage;
            armorHealth = (int) armorHealth;
            if (armorHealth <= 0) {
                armorHealth = 0;
                playerBody.setArmor(0f);
            }
        }
    }

    // Запуск тряски
    public void startShake(float intensity, float duration) {
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTime = 0f;
    }

    public void feelPain(){
        StreamedSoundSource source = playerPain[ThreadLocalRandom.current().nextInt(0, playerPain.length)];
        source.setVolume(AudioConfiguration.SOUND.getValue());
        source.play();

        OverlaysComponent.getInstance().uploadOverlay(OverlaysComponent.getInstance().pain);
        hudContainer.decreaseLive((float) playerBody.getCurrentHealth() / playerBody.getHealth(), playerBody.getCurrentHealth(), playerBody.getHealth());
        hudContainer.setHealth(String.valueOf(playerBody.getCurrentHealth()));
    }

    // Геттеры
    public PerspectiveCamera getCamera() {
        return mCamera;
    }

    public ModelInstance getBody() {
        return mPlayer;
    }

    // Рендер оружия
    public void renderEntity(float delta, boolean isPaused) {

        float spread = mWeaponContainer.getSpreadCount();
        if(spread > 0)
            hudContainer.setOffsetCrosshair(spread);

        if(previousHealth != playerBody.getCurrentHealth()){
            if(previousHealth > playerBody.getCurrentHealth())
                feelPain();
            previousHealth = playerBody.getCurrentHealth();
        }

        hudContainer.setArmor(String.valueOf((int) Math.floor(armorHealth)), armorHealth / 100);
        hudContainer.setCurrentPosition(mWeaponContainer.getCurrentWeapon());
        mWeaponContainer.render(mCamera.position, delta, isPaused);
        hudContainer.render(delta, isPaused);
    }

    // Освобождение ресурсов
    @Override
    public void dispose() {
        // Dispose weapon container
        if (mWeaponContainer != null) {
            mWeaponContainer.dispose();
            mWeaponContainer = null;
        }

        // Dispose HUD container
        if (hudContainer != null) {
            hudContainer.dispose();
            hudContainer = null;
        }

        // Dispose player model
        if (mPlayer != null) {
            mPlayer.model.dispose();
            mPlayer = null;
        }

        // Dispose sound sources
        if (walkingSounds != null) {
            for (StreamedSoundSource sound : walkingSounds) {
                if (sound != null) {
                    sound.dispose();
                }
            }
            walkingSounds = null;
        }

        if (jumpSound != null) {
            for (StreamedSoundSource sound : jumpSound) {
                if (sound != null) {
                    sound.dispose();
                }
            }
            jumpSound = null;
        }

        if (playerPain != null) {
            for (StreamedSoundSource sound : playerPain) {
                if (sound != null) {
                    sound.dispose();
                }
            }
            playerPain = null;
        }

        if (downSound != null) {
            downSound.dispose();
            downSound = null;
        }

        // Return vectors to pool
        if (mPosition != null) {
            PhysicComponent.getInstance().getVectorPool().free(mPosition);
            mPosition = null;
        }

        if (mTemp != null) {
            PhysicComponent.getInstance().getVectorPool().free(mTemp);
            mTemp = null;
        }

        // Clear other references
        mCamera = null;
        playerBody = null;
        keys.clear();
    }

    public void applyRotation(Vector3 cameraUp, Vector3 cameraDirection) {
        mCamera.up.set(cameraUp);
        mCamera.direction.set(cameraDirection);
        mCamera.update();
    }
}
