package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.CustomInputProcessor;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.weapons.BalisticWeapon;
import com.hexedrealms.weapons.ShootWeapon;
import com.hexedrealms.weapons.Weapon;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class WeaponContainer implements Disposable {
    public final static float SHAKE_MOVING_FREQUENCY = 8f;
    public final static float SHAKE_MOVING_AMPLITUDE = 60f;
    public final static float SCALE = 2.6f;
    public final static float INERTIA = 1.2f;

    private static final int [] hotKeys = new int[] {
        Input.Keys.NUM_1,
        Input.Keys.NUM_2,
        Input.Keys.NUM_3,
        Input.Keys.NUM_4,
        Input.Keys.NUM_5,
    };

    private Array<Weapon> weapons;
    private TextureRegion mTexture;
    private SpriteBatch mContainer;
    private Vector2 mPosition, mVelocity, mSway, mStartPosition;
    private PointLightEx shoot;
    private Camera camera;
    private CustomInputProcessor inputProcessor;
    private InputMultiplexer inputMultiplexer;
    private float mCenterX, mScaleX, mScaleY, offsetY, offsetX, elapsedTime;
    private int currentWeapon, previousWeapon = -1;
    private boolean isJumping, isHotSwap;

    public WeaponContainer(Camera camera, Environment environment){
        weapons = new Array<>();

        inputProcessor = new CustomInputProcessor();
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);

        shoot = LightingComponent.getInstance().getPointLightPool().obtain();
        this.camera = camera;
        this.mSway = new Vector2(0,0);
        this.mContainer = new SpriteBatch();
        this.mVelocity = new Vector2();

        environment.add(shoot);
        LightingComponent.getInstance().getPointLightArray().add(shoot);

        if(weapons.size > 0) init();
    }

    public Array<Weapon> getWeapons(){
        return weapons;
    }

    private void init(){
        mTexture = weapons.get(currentWeapon).getFrame();

        float aspectRatio = (float) mTexture.getRegionWidth() / mTexture.getRegionHeight();
        float screenWidth = Gdx.graphics.getWidth();

        mScaleX = screenWidth / SCALE;
        mScaleY = mScaleX / aspectRatio;

        this.mCenterX = screenWidth / 2 - mScaleX / 3.5f;
        this.mPosition = new Vector2(mCenterX, 0);
        this.mStartPosition = new Vector2(mPosition);
    }

    public boolean contains(Weapon weapon){
        return weapons.contains(weapon, true);
    }

    public boolean isEmpty(){
        return weapons.isEmpty();
    }

    public InputMultiplexer getInputMultiplexer(){
        return inputMultiplexer;
    }

    public void setJumping(boolean jumping) {
        this.isJumping = jumping;
    }

    public void movementCondition(float delta, float scl, boolean isPaused) {
        if (!weapons.get(currentWeapon).isStatic()) {
            elapsedTime = 0;
            mVelocity.set(0, 0);
            return;
        }

        // Гравитация, если персонаж прыгает
        if (isJumping)
            mVelocity.y += Math.sin(delta * 5) * 30f;

        mVelocity.x *= 0.95f;
        mVelocity.y *= 0.95f;

        // Обновление времени (для стабильных колебаний)
        elapsedTime += delta;  // Время увеличивается на величину delta (независимо от FPS)

        // Вычисление амплитуды колебаний (независимо от FPS)

        float shakeX = (float) Math.cos(elapsedTime * (SHAKE_MOVING_FREQUENCY * scl)) * (SHAKE_MOVING_AMPLITUDE * scl);
        float shakeY = (float) Math.sin(elapsedTime * (SHAKE_MOVING_FREQUENCY * scl)) * (SHAKE_MOVING_AMPLITUDE * scl);
        if(isPaused)
            shakeX = shakeY = 0;

        // Смещение мыши с учетом инерции
        float mouseX = Gdx.input.getDeltaX();
        float mouseY = Gdx.input.getDeltaY();
        if(!isPaused)
            mSway.set(-mouseX * INERTIA, -mouseY * INERTIA);

        // Добавляем колебания и инерцию от мыши
        Vector2 targetPosition = mStartPosition.cpy().add(mSway).add(shakeX, Math.abs(shakeY));
        mPosition.lerp(targetPosition, delta * 10f);

        // Ограничиваем максимальную скорость
        float maxSpeed = 60;
        if (mVelocity.len() > maxSpeed) {
            mVelocity.nor().scl(maxSpeed);
        }
    }

    public float getPercent(){
        Weapon current = weapons.get(currentWeapon);
        if(current instanceof ShootWeapon){
            ShootWeapon shootWeapon = (ShootWeapon) current;
            return (float) shootWeapon.getTotalBullets() / shootWeapon.getMaxBullets();
        }else if(current instanceof BalisticWeapon){
            BalisticWeapon balisticWeapon = (BalisticWeapon) current;
            return balisticWeapon.getPercent();
        }
        return 0;
    }

    public float getSpreadCount(){
        if(weapons.isEmpty()) return 0;
        return weapons.get(currentWeapon) instanceof ShootWeapon ? ((ShootWeapon) weapons.get(currentWeapon)).getActualSpreadCount() : 0;
    }

    private void decreaseLight(float delta){
        int index = LightingComponent.getInstance().getPointLightArray().indexOf(shoot, true);
        Level.getInstance().updateEnvironment(index);

        float itens = shoot.intensity;
        if(itens > 0){
            itens -= delta * 200f;
            shoot.setIntensity(itens);
        }
    }

    public int getWeaponCount(){
        return weapons.size;
    }

    public void addWeapon(Weapon weapon) {
        if (weapons.contains(weapon, true)) return;

        // Получаем список всех доступных оружий для определения порядка
        Array<Weapon> allWeapons = ResourcesLoader.getAllWeapons();

        // Добавляем новое оружие
        weapons.add(weapon);

        // Сортируем оружие согласно порядку в allWeapons
        weapons.sort((w1, w2) -> {
            int index1 = allWeapons.indexOf(w1, true);
            int index2 = allWeapons.indexOf(w2, true);
            return Integer.compare(index1, index2);
        });

        // Инициализируем контейнер и переключаемся на новое оружие
        init();

        setTargetWeapon(weapons.indexOf(weapon, true));
    }

    public void setTargetWeapon(int index){
        Weapon current = weapons.get(currentWeapon);
        current.stopAnimation();
        currentWeapon = index;
        switchWeapon(current);
    }

    public TextureRegion getWeaponIcon(){

        if(currentWeapon == -1) return null;

        return weapons.get(currentWeapon).getIcon();
    }

    private void handleSwitchWeapons() {
        if (weapons.isEmpty()) return;

        Weapon current = weapons.get(currentWeapon);

        if(previousWeapon == -1 && Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.FAST_GUN.getValue())){
            Weapon weapon = weapons.get(0);
            if(weapon instanceof ShootWeapon || weapon instanceof BalisticWeapon) return;

            previousWeapon = currentWeapon;
            isHotSwap = true;
            switchToWeapon(0, current);
        }

        // Обработка горячих клавиш
        if (inputProcessor.keyCode != -1) {
            int index = IntStream.range(0, hotKeys.length)
                .filter(i -> hotKeys[i] == inputProcessor.keyCode)
                .findFirst()
                .orElse(-1);

            if(index == currentWeapon) return;

            if (index >= 0 && index < weapons.size) {
                switchToWeapon(index, current);
                return;
            }
        }

        // Обработка скролла колесика
        if (inputProcessor.getScrolledY() != 0) {
            int newIndex = (currentWeapon - (int) Math.signum(inputProcessor.getScrolledY()) + weapons.size) % weapons.size;
            switchToWeapon(newIndex, current);
        }
    }

    private void switchToWeapon(int newIndex, Weapon currentWeapon) {
        currentWeapon.stopAnimation();
        this.currentWeapon = newIndex;
        switchWeapon(currentWeapon);
        inputProcessor.clearScroll();
    }

    private void switchWeapon(Weapon weapon){
        weapon = weapons.get(currentWeapon);
        if(Level.getInstance().isShowed)
            weapon.playSelect();

        if (weapon instanceof ShootWeapon){
            ShootWeapon shootWeapon = (ShootWeapon) weapon;
            shootWeapon.Reload();
        }

        calculateParams();
        mPosition.set(mStartPosition.x, isHotSwap ? 0 : -mScaleY);
        inputProcessor.clearScroll();
    }

    private void handleWeaponFire(Vector3 position){
        Weapon weapon = weapons.get(currentWeapon);
        if(!weapon.isStatic()) return;

        if (weapon instanceof ShootWeapon) {
            ShootWeapon currentWeapon = (ShootWeapon) weapon;
            if (currentWeapon.getCurrentBullets() > 0) {
                increaseWeaponLight(currentWeapon.getShootColor(), position);
                fireWeapon(currentWeapon);
            }
        } else if (weapon instanceof BalisticWeapon) {

            BalisticWeapon currentWeapon = (BalisticWeapon) weapon;
            if (currentWeapon.getCurrentBullets() > 0)
                increaseWeaponLight(currentWeapon.getShootColor(), position);

        }

        weapon.Shoot(camera);
    }

    private void fireWeapon(Weapon weapon) {
        Vector2 shake = ((ShootWeapon) weapon).getDurationShaker();
        if (shake != null) Level.getInstance().getPlayer().startShake(shake.x, shake.y);
    }

    private boolean canFire(){
        Weapon weapon = weapons.get(currentWeapon);
        boolean isClicked = false;

        isClicked = Gdx.input.isButtonPressed((Integer) ControlsConfiguration.ATACK.getValue());

        return mPosition.y + weapons.get(currentWeapon).getyOffsetCustom() >= -30 + weapons.get(currentWeapon).getyOffsetCustom() && isClicked;
    }

    private void UpdateFrame(Vector3 position, float delta, boolean isPaused){
        if(weapons.isEmpty()) return;
        // Обработка смены оружия
        if(!isPaused) {

            handleSwitchWeapons();
            Weapon weapon = weapons.get(currentWeapon);

            if(Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.RELOAD.getValue()) && weapon instanceof ShootWeapon){
                ShootWeapon shootWeapon = (ShootWeapon) weapon;
                shootWeapon.consumeReload();
            }

            if (canFire() || isHotSwap){
                handleWeaponFire(position);

                if(isHotSwap) {
                    isHotSwap = false;
                }
            }
            else if (weapon instanceof ShootWeapon)
                ((ShootWeapon) weapons.get(currentWeapon)).stopShooting();

            decreaseLight(delta);  // Уменьшаем интенсивность света
        }

        if(!weapons.get(currentWeapon).isShooting() && previousWeapon > -1){
            currentWeapon = previousWeapon;
            previousWeapon = -1;
        }

        weapons.get(currentWeapon).Render(delta);  // Отображаем оружие
        mTexture = weapons.get(currentWeapon).getFrame();  // Обновляем текстуру оружия

        calculateParams();  // Пересчитываем параметры для текущего оружия
    }

    public String getBullets(){
        Weapon weapon = weapons.get(currentWeapon);

        if(weapon instanceof ShootWeapon){
            ShootWeapon shootWeapon = (ShootWeapon) weapon;
            return shootWeapon.getCurrentBullets() + "/" +shootWeapon.getShopBullets();
        } else if (weapon instanceof BalisticWeapon) {
            BalisticWeapon wizardWeapon = (BalisticWeapon) weapon;
            return wizardWeapon.getCurrentBullets() +"";
        }
        return null;
    }

    private void increaseWeaponLight(Color color, Vector3 position){
        shoot.setPosition(position);
        shoot.setIntensity(70f);
        shoot.setColor(color);
    }

    private void calculateParams() {
        float scale = weapons.get(currentWeapon).getScaleViewed();
        float aspectRatio = (float) mTexture.getRegionWidth() / mTexture.getRegionHeight();
        float screenWidth = Gdx.graphics.getWidth();

        mScaleX = screenWidth / SCALE * scale;
        mScaleY = mScaleX / aspectRatio;

        mCenterX = screenWidth / 2 - mScaleX / 3.6f;
        mStartPosition.x = mCenterX;

        offsetY = mScaleY * 0.18f;
        offsetY -= weapons.get(currentWeapon).getyOffsetCustom();
        if (scale != 1f)
            offsetX = mScaleX * (0.05f + scale * 0.01f) + weapons.get(currentWeapon).getxOffsetCustom();
        else
            offsetX = 0;
    }

    public void render(Vector3 position, float delta, boolean isPaused) {
        if(Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0 || weapons.isEmpty()) return;
        UpdateFrame(position, delta, isPaused);
        mContainer.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        mContainer.begin();
        mContainer.draw(mTexture, mPosition.x + mVelocity.x - offsetX, Math.min(mPosition.y + mVelocity.y - offsetY, -10), mScaleX,mScaleY);
        mContainer.end();
    }

    public int getCurrentWeapon(){
        return currentWeapon;
    }

    @Override
    public void dispose() {
        for(Weapon weapon : weapons)
            weapon.dispose();

        mContainer.dispose();
        if(mTexture != null)
            mTexture.getTexture().dispose();

        weapons.clear();
        weapons = null;
        mTexture = null;
        mContainer = null;
        mPosition = null;
        mVelocity = null;
        mSway = null;
        mStartPosition = null;
        shoot = null;
        camera = null;
        inputProcessor = null;
    }
}
