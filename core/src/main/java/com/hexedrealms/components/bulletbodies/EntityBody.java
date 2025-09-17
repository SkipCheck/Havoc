package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

public class EntityBody extends btRigidBody {
    public EntityBody(btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
}
