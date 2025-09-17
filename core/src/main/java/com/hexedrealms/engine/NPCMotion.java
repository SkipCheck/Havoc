package com.hexedrealms.engine;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class NPCMotion extends btMotionState {
    Matrix4 transform;

    public NPCMotion(Matrix4 transform){
        this.transform = transform;
    }

    public void getWorldTransform(Matrix4 worldTrans) {
        worldTrans.set(this.transform);
    }

    public void setWorldTransform(Matrix4 worldTrans) {
        this.transform.set(worldTrans);
    }
}
