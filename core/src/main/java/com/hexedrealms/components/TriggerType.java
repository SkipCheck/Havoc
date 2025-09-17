package com.hexedrealms.components;

import com.badlogic.gdx.math.Vector3;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.ResourcesLoader;
import com.hexedrealms.screens.Level;
import com.hexedrealms.weapons.Weapon;
import de.pottgames.tuningfork.SoundBuffer;

public abstract class TriggerType implements Runnable {
    protected String target;
    protected SoundBuffer soundBuffer;
    protected Vector3 position;
    protected ItemEntity entity;

    public abstract void execute(String parameter, SoundBuffer soundBuffer);
    public abstract void setPosition(Vector3 position);
    public abstract void putItemEntity(ItemEntity itemEntity);
    public abstract TriggerType copy(); // Теперь можно делать глубокое копирование

    // Общие методы (если нужны)
    public void run() {
        // Базовая реализация (можно переопределить)
    }
}

