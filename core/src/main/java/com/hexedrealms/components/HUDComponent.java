package com.hexedrealms.components;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class HUDComponent {
    protected TextureRegion region;
    protected TextureRegion clipRegion;
    protected Vector2 position;
    protected Vector2 originalSize;  // Оригинальные размеры (без клипа)
    protected Vector2 currentSize;   // Текущие отображаемые размеры (с учетом клипа)
    protected boolean isEnabled = true;
    protected final Array<HUDComponent> children;
    protected float clipFactorX = 1f; // Коэффициент клипа по X (0-1)
    protected float clipFactorY = 1f; // Коэффициент клипа по Y (0-1)

    public HUDComponent(TextureRegion region, int x, int y, int width, int height) {
        if(region != null) {
            this.region = new TextureRegion(region);
            this.clipRegion = new TextureRegion(region);
        }
        this.position = new Vector2(x, y);
        this.originalSize = new Vector2(width, height);
        this.currentSize = new Vector2(width, height);
        this.children = new Array<>();
    }

    public void resize(float parentWidth, float parentHeight) {
        // Обновляем текущие размеры с учетом клипа
        currentSize.set(
            originalSize.x * clipFactorX,
            originalSize.y * clipFactorY
        );

        // Обновляем дочерние элементы
        for (HUDComponent child : children) {
            child.resize(currentSize.x, currentSize.y);
        }
    }

    public int getChildrenCount() {
        return children.size;
    }

    public void setConfig(HUDComponent component) {
        this.position.set(component.position);
        this.originalSize.set(component.originalSize);
        this.currentSize.set(component.currentSize);
    }

    public void setX(float value) {
        position.x = value;
    }

    public void setRegion(TextureRegion region) {
        if (region == null) return;

        this.region = new TextureRegion(region);
        this.clipRegion = new TextureRegion(region);

        // Сохраняем пропорции
        float widthRatio = currentSize.x / originalSize.x;
        float heightRatio = currentSize.y / originalSize.y;

        originalSize.set(
            region.getRegionWidth(),
            region.getRegionHeight()
        );

        currentSize.set(
            originalSize.x * widthRatio,
            originalSize.y * heightRatio
        );

        applyClipping();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public HUDComponent addChild(HUDComponent child, float percent, float relativeOffsetX, float relativeOffsetY) {
        if (child.getRegion() == null || children.contains(child, true)) return null;

        float childAspectRatio = (float) child.getRegion().getRegionWidth() / child.getRegion().getRegionHeight();
        float parentWidth = getCurrentSize().x;
        float parentHeight = getCurrentSize().y;
        float scaleX = parentWidth * percent;
        float scaleY = scaleX / childAspectRatio;
        float offsetXPercent = relativeOffsetX / 100f;
        float offsetYPercent = relativeOffsetY / 100f;
        float absoluteOffsetX = parentWidth * offsetXPercent;
        float absoluteOffsetY = parentHeight * offsetYPercent;

        child.getPosition().set(
            getPosition().x + absoluteOffsetX,
            getPosition().y + absoluteOffsetY
        );

        child.originalSize.set(scaleX, scaleY);
        child.currentSize.set(scaleX, scaleY);
        children.add(child);
        return this;
    }

    public void setClip(float widthFactor, float heightFactor) {
        // Ограничиваем значения клипа от 0 до 1
        this.clipFactorX = Math.max(0, Math.min(1, widthFactor));
        this.clipFactorY = Math.max(0, Math.min(1, heightFactor));

        applyClipping();
    }

    private void applyClipping() {
        if (region == null) return;

        // Обновляем регион клипа
        int originalX = region.getRegionX();
        int originalY = region.getRegionY();
        int originalWidth = region.getRegionWidth();
        int originalHeight = region.getRegionHeight();

        clipRegion.setRegion(
            originalX,
            originalY,
            (int)(originalWidth * clipFactorX),
            (int)(originalHeight * clipFactorY)
        );

        // Обновляем текущие размеры с учетом клипа
        currentSize.set(
            originalSize.x * clipFactorX,
            originalSize.y * clipFactorY
        );
    }

    public TextureRegion getRegion() {
        return region;
    }

    public Vector2 getOriginalSize() {
        return originalSize;
    }

    public void setSize(float width, float height){
        currentSize.x = width;
        currentSize.y = height;
    }

    public Vector2 getCurrentSize() {
        return currentSize;
    }

    public Vector2 getPosition() {
        return position;
    }

    public void render(SpriteBatch batch, boolean isPaused) {
        if (!isEnabled) return;

        TextureRegion renderRegion = (clipFactorX < 1f || clipFactorY < 1f) ? clipRegion : region;
        if (renderRegion == null) return;

        batch.draw(renderRegion, position.x, position.y, currentSize.x, currentSize.y);

        if (!children.isEmpty()) {
            for (HUDComponent component : children) {
                component.render(batch, isPaused);
            }
        }
    }
}
