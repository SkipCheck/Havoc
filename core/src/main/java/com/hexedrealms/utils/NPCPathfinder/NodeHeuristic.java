package com.hexedrealms.utils.NPCPathfinder;

import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.math.Vector3;

public class NodeHeuristic implements Heuristic<Node> {
    @Override
    public float estimate(Node node, Node endNode) {
        return Vector3.dst(
            node.position.x,
            node.position.y,
            node.position.z,
            endNode.position.x,
            endNode.position.y,
            endNode.position.z
        );
    }
}
