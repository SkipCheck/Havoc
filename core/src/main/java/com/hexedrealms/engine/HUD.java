package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.hexedrealms.components.HUDComponent;
import com.hexedrealms.components.HUDCrossHair;
import com.hexedrealms.components.HUDFace;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.components.bulletbodies.TriggerBody;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.Enemy;
import com.hexedrealms.visuicomponents.VisUIHud;

public class HUD {
    public static final float SCALE = 3.75f;
    private final TextureAtlas hudAtlas;
    private final Camera camera;
    private final ClosestRayResultCallback callback;
    private final Vector3 from;
    private final Vector3 to;

    private SpriteBatch hudRenderer;
    private Array<HUDComponent> components;
    private Array<HUDComponent> items, keys;

    private HUDFace hudFace;
    private HUDComponent healthComponent;
    private HUDComponent defComponent;
    private HUDComponent bulletsComponent;
    private HUDComponent weaponIcon;
    private HUDComponent weaponStats;
    private HUDComponent enemyHealth;
    private HUDComponent person;
    private HUDComponent enemyBar;
    private HUDCrossHair crossHair;
    private HUDComponent slot;

    private VisUIHud hudStats;
    private Enemy previousEnemy;

    private float widthSlot;
    private float heightSlot;
    private float xInventory, xKey, widthKey, heightKey;
    private int countItems;
    private float timer;
    private float delta;

    public HUD(TextureAtlas atlas, Camera camera) {
        this.hudAtlas = atlas;
        this.camera = camera;

        hudRenderer = new SpriteBatch();
        crossHair = new HUDCrossHair();
        items = new Array<>();
        keys = new Array<>();

        PhysicComponent instance = PhysicComponent.getInstance();
        this.callback = instance.getRaycastCallbackPool().obtain();
        this.from = instance.getVectorPool().obtain();
        this.to = instance.getVectorPool().obtain();

        calculateSlotSizes();
        initializeComponents();
    }

    private void calculateSlotSizes() {
        widthSlot = Gdx.graphics.getWidth() * 0.08f;
        heightSlot = Gdx.graphics.getHeight() * 0.05f;
        widthKey = Gdx.graphics.getWidth() * 0.03f;
        heightKey = Gdx.graphics.getHeight() * 0.025f;
        xInventory = 10;
        xKey = Gdx.graphics.getWidth() * 0.092f;
    }

    // HUD.java
    public void resize(int width, int height) {
        // 1. Обновляем базовые параметры
        calculateSlotSizes();

        // 2. Обновляем основные компоненты
        updateComponentsSize();

        // 3. Обновляем слоты оружия
        xInventory = 10;
        for (HUDComponent item : items) {
            item.getPosition().set(xInventory, height - heightSlot * 1.35f);
            item.getOriginalSize().set(widthSlot, heightSlot);
            item.resize(widthSlot, heightSlot);
            xInventory += widthSlot * 1.1f;
        }

        // 4. Обновляем выделение слота
        if (!items.isEmpty()) {
            slot.setX(items.get(countItems - 1).getPosition().x - 10);
            slot.getPosition().y = height - heightSlot * 1.8f;
        }

        // 5. Обновляем прицел
        crossHair.resize(width, height);

        // 6. Обновляем матрицу проекции
        hudRenderer.getProjectionMatrix().setToOrtho2D(0, 0, width, height);

        if (weaponIcon != null && weaponIcon.getRegion() != null) {
            TextureRegion region = weaponIcon.getRegion();
            float textureAspectRatio = (float) region.getRegionWidth() / region.getRegionHeight();

            float baseSize = width * 0.13f;
            float newWidth, newHeight;

            if (textureAspectRatio > 1) {
                newWidth = baseSize;
                newHeight = baseSize / textureAspectRatio;
            } else {
                newHeight = baseSize;
                newWidth = baseSize * textureAspectRatio;
            }

            weaponIcon.setSize((int) newWidth, (int) newHeight);
        }
    }

    private void updateComponentsSize() {
        // Персонаж
        updateComponent(person, hudAtlas.findRegion("person"), 0, 0, false, false);
        updateChild(person, hudFace, 0.27f, 1.8f, 8);
        updateChild(person, healthComponent, 0.4f, 51.9f, 8);
        updateChild(person, defComponent, 0.35f, 49.9f, 28);

        // Панель врага
        updateComponent(enemyBar, hudAtlas.findRegion("health_bar"), 0, Gdx.graphics.getHeight() - 100, false, true);
        updateChild(enemyBar, enemyHealth, 1f, 0f, 12f);

        // Статистика оружия
        updateComponent(weaponStats, hudAtlas.findRegion("weapon"), Gdx.graphics.getWidth(), 0, true, false);
        updateChild(weaponStats, bulletsComponent, 0.62f, 15f, 11f);

        // Слот выделения
        slot.setSize(
            (int)(widthSlot * 1.15f),
            (int)(heightSlot * 1.75f)
        );
    }

    private void updateComponent(HUDComponent component, TextureRegion region, int x, int y, boolean autoCalc, boolean center) {
        float aspectRatio = (float) region.getRegionWidth() / region.getRegionHeight();
        float screenWidth = Gdx.graphics.getWidth();
        float scaleX = screenWidth / SCALE;
        float scaleY = scaleX / aspectRatio;

        if (autoCalc) x -= scaleX;
        if (center) x = (int) ((screenWidth - scaleX) / 2);

        component.setRegion(region);
        component.getPosition().set(x, y);
        component.getOriginalSize().set(scaleX, scaleY);
        component.resize(scaleX, scaleY);
    }

    private void updateChild(HUDComponent parent, HUDComponent child, float percent, float relOffsetX, float relOffsetY) {
        float aspectRatio = (float) child.getRegion().getRegionWidth() / child.getRegion().getRegionHeight();
        float parentWidth = parent.getCurrentSize().x;
        float parentHeight = parent.getCurrentSize().y;

        float width = parentWidth * percent;
        float height = width / aspectRatio;

        float offsetX = parentWidth * (relOffsetX / 100f);
        float offsetY = parentHeight * (relOffsetY / 100f);

        child.getPosition().set(
            parent.getPosition().x + offsetX,
            parent.getPosition().y + offsetY
        );

        child.getOriginalSize().set(width, height);
        child.resize(width, height);
    }

    private void initializeComponents() {
        components = new Array<>();

        // Основные компоненты
        person = createComponent(hudAtlas.findRegion("person"), 0, 0, false, false);
        person
            .addChild(
                hudFace = new HUDFace(new TextureAtlas(Gdx.files.internal("textures/atlases/face/face.atlas")), 0,0, 150, 150)
                , 0.27f, 1.8f, 8)
            .addChild(
                healthComponent = createComponent(hudAtlas.findRegion("health"), 0,0, false, false)
                , 0.4f, 51.9f, 8)
            .addChild(
                defComponent = createComponent(hudAtlas.findRegion("def"), 0,0, false, false)
                , 0.35f, 49.9f, 28);

        // Панель здоровья врага
        enemyBar = createComponent(hudAtlas.findRegion("health_bar"), 0, Gdx.graphics.getHeight() - 100, false, true);
        enemyBar.addChild(
            enemyHealth = createComponent(hudAtlas.findRegion("health_point"), 0, 0, false, false),
            1f, 0f, 12f);
        enemyBar.setEnabled(false);

        // Статистика оружия
        weaponStats = createComponent(hudAtlas.findRegion("weapon"), Gdx.graphics.getWidth(), 0, true, false);
        weaponStats.addChild(
            bulletsComponent = createComponent(hudAtlas.findRegion("bullets"), 0, 0, false, false),
            0.62f, 15f, 11f);
        bulletsComponent.setEnabled(false);

        // Иконка оружия
        Vector2 size = weaponStats.getOriginalSize();
        Vector2 position = weaponStats.getPosition();
        int iconWidth = (int)(Gdx.graphics.getWidth() * 0.13f);
        int iconHeight = (int)(Gdx.graphics.getHeight() * 0.09f);
        weaponIcon = new HUDComponent(null,
            (int)(position.x + size.x * 0.3f),
            (int)(position.y + size.y * 0.8f),
            iconWidth, iconHeight);

        // Слоты инвентаря
        slot = new HUDComponent(hudAtlas.findRegion("slot"),
            0,
            (int)(Gdx.graphics.getHeight() - heightSlot * 1.8f),
            (int)(widthSlot * 1.15f),
            (int)(heightSlot * 1.75f));
        slot.setEnabled(false);

        // Добавляем компоненты
        components.add(person);
        components.add(enemyBar);
        components.add(weaponStats);
        components.add(weaponIcon);
        components.add(slot);

        // Инициализация UI статистики
        hudStats = new VisUIHud();
        hudStats.setArmor("100", new Vector2(175, 75));
        hudStats.setHealth("100", new Vector2(175, 25));
        hudStats.setBullets("", new Vector2(VisUIComponent.getInstance().getStage().getViewport().getWorldWidth() - 90, 40));
        VisUIComponent.getInstance().appendActor(hudStats);
    }

    private HUDComponent createComponent(TextureRegion region, int x, int y, boolean autoCalc, boolean center) {
        float aspectRatio = (float)region.getRegionWidth() / region.getRegionHeight();
        float screenWidth = Gdx.graphics.getWidth();
        float scaleX = screenWidth / SCALE;
        float scaleY = scaleX / aspectRatio;

        if (autoCalc) x -= scaleX;
        if (center) x = (int)((screenWidth - scaleX) / 2);

        return new HUDComponent(region, x, y, (int)scaleX, (int)scaleY);
    }

    public void setHealth(String health) {
        hudStats.health.setText(health);
    }

    public void setArmor(String armor, float percent) {
        hudStats.armor.setText(armor);
        defComponent.setEnabled(armor != null);
        defComponent.setClip(percent, 1f);
    }

    public void setBullets(String bullets, float percent) {
        hudStats.bullets.setText(bullets);
        bulletsComponent.setEnabled(bullets != null);
        bulletsComponent.setClip(percent, 1f);
    }

    public void setMessage(String message, Color color){
        hudStats.message.setText(message);
        hudStats.message.setColor(color);
        timer = 0;
    }

    public void setOffsetCrosshair(float offsetCrosshair) {
        crossHair.setOffsetSpread(new Vector2(offsetCrosshair, offsetCrosshair));
    }

    public void increaseLive(float percent, int currentHealth,int maxHealth){
        hudFace.increaseLive(currentHealth, maxHealth);
        healthComponent.setClip(percent, 1f);
    }

    public void decreaseLive(float percent, int currentHealth,int maxHealth) {
        hudFace.activatePain(currentHealth, maxHealth);
        healthComponent.setClip(percent, 1f);
    }

    private float calcHealth(Enemy enemy) {
        return (float)enemy.getCurrentHealth() / enemy.getHealth();
    }

    private void checkEnemy() {
        from.set(camera.position);
        to.set(from).add(camera.direction.cpy().scl(150f));
        PhysicComponent.getInstance().isRaycastedCustomCallbackRay(from, to, callback);

        if (callback.hasHit() && callback.getCollisionObject() instanceof Enemy &&
            !(callback.getCollisionObject() instanceof PlayerBody)) {

            previousEnemy = (Enemy)callback.getCollisionObject();
            enemyBar.setEnabled(true);
            enemyHealth.setClip(calcHealth(previousEnemy), 1f);
            timer = 0f;
            return;
        }

        if(callback.hasHit() && callback.getCollisionObject() instanceof TriggerBody){

            TriggerBody body = (TriggerBody) callback.getCollisionObject();
            Vector3 position = body.getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain());

            if(position.dst(camera.position) < 3 && body.isTriggerActived()){
                setMessage("Нажмите "+Input.Keys.toString((Integer) ControlsConfiguration.EVENT.getValue()), Color.WHITE);

                if(Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.EVENT.getValue())){
                    body.activateTrigger();
                    body.disposeTrigger();
                }
            }
        }

        if (timer > 2f) {
            enemyBar.setEnabled(false);
            timer = 0f;
        }

        if (enemyBar.isEnabled()) {
            enemyHealth.setClip(calcHealth(previousEnemy), 1f);
            timer += delta;
        }
    }

    public int getCurrentWeaponCount() {
        return countItems;
    }

    public void clearWeaponSlots() {
        for (HUDComponent component : items) {
            components.removeValue(component, true);
        }
        items.clear();
        xInventory = 10;
        countItems = 0;
        slot.setEnabled(false);
    }

    public void addKeySlot(TextureRegion region){
        Vector2 size = person.getCurrentSize();
        HUDComponent component = new HUDComponent(region,
            (int)xKey,
            (int)(size.y - size.y * 0.25f),
            (int)widthKey,
            (int)heightKey);

        xKey += widthKey * 1.62f;
        components.add(component);
    }

    public void addWeaponSlot(TextureRegion region) {
        slot.setEnabled(true);
        crossHair.setEnabled(true);

        HUDComponent component = new HUDComponent(region,
            (int)xInventory,
            (int)(Gdx.graphics.getHeight() - heightSlot * 1.35f),
            (int)widthSlot,
            (int)heightSlot);

        xInventory += widthSlot * 1.1f;
        items.add(component);
        components.add(component);
        countItems++;
    }

    public void render(float delta, boolean isPaused) {
        if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) return;

        this.delta = delta;
        checkEnemy();

        crossHair.render(delta);
        hudRenderer.begin();

        for (HUDComponent component : components) {
            if (component.isEnabled()) {
                component.render(hudRenderer, isPaused);
            }
        }

        hudRenderer.end();
    }

    public void setCurrentPosition(int currentPosition) {
        if (items.isEmpty() || currentPosition < 0 || currentPosition >= items.size) return;
        slot.setX(items.get(currentPosition).getPosition().x - 10);
    }

    public void addWeaponIcon(TextureRegion region) {
        if (weaponIcon != null && region != null) {
            weaponIcon.setRegion(region);

            // Сохраняем оригинальные пропорции текстуры
            float textureAspectRatio = (float) region.getRegionWidth() / region.getRegionHeight();

            // Вычисляем базовый размер на основе разрешения экрана
            float baseSize = Gdx.graphics.getWidth() * 0.13f;

            // Рассчитываем размеры с сохранением пропорций
            float width, height;
            if (textureAspectRatio > 1) {
                // Широкие текстуры
                width = baseSize;
                height = baseSize / textureAspectRatio;
            } else {
                // Высокие или квадратные текстуры
                height = baseSize;
                width = baseSize * textureAspectRatio;
            }

            // Устанавливаем размер с учетом пропорций
            weaponIcon.setSize((int) width, (int) height);
        }
    }

    public void dispose() {
        hudRenderer.dispose();
    }
}
