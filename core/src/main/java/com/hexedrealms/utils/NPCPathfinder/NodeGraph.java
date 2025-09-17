package com.hexedrealms.utils.NPCPathfinder;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class NodeGraph implements IndexedGraph<Node> {
    public final Array<Node> nodes = new Array();
    private final NodeHeuristic nodeHeuristic = new NodeHeuristic();
    private final Array<Edge> edges = new Array();
    private final ObjectMap<Node, Array<Connection<Node>>> edgesMap = new ObjectMap();
    private int lastNodeIndex = 0;
    public void addNode(Node tile) {
        tile.index = lastNodeIndex;
        lastNodeIndex++;
        nodes.add(tile);
    }
    public void connectNodes(Node fromTile, Node toTile) {
        Edge edge = new Edge(fromTile, toTile);
        if (!edgesMap.containsKey(fromTile)) edgesMap.put(fromTile, new Array());
        edgesMap.get(fromTile).add(edge);
        edges.add(edge);
    }
    public void endInit(){
        // Проходим по каждому узлу и ищем его соседей
        for (int i = 0; i < nodes.size; i++) {
            Node node = nodes.get(i);
            for (int j = 0; j < nodes.size; j++) {
                Node otherNode = nodes.get(j);
                if (node == otherNode) continue;
                float distance = node.position.dst(otherNode.position);
                if (distance < 12f) connectNodes(node, otherNode);
            }
        }
    }
    public GraphPath<Node> findPath(Node startNode, Node goalNode) {
        GraphPath<Node> nodePath = new DefaultGraphPath();
        new IndexedAStarPathFinder<>(this).searchNodePath(startNode, goalNode, nodeHeuristic, nodePath);
        return nodePath;
    }
    @Override
    public int getIndex(Node node) {
        return node.index;
    }
    @Override
    public int getNodeCount() {
        return lastNodeIndex;
    }
    @Override
    public Array<Connection<Node>> getConnections(Node fromNode) {
        if (edgesMap.containsKey(fromNode)) return edgesMap.get(fromNode);
        return new Array<>(0);
    }
}
