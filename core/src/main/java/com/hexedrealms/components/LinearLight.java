package com.hexedrealms.components;

import com.hexedrealms.engine.LightingComponent;
import com.hexedrealms.screens.Level;

public class LinearLight extends DynamicLight{
    private float cycleTime; // Время для полного цикла (период синусоиды)
    private float timeAccumulator;
    public LinearLight(float baseIntensity, float flickerSpeed, float flickerAmount, float intensityChangeSpeed, float cycleTime) {
        super(baseIntensity, flickerSpeed, flickerAmount, intensityChangeSpeed);
        this.cycleTime = cycleTime; // Задаем период синусоиды
        this.timeAccumulator = 0f;
    }

    public void update(float delta){
        // Обновляем накопленное время для синусоиды
        timeAccumulator += delta * flickerSpeed;

        // Используем синусоиду для изменения интенсивности
        float sineValue = (float)Math.sin(2 * Math.PI * (timeAccumulator / cycleTime));
        intensity = Math.max(sineValue * flickerAmount, baseIntensity);
        int index = LightingComponent.getInstance().getPointLightArray().indexOf(this, true);
        Level.getInstance().updateEnvironment(index);
    }
}
