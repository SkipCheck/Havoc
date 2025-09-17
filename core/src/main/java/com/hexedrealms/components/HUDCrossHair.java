package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.hexedrealms.configurations.CrossHairConfiguration;

public class HUDCrossHair extends Actor {
    private ShapeRenderer shapeRenderer;
    private Vector2 offsetSpread, start;
    private float centerX;
    private float centerY;

    // Эталонное разрешение (базовое разрешение игры)
    private static final float REFERENCE_WIDTH = 1920f;
    private static final float REFERENCE_HEIGHT = 1080f;
    private static final float REFERENCE_SCALE = 1.0f;

    private boolean enabled;

    public HUDCrossHair() {
        shapeRenderer = new ShapeRenderer();
        start = new Vector2();
        offsetSpread = new Vector2();
        this.centerX = Gdx.graphics.getWidth() / 2f;
        this.centerY = Gdx.graphics.getHeight() / 2f;
    }

    public void resize(int width, int height) {
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0,0, width, height);
    }

    public void setOffsetSpread(Vector2 offsetSpread) {
        this.offsetSpread.set(offsetSpread);
    }

    @Override
    public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
        batch.end();
        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        render(Gdx.graphics.getDeltaTime());
        Gdx.gl.glDisable(GL30.GL_BLEND);
        batch.begin();
    }

    public void render(float delta) {
        if(Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) return;

        if(!enabled) return;
        if (offsetSpread.len() > 0)
            offsetSpread.lerp(start, delta * 10f);

        // Рассчитываем коэффициент масштабирования
        float scale = calculateScale();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        drawCenterCrossOutline(scale);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor((Color) CrossHairConfiguration.COLOR.getValue());
        drawCenterCross(scale);
        shapeRenderer.end();
    }

    private float calculateScale() {
        // Рассчитываем коэффициент масштабирования как среднее геометрическое
        float widthRatio = REFERENCE_WIDTH / Gdx.graphics.getWidth();
        float heightRatio = REFERENCE_HEIGHT / Gdx.graphics.getHeight();

        // Среднее геометрическое для равномерного масштабирования
        return (float) Math.sqrt(widthRatio * heightRatio);
    }

    private void drawCenterCross(float scale) {
        // Применяем обратное масштабирование
        float distance = (Float) CrossHairConfiguration.DISTANCE.getValue() * scale;
        float size = (Float) CrossHairConfiguration.SIZE.getValue() * scale;
        float thickness = (Float) CrossHairConfiguration.THICKNESS.getValue() * scale;

        shapeRenderer.circle(centerX, centerY, 2 * scale);

        // Горизонтальные полоски
        shapeRenderer.rect(
            centerX - (distance + size) - offsetSpread.x * scale,
            centerY - thickness / 2,
            size,
            thickness
        );
        shapeRenderer.rect(
            centerX + distance + offsetSpread.x * scale,
            centerY - thickness / 2,
            size,
            thickness
        );

        // Вертикальные полоски
        shapeRenderer.rect(
            centerX - thickness / 2,
            centerY - (distance + size) - offsetSpread.y * scale,
            thickness,
            size
        );
        shapeRenderer.rect(
            centerX - thickness / 2,
            centerY + distance + offsetSpread.y * scale,
            thickness,
            size
        );
    }

    private void drawCenterCrossOutline(float scale) {
        float distance = (Float) CrossHairConfiguration.DISTANCE.getValue() * scale;
        float size = (Float) CrossHairConfiguration.SIZE.getValue() * scale;
        float thickness = (Float) CrossHairConfiguration.THICKNESS.getValue() * scale;

        shapeRenderer.circle(centerX, centerY, 4 * scale);

        shapeRenderer.rect(
            centerX - (distance + size) - offsetSpread.x * scale,
            centerY - thickness / 2,
            size,
            thickness
        );
        shapeRenderer.rect(
            centerX + distance + offsetSpread.x * scale,
            centerY - thickness / 2,
            size,
            thickness
        );

        shapeRenderer.rect(
            centerX - thickness / 2,
            centerY - (distance + size) - offsetSpread.y * scale,
            thickness,
            size
        );
        shapeRenderer.rect(
            centerX - thickness / 2,
            centerY + distance + offsetSpread.y * scale,
            thickness,
            size
        );
    }

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
