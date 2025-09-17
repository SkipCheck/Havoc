package com.hexedrealms.utils.occlusion;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

public class BSPNode {
    public BoundingBox boundingBox;
    public BSPNode front;
    public BSPNode back;
    public Array<Renderable> objects;

    public BSPNode(BoundingBox box) {
        this.boundingBox = new BoundingBox(box);
        this.objects = new Array<>();
    }

    public boolean isLeaf() {
        return front == null && back == null;
    }
}
