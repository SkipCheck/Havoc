package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

public class OcclusionBody extends btRigidBody {
    public int depth;
    public BoundingBox box;
    public OcclusionBody(btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
}
