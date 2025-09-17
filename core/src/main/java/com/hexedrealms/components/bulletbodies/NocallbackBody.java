package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

public class NocallbackBody extends btRigidBody{
    public NocallbackBody(btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
}
