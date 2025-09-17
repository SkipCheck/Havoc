package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.*;
import com.crashinvaders.vfx.effects.util.MixEffect;
import com.hexedrealms.configurations.GraphicConfiguration;
import com.hexedrealms.configurations.PostProcessorConfiguration;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

public class PostProcessorComponent implements Disposable {
    private final static float MAX_BLUR = 0.5f;
    private static PostProcessorComponent instance;
    private VfxManager postprocessor;
    private LevelsEffect levelsEffect;
    private RadialBlurEffect radialBlurEffect;
    private MotionBlurEffect blurEffect;
    private BloomEffect bloomEffect;
    private ColorAttribute colorAttribute;
    private PBRColorAttribute fogAttribute;
    private Color color = new Color();
    private Color tempColor = new Color();
    private float time;
    private float currentBlurStrength;


    private PostProcessorComponent() {
        initialize();
    }

    public static PostProcessorComponent getInstance() {
        if (instance == null) {
            instance = new PostProcessorComponent();
        }
        return instance;
    }

    public void setColorAmbient(Color color){
        tempColor = color;
        colorAttribute.color.set(color).mul((Float)PostProcessorConfiguration.BRIGHTNESS.getValue());
        fogAttribute.color.set(color).mul((Float)PostProcessorConfiguration.BRIGHTNESS.getValue());
    }

    private void initialize() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        colorAttribute = new ColorAttribute(ColorAttribute.AmbientLight, color);
        fogAttribute = new PBRColorAttribute(PBRColorAttribute.Fog, Color.WHITE);
        postprocessor = new VfxManager(Pixmap.Format.RGBA8888, width, height);
        postprocessor.setBlendingEnabled(true);
        configureEffects();
    }

    private void configureEffects() {
        levelsEffect = new LevelsEffect();
        levelsEffect.setContrast(1.1f);
        levelsEffect.setSaturation(1.3f);
        levelsEffect.setGamma(1f);
        levelsEffect.setBrightness(0.03f);
        postprocessor.addEffect(levelsEffect);

        OldTvEffect oldTvEffect = new OldTvEffect();
        postprocessor.addEffect(oldTvEffect);

        radialBlurEffect = new RadialBlurEffect(6);
        radialBlurEffect.setDisabled(true);
        postprocessor.addEffect(radialBlurEffect);

        updateBlurStrength();
        blurEffect.setDisabled(!(Boolean)PostProcessorConfiguration.MOTION_BLUR.getValue());

        bloomEffect = new BloomEffect();
        bloomEffect.setBloomIntensity(1.6f);
        bloomEffect.setThreshold(0.95f);
        bloomEffect.setBaseSaturation(1.08f);
        bloomEffect.setDisabled(!(Boolean) PostProcessorConfiguration.BLOOM.getValue());
        postprocessor.addEffect(bloomEffect);
    }

    public void enableBloom(boolean enable) {
        bloomEffect.setDisabled(!enable);
    }

    public void enableMotionBlur(boolean enable) {
        updateBlurStrength();
        blurEffect.setDisabled(!enable);
    }

    private void updateBlurStrength() {

        this.currentBlurStrength = MathUtils.clamp(0.3f, 0f, 1f);
        postprocessor.removeEffect(blurEffect);

        // Создаем новый эффект с текущей силой размытия
        blurEffect = new MotionBlurEffect(
            Pixmap.Format.RGB565,
            MixEffect.Method.MIX,
            currentBlurStrength
        );
        blurEffect.setDisabled((Boolean) PostProcessorConfiguration.MOTION_BLUR.getValue());
        postprocessor.addEffect(blurEffect);
    }

    public void enableRadialBlur(boolean enable) {
        if (radialBlurEffect.isDisabled()) {
            radialBlurEffect.setDisabled(!enable);
        }
    }

    public void resize(int width, int height) {
        postprocessor.resize(width, height);
    }

    public void beginCapture(float delta) {
        postprocessor.beginInputCapture();

        if (!radialBlurEffect.isDisabled()) {
            time += delta;

            if (time > MAX_BLUR || PhysicComponent.getInstance().getLinearVelocity().len() < 20) {
                radialBlurEffect.setDisabled(true);
                time = 0f;
            }
        }
    }

    public void applyBrightness(float value) {
        value = MathUtils.clamp(value, -1f, 1f);
        value = (float)Math.sin(value * MathUtils.PI / 2);

        colorAttribute.color.set(tempColor).mul(1+value);
        fogAttribute.color.set(new Color(0.5f, 0.5f, 0.5f,1));

    }

    public PBRColorAttribute getFogAttribute() {
        return fogAttribute;
    }

    public ColorAttribute getColorAttribute() {
        return colorAttribute;
    }

    public void endCapture() {
        postprocessor.endInputCapture();
    }

    public void applyEffects() {
        postprocessor.applyEffects();
    }

    public void renderToScreen() {
        postprocessor.renderToScreen();
    }

    public void cleanUpBuffers() {
        postprocessor.cleanUpBuffers();
    }

    @Override
    public void dispose() {
        if (postprocessor != null) {
            postprocessor.dispose();
            postprocessor = null;
        }

        if (levelsEffect != null) {
            levelsEffect.dispose();
            levelsEffect = null;
        }

        if (radialBlurEffect != null) {
            radialBlurEffect.dispose();
            radialBlurEffect = null;
        }

        if (blurEffect != null) {
            blurEffect.dispose();
            blurEffect = null;
        }

        if (bloomEffect != null) {
            bloomEffect.dispose();
            bloomEffect = null;
        }

        color = null;
        tempColor = null;
        colorAttribute = null;
        fogAttribute = null;

        instance = null;
    }

    public LevelsEffect getLevelsEffect() {
        return levelsEffect;
    }

    public VfxManager getPostprocessor() {
        return postprocessor;
    }
}
