package com.hexedrealms.components;

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

public class CustomEffect extends Pool<ParticleEffect> implements Disposable {
    private ParticleEffect effect;
    public CustomEffect(ParticleEffect effect){
        this.effect = effect;
    }

    @Override
    public void free(ParticleEffect pfx) {
        super.free(pfx);
    }

    @Override
    protected ParticleEffect newObject() {
        return effect.copy();
    }

    @Override
    public void dispose() {
        // Dispose the base particle effect
        if (effect != null) {
            effect.dispose();
            effect = null;
        }

        clear();
    }
}
