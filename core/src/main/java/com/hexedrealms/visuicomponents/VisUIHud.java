package com.hexedrealms.visuicomponents;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.hexedrealms.configurations.PlayerConfiguration;
import com.kotcrab.vis.ui.widget.VisLabel;

public class VisUIHud extends Actor {
    private static final int SIZE = 15;
    public VisLabel armor;
    public VisLabel health;
    public VisLabel bullets;
    public VisLabel message;

    private float pulseTime = 0f; // Таймер для анимации
    private float pulseSpeed = 4f; // Скорость пульсации
    private float minScale = 1f; // Минимальный масштаб
    private float maxScale = 1.4f; // Максимальный масштаб

    private float timer;

    public VisUIHud(){
        super();
        (armor = new VisLabel()).setFontScale(0.9f);
        (health = new VisLabel()).setFontScale(0.9f);
        (bullets = new VisLabel()).setFontScale(0.9f);
        (message = new VisLabel()).setFontScale(0.9f);
    }

    public void setMessage(String text, Vector2 position){
        message.setText(text);
        message.setPosition(position.x, position.y);
        message.setColor(new Color(1f,1f,1f,1f));
        pulseTime = 0f;
    }

    public void setArmor(String text, Vector2 position){
        armor.setText(text);
        armor.setPosition(position.x, position.y);
        armor.setColor(new Color(1f,1f,1f,0.6f));
    }

    public void setHealth(String text, Vector2 position){
        health.setText(text);
        health.setPosition(position.x, position.y);
        health.setColor(new Color(1f,1f,1f,0.6f));
    }

    public void setBullets(String text, Vector2 position){
        bullets.setText(text);
        bullets.setPosition(position.x, position.y);
        bullets.setColor(new Color(1f,1f,1f,0.8f));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Рисуем метки
        armor.draw(batch, parentAlpha);
        health.draw(batch, parentAlpha);
        bullets.draw(batch, parentAlpha);

        if(message.getText() != null) {
            float delta = Gdx.graphics.getDeltaTime();
            pulseTime += delta * pulseSpeed;
            // Плавное изменение масштаба с помощью синуса
            float scale = minScale + (maxScale - minScale) * (0.5f + 0.5f * (float) Math.sin(pulseTime));
            message.setFontScale(scale);

            float width = message.getPrefWidth() / 2;
            float height = message.getPrefHeight() / 2;
            message.setPosition((float) Gdx.graphics.getWidth() / 2 - width, (float) Gdx.graphics.getHeight() * 0.2f);
            message.draw(batch, parentAlpha);
            timer += delta;

            if(timer > 2f){
                message.setText(null);
                timer = 0;
            }
        }
    }

    @Override
    public void act(float delta) {
        // Обновляем состояние меток
        armor.act(delta);
        health.act(delta);
        bullets.act(delta);
    }
}
