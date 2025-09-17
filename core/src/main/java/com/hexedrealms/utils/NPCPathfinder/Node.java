package com.hexedrealms.utils.NPCPathfinder;

import com.badlogic.gdx.math.Vector3;
import com.hexedrealms.engine.PhysicComponent;

public class Node {
    public Vector3 position;
    public int index;
    public String name;

    public Node(float[] position) {
        this.position = PhysicComponent
            .getInstance()
            .getVectorPool()
            .obtain()
            .set(position[0], position[1], position[2]);
    }

    public Node(Vector3 position) {
        this.position = position;
    }


    public void setIndex (int index){
        this.index = index;
    }
}
