package com.hexedrealms.engine;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.NPC;
import com.hexedrealms.components.Trigger;
import com.hexedrealms.utils.NPCPathfinder.Node;
import com.hexedrealms.utils.NPCPathfinder.NodeGraph;
import com.hexedrealms.utils.damage.DamageType;

import java.util.HashMap;
import java.util.Map;

public class NPCComponent implements Disposable {

    private static NPCComponent instance;
    private static Array<Steerable<Vector3>> obstacles = new Array<>();
    public Array<NPC> npces;
    private NodeGraph graph;
    private Player player;
    private Node targetNode;

    public static Map<Trigger, Array<NPC>> triggerArrayMap = new HashMap<>();

    private NPCComponent(NodeGraph graph, Player player){
        this.npces = new Array<>();
        this.graph = graph;
        this.player = player;

        targetNode = findNearestNode(PhysicComponent.getInstance().getVectorPool().obtain().set(player.getCamera().position));
    }

    public static Array<Steerable<Vector3>> getObstacles() {
        return obstacles;
    }

    public Node findNearestNode(Vector3 position){
        position.y -= 2f;
        Node nearestNode = null;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < graph.nodes.size; i++) {
            float distance = position.dst(graph.nodes.get(i).position);
            if (distance < minDistance) {
                minDistance = distance;
                nearestNode = graph.nodes.get(i);
            }
        }

        return nearestNode;
    }

    public Node getTargetNode(){
        return targetNode;
    }

    public NodeGraph getGraph() {
        return graph;
    }

    public void update(float delta){
        Node node = findNearestNode(PhysicComponent.getInstance().getVectorPool().obtain().set(player.getCamera().position));
        if(targetNode != node)
            targetNode = node;

        for(int i = 0; i < triggerArrayMap.size(); i++){
            Trigger trigger = (Trigger) triggerArrayMap.keySet().toArray()[i];
            if(!trigger.isActive){
                AudioComponent.getInstance().setMusicState(AudioComponent.MusicState.COMBAT);
                for(NPC npc : triggerArrayMap.get(trigger)){
                    npc.init();
                    npc.playPortal();
                }
                npces.addAll(triggerArrayMap.get(trigger));

                triggerArrayMap.remove(trigger);
            }
        }

        for(NPC npc : npces)
            npc.update(delta);
    }

    public void removeNPC(NPC npc){
        npces.removeValue(npc, true);
        if(npces.isEmpty())
            AudioComponent.getInstance().setMusicState(AudioComponent.MusicState.EXPLORATION);
    }

    public static NPCComponent getInstance(NodeGraph graph, Player player) {
        if(instance == null)
            instance = new NPCComponent(graph, player);

        return instance;
    }

    @Override
    public void dispose() {
        for(NPC npc : npces){
            npc.dispose();
        }
        triggerArrayMap.clear();
        npces.clear();
        npces = null;
        targetNode = null;
        instance = null;
    }
}
