package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.ModelInstanceParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.CustomEffect;

import java.io.Serializable;
import java.util.HashMap;

public class ParticlesComponent implements Disposable{

    private static ParticlesComponent instance;
    private ParticleSystem particleSystem;
    private HashMap<String, CustomEffect> map;
    private Array<ParticleEffect> dynamics;
    private Array<ParticleEffect> statics;
    public AssetManager manager;
    private static final String PARTICLES_DIR = "assets/textures/particles";

    public ParticlesComponent(Camera camera) {
        dynamics = new Array<>();
        statics = new Array<>();
        map = new HashMap<>();

        // Initialize particle system
        initializeParticleSystem(camera);

        // Auto-load all particle effects
        autoLoadParticleEffects();
    }

    public void clear(){

        for(ParticleEffect effect : dynamics){
            particleSystem.remove(effect);
        }
        for(ParticleEffect effect : statics){
            particleSystem.remove(effect);
        }

        dynamics.clear();
        statics.clear();
    }

    private void initializeParticleSystem(Camera camera) {
        particleSystem = new ParticleSystem();
        BlendingAttribute blendingAttribute = new BlendingAttribute(true, 1f);

        ParticleShader.Config config = new ParticleShader.Config(ParticleShader.AlignMode.Screen);

        BillboardParticleBatch billboardParticleBatch = new BillboardParticleBatch(ParticleShader.AlignMode.ViewPoint, true, 2000, blendingAttribute, new DepthTestAttribute(515, true));
        billboardParticleBatch.setCamera(camera);


        PointSpriteParticleBatch pointSpriteParticleBatch = new PointSpriteParticleBatch(1000, config, blendingAttribute, new DepthTestAttribute(GL30.GL_LEQUAL, true));
        pointSpriteParticleBatch.setCamera(camera);

        particleSystem.add(pointSpriteParticleBatch);
        particleSystem.add(billboardParticleBatch);

        manager = new AssetManager();
    }

    private void autoLoadParticleEffects() {
        // Get all files in the particles directory
        FileHandle dirHandle = Gdx.files.internal(PARTICLES_DIR);


        if (!dirHandle.exists()) {
            Gdx.app.error("ParticlesComponent", "Particles directory not found: " + PARTICLES_DIR);
            return;
        }

        // Set up load parameters
        ParticleEffectLoader.ParticleEffectLoadParameter loadParam =
            new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());


        // Find all .pfx files
        Array<FileHandle> pfxFiles = new Array<>();
        findParticleEffects(dirHandle, pfxFiles);

        // Load all found particle effects
        for (FileHandle file : pfxFiles) {
            String path = file.path();
            manager.load(path, ParticleEffect.class, loadParam);
        }

        // Finish loading
        manager.finishLoading();

        // Create CustomEffect wrappers for all loaded effects
        for (FileHandle file : pfxFiles) {
            String path = file.path();

            map.put(file.name(), new CustomEffect(manager.get(path)));
            Gdx.app.log("ParticlesComponent", "Loaded particle effect: " + path);
        }
    }

    private void findParticleEffects(FileHandle directory, Array<FileHandle> results) {
        // Recursively search for .pfx files
        for (FileHandle file : directory.list()) {
            if (file.isDirectory()) {
                findParticleEffects(file, results);
            } else if (file.extension().equalsIgnoreCase("pfx")) {

                results.add(file);
            }
        }
    }

    public ParticleEffect getEffectByName(String name){
        ParticleEffect effect = map.get(name).obtain();
        return effect;
    }

    public void freeEffectByName(String name, ParticleEffect effect){
        map.get(name).free(effect);
    }

    public ParticleEffect findEffect(String name){
        for (String key : map.keySet()){
            if (key.toLowerCase().contains(name)) return map.get(key).obtain();
        }
        return null;
    }

    public void addEffect(ParticleEffect effect){
        statics.add(effect);
        particleSystem.add(effect);
    }

    public void addDynamicEffect(ParticleEffect effect){
        dynamics.add(effect);
        addEffect(effect);
    }

    public void removeParticle(ParticleEffect effect){
        particleSystem.remove(effect);
        effect.reset();
    }

    public static ParticlesComponent getInstance(Camera camera) {
        if(instance == null)
            instance = new ParticlesComponent(camera);

        return instance;
    }

    public void render(ModelBatch batch, Environment environment, float delta, boolean isPaused){

        if(!isPaused) {
            int size = dynamics.size;
            for(int i = size; --i >= 0;) {
                ParticleEffect effect = dynamics.get(i);

                if (effect.getControllers().get(0).emitter.percent > 1f) {
                    dynamics.removeIndex(i);
                    removeParticle(effect);
                }
            }

            particleSystem.update(delta);
        }

        particleSystem.begin();
        particleSystem.draw();
        particleSystem.end();

        batch.render(particleSystem, environment);
    }

    @Override
    public void dispose() {
        // Dispose all particle effects
        for (CustomEffect customEffect : map.values()) {
            customEffect.dispose();
        }
        map.clear();

        // Clear and dispose active particle effects
        for (ParticleEffect effect : dynamics) {
            if (effect != null) {
                particleSystem.remove(effect);
                effect.dispose();
            }
        }
        dynamics.clear();

        for (ParticleEffect effect : statics) {
            if (effect != null) {
                particleSystem.remove(effect);
                effect.dispose();
            }
        }
        statics.clear();

        // Dispose particle system and batches
        if (particleSystem != null) {
            particleSystem = null;
        }

        // Dispose asset manager
        if (manager != null) {
            manager.dispose();
            manager = null;
        }

        // Reset singleton instance
        instance = null;
    }
}
