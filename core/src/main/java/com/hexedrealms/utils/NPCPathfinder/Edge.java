package com.hexedrealms.utils.NPCPathfinder;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector3;

public class Edge implements Connection<Node> {
    private float cost;
    private Node fromNode;
    private Node toNode;

    public Edge(Node fromNode, Node toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        cost = Vector3.dst(
            fromNode.position.x,
            fromNode.position.y,
            fromNode.position.z,
            toNode.position.x,
            toNode.position.y,
            toNode.position.z
        );
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public Node getFromNode() {
        return fromNode;
    }

    @Override
    public Node getToNode() {
        return toNode;
    }
}

