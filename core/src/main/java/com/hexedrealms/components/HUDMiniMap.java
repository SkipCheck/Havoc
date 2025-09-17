package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.engine.Player;

public class HUDMiniMap {

    private ShapeRenderer shapeRenderer;
    private float centerX, centerY;

    private TextureRegion minimap, clip;
    private Vector2 position;
    private Vector2 size;
    private float x, y;
    private float widthRender, heightRender;

    public HUDMiniMap(String map, float x, float y, float xClip, float yClip, float widthRender, float heightRender){
        this.minimap = new TextureRegion(new Texture(map), (int) (xClip - widthRender / 2), (int) (yClip - heightRender/2), (int) widthRender, (int) heightRender);
        this.x = x;
        this.y = y;
        this.widthRender = widthRender;
        this.heightRender = heightRender;
        this.centerX = x + widthRender/2;
        this.centerY = y + heightRender/2;
        shapeRenderer = new ShapeRenderer();
    }

    public Texture getTexture(){
        return minimap.getTexture();
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getSize() {
        return size;
    }

    public void scrollMiniMap(float x, float y){
        minimap.scroll(x, y);
    }

    public void setBounds(float x, float y, float width, float height){
        position = new Vector2(x, y);
        size = new Vector2(width, height);
    }

    public void render(SpriteBatch batch){
        batch.draw(minimap, x, y, widthRender, heightRender);
    }

    public void renderPoint(){
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.circle(centerX, centerY, 5);
        shapeRenderer.end();
    }
}
