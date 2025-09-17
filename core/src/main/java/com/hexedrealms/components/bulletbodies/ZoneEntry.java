package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;

public class ZoneEntry {
    private BoundingBox boundingBox;
    private ZoneBody object;
    public boolean isEntered;
    public Array<btCollisionObject> enteredObjects;

    public ZoneEntry(BoundingBox box, ZoneBody object){
        this.boundingBox = box;
        this.object = object;
        this.enteredObjects = new Array<>();
    }

    public void putObject(btCollisionObject object){
        if(!enteredObjects.contains(object, true))
            enteredObjects.add(object);
    }

    public ZoneBody getBody(){
        return object;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public boolean isEntered(BoundingBox box){
        return boundingBox.contains(box.getCenter(new Vector3())) || boundingBox.contains(box) ;
    }

    public boolean isEntered(Vector3 position){
        isEntered = boundingBox.contains(position);
        return isEntered;
    }
}
