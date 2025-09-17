package com.hexedrealms.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.utils.Array;
import com.hexedrealms.components.DecalAnimation;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.PhysicComponent;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.utils.damage.Enemy;
import de.pottgames.tuningfork.EaxReverb;
import de.pottgames.tuningfork.SoundEffect;
import de.pottgames.tuningfork.StreamedSoundSource;


public class ShootWeapon extends Weapon{
    protected Color shootColor;
    protected DecalAnimation reloadAnimation;
    protected Array<TextureAtlas.AtlasRegion> reload;
    protected StreamedSoundSource soundReload;
    protected SoundEffect reverbEffect;
    protected String nameParticle;
    protected Vector2 shaking;
    protected ClosestRayResultCallback rayResultCallback;
    protected boolean isDynamicSpread, isReload, isRiffle;
    protected int spreadCount, shootCount, maxBullets, currentBullets, currentShops, shopBullets, totalBullets;
    protected float itensivityReload, scaleViewReload, actualSpreadCount;

    private float currentSpreadMultiplier = 0.2f; // Текущий множитель разброса
    private final float spreadIncreaseRate = 0.2f; // Скорость увеличения разброса
    private final float maxSpreadMultiplier = 2f;

    public ShootWeapon(String filename, String soundOfShoot, String soundOfSelect, String soundOfReload,
                       String textureOfHole, String nameParticle, Color shootColor,
                       int shootCount, int scaleRay, int spreadCount, int maxBullets, int shopBullets, float speedChangeFrame,
                       float itensivityReload, float scaleViewed, float scaleViewReload, Vector2 shaking, boolean isDynamicSpread, DamageType damageType, float basicDamage, int currentBullets, int currentShops, boolean isRiffle) {

        super(filename, soundOfShoot, soundOfSelect, null, textureOfHole, speedChangeFrame, scaleViewed, scaleRay, damageType, basicDamage);

        this.isRiffle = isRiffle;
        this.shootColor = shootColor;
        this.shootCount = shootCount;
        this.spreadCount = spreadCount;
        this.shopBullets = shopBullets;
        this.maxBullets = maxBullets;
        this.nameParticle = nameParticle;
        this.currentBullets = Math.min(currentBullets, maxBullets);
        this.currentShops = Math.min(currentShops, shopBullets);
        this.scaleViewReload = scaleViewReload;
        this.itensivityReload = itensivityReload;
        this.soundReload = new StreamedSoundSource(Gdx.files.internal(soundOfReload));
        this.soundReload.setRelative(true);
        this.reverbEffect = new SoundEffect(EaxReverb.castleSmallRoom());
        this.shaking = shaking;
        this.isDynamicSpread = isDynamicSpread;
        this.rayResultCallback = PhysicComponent.getInstance().getRaycastCallbackPool().obtain();
        this.totalBullets = currentBullets * currentShops;

        reload = atlas.findRegions("reload");
        reloadAnimation = new DecalAnimation(reload.size);
    }

    public void setCurrentBullets(int count){
        currentBullets = count;
    }

    public void setTotalBullets(int count){
        totalBullets = count;
    }

    public void addTotalBullets(int count){
        int currentCount = totalBullets + currentBullets + count - getMaxBullets();
        totalBullets += count - currentCount;
    }

    public int getMaxBullets(){
        return maxBullets * (shopBullets+1);
    }

    public boolean isRiffle() {
        return isRiffle;
    }

    public int getTotalBullets() {
        return totalBullets + currentBullets;
    }

    public void Shoot(Camera camera){
        if(currentBullets > 0) {
            if (!isShooting) {
                sound.play(AudioConfiguration.SOUND.getValue(), reverbEffect);

                actualSpreadCount = isDynamicSpread
                    ? (spreadCount * currentSpreadMultiplier)
                    : spreadCount;

                for (int i = 0; i < shootCount; i++) {
                    System.out.println(nameParticle);
                    Enemy enemy = PhysicComponent.getInstance().consumeRaycast(mScaleRay, (int) actualSpreadCount, camera, mHitTexture, nameParticle);
                    checkDamage(enemy);

                }

                if (isDynamicSpread)
                    currentSpreadMultiplier = MathUtils.lerp(currentSpreadMultiplier, maxSpreadMultiplier, spreadIncreaseRate);

            }

            isShooting = true;
            mAnimation.startAnimation();
            currentBullets--;

            if(currentBullets == 0) {
                reloadAnimation.startAnimation();
                Reload();
            }
        }
    }

    public void stopShooting() {
        currentSpreadMultiplier = 0f;
        actualSpreadCount = 0;
    }

    public Vector2 getDurationShaker() {
        return shaking;
    }

    public void Reload(){
        if(currentBullets == 0 && totalBullets > 0 || isReload) {
            soundReload.setVolume(AudioConfiguration.SOUND.getValue());
            soundReload.play();
        }
    }

    public void stopAnimation(){
        if(currentBullets > 0) {
            super.stopAnimation();
        }

        if(soundReload.isPlaying())
            soundReload.pause();
    }

    public float getActualSpreadCount() {
        return actualSpreadCount;
    }

    public float getScaleViewed(){
        return prevScale;
    }

    public int getCurrentBullets(){
        return currentBullets;
    }

    public int getShopBullets(){
        return totalBullets;
    }

    public void Render(float delta){
        super.Render(delta);
        activateReload(delta);
    }

    public void activateReload(float delta){
        if(totalBullets <=  0) return;
        if((currentBullets == 0 && mAnimation.isFinalled()) || isReload){
            stopShooting();
            prevScale = scaleViewed * scaleViewReload;
            reloadAnimation.UpdateDecal(delta * speedChangeFrame * itensivityReload);
            mCurrentFrame = reloadAnimation.getFrame();
            mCurrentTexture = reload.get(mCurrentFrame);

            if(reloadAnimation.isFinalled()) {
                int razn = maxBullets - currentBullets;
                totalBullets -= razn;
                currentBullets += razn;
                if(totalBullets < 0){
                    currentBullets += totalBullets;
                    totalBullets = 0;
                }
                mCurrentTexture = shoot.get(0);
                prevScale = scaleViewed;
                isReload = false;
            }
        }
    }

    @Override
    public void dispose(){
        for(TextureAtlas.AtlasRegion region : reload)
            region.getTexture().dispose();

        mHitTexture.getTexture().dispose();
        soundReload.dispose();

        reload.clear();

        shootColor = null;
        mHitTexture = null;
        reloadAnimation = null;
        reload = null;
        soundReload = null;
    }

    public Color getShootColor(){
        return shootColor;
    }

    @Override
    public String toString() {
        return "ShootWeapon{" +
            "shootColor=" + shootColor +
            ", reloadAnimation=" + reloadAnimation +
            ", reload=" + reload +
            ", soundReload=" + soundReload +
            ", reverbEffect=" + reverbEffect +
            ", nameParticle='" + nameParticle + '\'' +
            ", shaking=" + shaking +
            ", rayResultCallback=" + rayResultCallback +
            ", isDynamicSpread=" + isDynamicSpread +
            ", spreadCount=" + spreadCount +
            ", shootCount=" + shootCount +
            ", maxBullets=" + maxBullets +
            ", currentBullets=" + currentBullets +
            ", shopBullets=" + shopBullets +
            ", itensivityReload=" + itensivityReload +
            ", scaleViewReload=" + scaleViewReload +
            ", actualSpreadCount=" + actualSpreadCount +
            ", currentSpreadMultiplier=" + currentSpreadMultiplier +
            ", spreadIncreaseRate=" + spreadIncreaseRate +
            ", maxSpreadMultiplier=" + maxSpreadMultiplier +
            '}';
    }

    public void consumeReload() {
        if(currentBullets == maxBullets) return;
        isReload = true;
        reloadAnimation.startAnimation();
        Reload();
    }
}
