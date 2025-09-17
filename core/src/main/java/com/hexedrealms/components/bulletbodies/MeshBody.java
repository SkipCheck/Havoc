package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

public class MeshBody extends btRigidBody {
    private BoundingBox box;
    public MeshBody(btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
    public boolean isEntered;

    public void putBox(BoundingBox box){
        this.box = box;
    }

    public BoundingBox getBox() {
        return box;
    }
}
