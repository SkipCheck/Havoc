package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class OverlaysComponent implements Disposable {

    private static OverlaysComponent instance;

    public Texture pain;
    public Texture health;
    public Texture armor;
    public Texture bullet;
    public Texture weapon;
    public Texture star;

    private Texture current;
    private SpriteBatch batch;

    private float timer;

    public static OverlaysComponent getInstance(){
        if(instance == null){
            instance = new OverlaysComponent();
        }
        return instance;
    }

    private OverlaysComponent(){
        batch = new SpriteBatch();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height){
        pain = new Texture(createTexture(width, height, new Color(0.7F, 0.1F, 0.0F, 0.4F)));
        health = new Texture(createTexture(width, height, new Color(0.0F, 0.7F, 0.0F, 0.4F)));
        armor = new Texture(createTexture(width, height, new Color(0.89F, 0.83F, 0.26F, 0.4F)));
        bullet = new Texture(createTexture(width, height, new Color(0.11F, 0.48F, 0.91F, 0.4F)));
        weapon = new Texture(createTexture(width, height, new Color(0.71F, 0.48F, 0.51F, 0.4F)));
        star = new Texture(createTexture(width, height, new Color(1F, 0.97F, 0.14F, 0.4F)));

        batch.getProjectionMatrix().setToOrtho2D(0,0,width,height);
    }

    private static Pixmap createTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, width, height);
        return pixmap;
    }

    public void uploadOverlay(Texture texture){
        current = texture;
    }

    public void render(float delta){

        if(timer > 0.15f){
            current = null;
            timer = 0f;
        }

        if(current == null) return;

        timer += delta;
        batch.begin();
        batch.draw(current, 0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }


    @Override
    public void dispose() {
        // Dispose all textures
        if (pain != null) {
            pain.dispose();
            pain = null;
        }

        if (health != null) {
            health.dispose();
            health = null;
        }

        if (armor != null) {
            armor.dispose();
            armor = null;
        }

        if (bullet != null) {
            bullet.dispose();
            bullet = null;
        }

        if (weapon != null) {
            weapon.dispose();
            weapon = null;
        }

        // Dispose current texture reference
        current = null;

        // Dispose SpriteBatch
        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        // Reset singleton instance
        instance = null;
    }
}
