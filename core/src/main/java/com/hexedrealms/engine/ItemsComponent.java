package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.ItemEntity;

public class ItemsComponent implements Disposable {

    private static ItemsComponent instance;
    private TextureAtlas atlas;
    private Array<ItemEntity> items;

    private ItemsComponent(){
        atlas = new TextureAtlas(Gdx.files.internal("textures/atlases/items/items.atlas"));
        items = new Array<>();
    }

    public Array<ItemEntity> getItems() {
        return items;
    }

    public void putItem(ItemEntity entity){
        items.add(entity);
    }

    public void removeItem(ItemEntity entity){
        items.removeValue(entity, true);
    }

    public TextureAtlas getAtlas(){
        return atlas;
    }

    public TextureRegion findRegion(String name){
        return atlas.findRegion(name);
    }

    public static ItemsComponent getInstance() {
        if(instance == null)
            instance = new ItemsComponent();
        return instance;
    }

    public void clearItems(){
        for (ItemEntity item : items) {
            if (item != null) {
                item.dispose();
            }
        }
        items.clear();
    }

    @Override
    public void dispose() {
        // Dispose all items
        clearItems();

        // Dispose texture atlas
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }

        // Reset singleton instance
        instance = null;
    }
}
