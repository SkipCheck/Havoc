package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import net.dermetfan.gdx.physics.box2d.RotationController;

public class HUDFace extends HUDComponent {
    private static final int TOTAL_INDICES = 4;
    private static final float ANIMATION_SPEED = 1f;
    private static final float HEALTH_SEGMENT = 0.25f; // 25% segments

    private final Array<TextureAtlas.AtlasRegion>[] idleAnimations;
    private final Array<TextureAtlas.AtlasRegion>[] painAnimations;
    private final DecalAnimation animation;

    private int activeIndex;
    private boolean isPainState;
    private int lastHealthSegment; // Tracks which health segment we're in

    public HUDFace(TextureAtlas atlas, int x, int y, int width, int height) {
        super(null, x, y, width, height);
        idleAnimations = createAnimationArrays(atlas, "idle");
        painAnimations = createAnimationArrays(atlas, "pain");

        region = idleAnimations[activeIndex].get(0);
        animation = new DecalAnimation(idleAnimations[activeIndex].size);
        lastHealthSegment = 0; // Start at full health
    }

    // HUDFace.java (наследуется от HUDComponent)
    @Override
    public void resize(float parentWidth, float parentHeight) {
        super.resize(parentWidth, parentHeight);
        // Дополнительная логика при необходимости
    }

    private Array<TextureAtlas.AtlasRegion>[] createAnimationArrays(TextureAtlas atlas, String animationType) {
        Array<TextureAtlas.AtlasRegion>[] animations = new Array[TOTAL_INDICES];
        for (int i = 0; i < TOTAL_INDICES; i++) {
            animations[i] = atlas.findRegions((100 - 25 * i) + "_" + animationType);
        }
        return animations;
    }

    public void increaseLive(int currentHealth, int maxHealth){
        if (maxHealth <= 0) return; // Avoid division by zero

        float healthPercentage = (float) currentHealth / maxHealth;
        int newSegment = (int) ((1 - healthPercentage) / HEALTH_SEGMENT);

        // Clamp the segment between 0 and TOTAL_INDICES-1
        newSegment = Math.min(Math.max(newSegment, 0), TOTAL_INDICES - 1);

        if (newSegment != lastHealthSegment) {
            lastHealthSegment = newSegment;
            activeIndex = newSegment;
        }
    }

    public void activatePain(int currentHealth, int maxHealth) {
        if (maxHealth <= 0) return; // Avoid division by zero

        float healthPercentage = (float) currentHealth / maxHealth;
        int newSegment = (int) ((1 - healthPercentage) / HEALTH_SEGMENT);

        // Clamp the segment between 0 and TOTAL_INDICES-1
        newSegment = Math.min(Math.max(newSegment, 0), TOTAL_INDICES - 1);

        // Only trigger pain animation if segment changed
        if (newSegment != lastHealthSegment) {
            lastHealthSegment = newSegment;
            triggerPainAnimation(newSegment);
        }
    }

    public void triggerPainAnimation(int index) {
        isPainState = true;

        // Ensure index is within valid range
        activeIndex = Math.min(Math.max(index, 0), TOTAL_INDICES - 1);
        animation.setSize(painAnimations[activeIndex].size);
        animation.startAnimation();
        animation.clearTimer();
    }

    public void render(SpriteBatch batch, boolean isPaused) {
        super.render(batch, isPaused);
        if(isPaused) return;

        animation.UpdateDecal(Gdx.graphics.getDeltaTime() * (isPainState ? ANIMATION_SPEED : ANIMATION_SPEED-0.5f));

        Array<TextureAtlas.AtlasRegion> currentAnimations = isPainState
            ? painAnimations[activeIndex]
            : idleAnimations[activeIndex];

        region = currentAnimations.get(animation.getFrame());

        if (isPainState && animation.isFinalled()) {
            animation.setSize(idleAnimations[activeIndex].size);
            isPainState = false;
        }
    }
}
