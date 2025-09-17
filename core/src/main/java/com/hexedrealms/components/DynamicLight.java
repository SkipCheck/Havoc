package com.hexedrealms.components;

import com.hexedrealms.engine.LightingComponent;
import com.hexedrealms.screens.Level;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

import java.util.concurrent.ThreadLocalRandom;

public class DynamicLight extends PointLightEx {
    protected float baseIntensity; // Базовая интенсивность
    protected float flickerSpeed; // Скорость мерцания
    protected float flickerAmount; // Сила мерцания
    protected float time; // Время для анимации
    protected float flickerInterval; // Интервал между мерцаниями
    protected float nextFlickerTime; // Время следующего мерцания
    protected float targetIntensity; // Целевая интенсивность
    protected float intensityChangeSpeed; // Скорость изменения интенсивности

    public DynamicLight(float baseIntensity, float flickerSpeed, float flickerAmount, float intensityChangeSpeed) {
        this.baseIntensity = baseIntensity;
        this.flickerSpeed = flickerSpeed;
        this.flickerAmount = flickerAmount;
        this.time = 0f;
        this.flickerInterval = 0.1f; // Интервал между мерцаниями
        this.nextFlickerTime = 0f; // Изначально следующее мерцание в прошлом
        this.targetIntensity = baseIntensity; // Начальное значение целевой интенсивности
        this.intensityChangeSpeed = intensityChangeSpeed; // Скорость изменения интенсивности
    }

    public void update(float delta) {
        time += delta;

        // Проверяем, пора ли обновить целевую интенсивность
        if (time >= nextFlickerTime) {
            // Генерируем случайное значение для новой целевой интенсивности
            float randomFlicker = ThreadLocalRandom.current().nextFloat(-flickerAmount, flickerAmount);
            targetIntensity = Math.max(baseIntensity + randomFlicker, baseIntensity); // Минимальная интенсивность 5f

            // Устанавливаем время следующего мерцания
            nextFlickerTime = time + flickerInterval; // Увеличиваем время на интервал
        }

        // Плавно изменяем интенсивность к целевой
        this.intensity += (targetIntensity - this.intensity) * intensityChangeSpeed * delta;
    }
}
