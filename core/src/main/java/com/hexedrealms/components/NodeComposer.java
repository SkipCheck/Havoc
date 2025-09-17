package com.hexedrealms.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.hexedrealms.components.bulletbodies.TriggerBody;
import com.hexedrealms.components.specialmeshes.CloudMesh;
import com.hexedrealms.components.specialmeshes.DoorMesh;
import com.hexedrealms.components.specialmeshes.WaterMesh;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.*;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.NPCPathfinder.NodeGraph;
import net.mgsx.gltf.data.scene.GLTFNode;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.lights.SpotLightEx;

public class NodeComposer {
    private Array.ArrayIterator<Node> iterator;
    private Level level;
    private NodeGraph graph;

    public NodeComposer(Array.ArrayIterator<Node> iterator, Level level) {
        this.iterator = iterator;
        this.level = level;
        this.graph = new NodeGraph();
    }

    public void handlingNodes() {
        while (iterator.hasNext()) {
            Node nodeBone = iterator.next();
            GLTFNode node = findMatchingGLTFNode(nodeBone);

            if (node != null) {
                if (processNode(node, nodeBone)) {
                    iterator.remove();
                }
            }
        }

        graph.endInit();
    }

    private GLTFNode findMatchingGLTFNode(Node nodeBone) {
        for (GLTFNode node : level.getMapModel().data.nodes) {
            if (node.name.equals(nodeBone.id)) {
                return node;
            }
        }
        return null;
    }

    private boolean processNode(GLTFNode node, Node nodeBone) {
        if (isDecorationNode(node)) {
            processDecorationNode(node, nodeBone);
            return true;
        } else if (isLightNode(node)) {
            processLightNode(node);
            return true;
        } else if (isPathfindingNode(node)) {
            processPathfindingNode(node);
            return true;
        } else if (isWater(node)) {
            processingWaterNode(nodeBone);
            return true;
        } else if (isOccluder(node)) {
            processOccluderNode(nodeBone);
            return true;
        } else if (isCloud(node)) {
            processingCloudNode(nodeBone);
            return true;
        } else if (isParticleNode(node)) {
            processParticleNode(node);
            return true;
        } else if (isDoorNode(node)) {
            processingDoorNode(nodeBone, node);
            return false;
        }else if (isItemNode(node)) {
            processingItemNode(node, nodeBone);
            return true;
        }else if (isTriggerNode(node)){
            JsonValue value = node.extras != null ? node.extras.value : null;
            processingTriggerNode(node, nodeBone);
            return !value.has("isVisible");
        } else if (isNocallback(node)) {
            JsonValue value = node.extras != null ? node.extras.value : null;
            boolean isVisible = value == null || value.getBoolean("isVisible");
            processNocallbackNode(nodeBone);
            return !isVisible;
        }
        return false;
    }

    private boolean isParticleNode(GLTFNode node) { return node.name.toLowerCase().contains("particle");}
    private boolean isItemNode(GLTFNode node) { return node.name.toLowerCase().contains("item");}
    private boolean isTriggerNode(GLTFNode node) { return node.name.toLowerCase().contains("trigger");}
    private boolean isDecorationNode(GLTFNode node) { return node.name.toLowerCase().contains("decoration"); }
    private boolean isLightNode(GLTFNode node) {return node.name.toLowerCase().contains("light");}
    private boolean isPathfindingNode(GLTFNode node) {return node.name.toLowerCase().contains("node");}
    private boolean isWater(GLTFNode node) { return node.name.toLowerCase().contains("water");}
    private boolean isCloud(GLTFNode node) { return node.name.toLowerCase().contains("cloud");}
    private boolean isOccluder(GLTFNode node) { return node.name.toLowerCase().contains("occluder");}
    private boolean isNocallback(GLTFNode node) { return node.name.toLowerCase().contains("nocallback");}
    private boolean isDoorNode(GLTFNode node) { return node.name.toLowerCase().contains("door");}
    private boolean isNPC(GLTFNode node){ return node.name.toLowerCase().contains("npc");}

    private void processDecorationNode(GLTFNode node, Node nodeBone) {
        DecorationData data = extractDecorationData(node);

        boolean isStatic = node.extras.value.has("static") ? node.extras.value.getBoolean("static") : false;

        EntityComponent.getInstance(null).addNodeEntity(
            nodeBone,
            data.isRigid,
            isStatic
        );
    }

    private void processingTriggerNode(GLTFNode node, Node nodeBone) {
        if(node.extras == null) return;
        JsonValue jsonValue = node.extras.value;

        switch (jsonValue.getString("type").toLowerCase()) {
            case "damage": {
                BoundingBox box = new BoundingBox();
                nodeBone.calculateBoundingBox(box);

                Trigger trigger = TriggerComponent.getInstance().triggerPool.obtain();
                trigger.isSingleUse = false;
                trigger.triggerZone = box;

                float quantity = jsonValue.getFloat("quantity");
                float per = jsonValue.getFloat("per");
                final float[] timer = {0};
                final long[] lastTime = {System.currentTimeMillis()}; // Запоминаем начальное время

                trigger.action = () -> {
                    long currentTime = System.currentTimeMillis();
                    float deltaTime = (currentTime - lastTime[0]) / 1000f; // Переводим в секунды
                    lastTime[0] = currentTime; // Обновляем время

                    timer[0] += deltaTime;
                    if (timer[0] >= per) {
                        Level.getInstance().getPlayer().addDamage(quantity);
                        timer[0] = 0;
                    }
                };

                TriggerComponent.getInstance().putTrigger(trigger);
                break;
            }
            case "secret": {
                BoundingBox box = new BoundingBox();
                nodeBone.calculateBoundingBox(box);

                Trigger trigger = TriggerComponent.getInstance().triggerPool.obtain();
                trigger.isSingleUse = true;
                trigger.triggerZone = box;

                String sound = jsonValue.has("sound") ? jsonValue.getString("sound") : null;

                trigger.action = () -> {
                    Level.getInstance().getPlayer().getHUD().setMessage("Вы нашли секрет!", Color.WHITE);

                    if(sound != null && sound.equals("doom")){
                        AudioComponent.getInstance().secretDoom.play(AudioConfiguration.SOUND.getValue());
                        return;
                    }

                    AudioComponent.getInstance().secret.play(AudioConfiguration.SOUND.getValue());
                };
                TriggerComponent.getInstance().putTrigger(trigger);
                break;
            }
            case "button": {
                Trigger trigger = TriggerComponent.getInstance().triggerPool.obtain();
                trigger.isSingleUse =  true;

                int size = nodeBone.getChildCount();
                for(int i = 0; i < size; i++){
                    Node targetNodeModel = nodeBone.getChild(i);
                    GLTFNode targetNode = findMatchingGLTFNode(targetNodeModel);

                    if(targetNode != null && isDoorNode(targetNode)) {
                        DoorMesh mesh = processingDoorNode(targetNodeModel, targetNode);
                        mesh.setTrigger(trigger);
                    }
                }

                String event = jsonValue.has("event") ? jsonValue.getString("event") : null;
                if(event != null){
                    trigger.action = () -> {

                    };
                }

                // Создаем физическое тело для триггера
                TriggerBody body = PhysicComponent.getInstance().addTriggerObject(nodeBone);
                body.setTrigger(trigger);

                BoundingBox box = new BoundingBox();
                body.getAabb(box.min, box.max);

                trigger.triggerZone = box;

                break;
            }
            case "doorclose" : {
                BoundingBox box = new BoundingBox();
                nodeBone.calculateBoundingBox(box);

                Trigger trigger = TriggerComponent.getInstance().triggerPool.obtain();
                trigger.isSingleUse =  true;
                trigger.triggerZone = box;

                int size = nodeBone.getChildCount();
                for(int i = 0; i < size; i++){
                    Node targetNodeModel = nodeBone.getChild(i);
                    GLTFNode targetNode = findMatchingGLTFNode(targetNodeModel);

                    if(targetNode != null && isDoorNode(targetNode)) {
                        DoorMesh mesh = processingDoorNode(targetNodeModel, targetNode);
                        mesh.setTrigger(trigger);
                    }
                }

                break;
            }
            case "spawn": {


                Trigger trigger = TriggerComponent.getInstance().triggerPool.obtain();
                trigger.isSingleUse = true;
                trigger.isActive = true;

                NPCComponent component = NPCComponent.getInstance(getGraph(), level.getPlayer());

                Array<NPC> npcs = new Array<>();
                int size = nodeBone.getChildCount();
                for(int i = 0; i < size; i++){
                    Node targetNodeModel = nodeBone.getChild(0);
                    GLTFNode targetNode = findMatchingGLTFNode(targetNodeModel);

                    if(targetNode != null && isNPC(targetNode)) {

                        Matrix4 worldTransform = new Matrix4(targetNodeModel.calculateWorldTransform());
                        // 3. Сбрасываем локальную трансформацию
                        targetNodeModel.translation.set(0, 0, 0);
                        targetNodeModel.rotation.idt();
                        targetNodeModel.scale.set(1, 1, 1);
                        targetNodeModel.localTransform.idt();

                        // 4. Применяем мировую трансформацию
                        targetNodeModel.localTransform.set(worldTransform);

                        NPC npc = ResourcesLoader.getBot(targetNode.extras.value.getString("name"));
                        npc.setPosition(component.findNearestNode(targetNodeModel.localTransform.getTranslation(new Vector3())), 0.5f);
                        npc.setNodeGraph(getGraph());
                        npcs.add(npc);

                        nodeBone.removeChild(targetNodeModel);
                    }
                }

                BoundingBox box = new BoundingBox();
                nodeBone.calculateBoundingBox(box);
                trigger.triggerZone = box;

                NPCComponent.triggerArrayMap.put(trigger, npcs);
                System.out.println(NPCComponent.triggerArrayMap.size());
                TriggerComponent.getInstance().putTrigger(trigger);
                break;
            }
        }
    }

    private void processingItemNode(GLTFNode node, Node nodeBone){
        JsonValue jsonValue = node.extras.value;

        ItemEntity itemEntity = ResourcesLoader.getItem(jsonValue.getString("name"));
        itemEntity.init(nodeBone.translation, level.getManager().environment);
        ItemsComponent.getInstance().putItem(itemEntity);
    }

    private DoorMesh processingDoorNode(Node nodeBone, GLTFNode node){
        JsonValue value = node.extras != null ? node.extras.value : null;
        String type = value.has("type") ? value.getString("type") : null ;
        float xMove = value.getFloat("x");
        float yMove = value.getFloat("y");
        float zMove = value.getFloat("z");

        DoorMesh doorMesh = new DoorMesh(nodeBone);
        doorMesh.putData(
            type, xMove, yMove, zMove
        );

        level.getDoorMeshes().add(doorMesh);
        return doorMesh;
    }

    private void processParticleNode(GLTFNode node){
        if(node.extras != null){
            JsonValue jsonValue = node.extras.value;
            String name = jsonValue.getString("name");
            ParticleEffect effect = ParticlesComponent.getInstance(null).findEffect(name);
            if(effect != null) {
                effect.init();
                effect.start();
                effect.translate(new Vector3(node.translation));
                ParticlesComponent.getInstance(null).addEffect(effect);
            }
        }
    }

    private void processOccluderNode(Node node) {
        PhysicComponent.getInstance().addOccluderObject(node);
    }

    private void processNocallbackNode(Node node) {
        PhysicComponent.getInstance().addNocallbackObject(node);
    }

    private DecorationData extractDecorationData(GLTFNode node) {
        DecorationData data = new DecorationData();
        if (node.extras != null) {
            JsonValue value = node.extras.value;
            data.isRigid = value.getBoolean("rigid");
        }
        return data;
    }

    private Quaternion createQuaternion(GLTFNode node) {
        float[] rot = node.rotation;
        return (rot != null)
            ? new Quaternion(rot[0], rot[1], rot[2], rot[3])
            : new Quaternion();
    }

    private void processingCloudNode(Node nodeBone) {
        CloudsBatch.getCloudsMeshes().add(new CloudMesh(nodeBone));
    }

    private void processingWaterNode(Node nodeBone) {
        WaterBatch.getWaterMeshes().add(new WaterMesh(nodeBone));
        PhysicComponent.getInstance().addWaterMesh(nodeBone);
    }

    private void processLightNode(GLTFNode node) {
        Node lightNode = level.getMapModel().scene.model.getNode(node.name);
        var light = level.getMapModel().scene.lights.get(lightNode);

        if (light instanceof PointLightEx) {
            PointLightEx pointLightEx = LightingComponent.getInstance().getPointLightPool().obtain();
            pointLightEx.set((PointLightEx) light);
            pointLightEx.setPosition(lightNode.translation);
            processPointLight(node, pointLightEx);
        } else if (light instanceof SpotLightEx) {
            level.getManager().environment.add((SpotLightEx) light);
        }
    }

    private void processPointLight(GLTFNode node, PointLightEx pointLightEx) {

        JsonValue value = node.extras != null ? node.extras.value : null;
        boolean isFlick = value != null && value.getBoolean("flick", false);
        boolean isLinear = value != null && value.getBoolean("linear", false);

        if (isFlick) {
            handleFlickLight(pointLightEx, value);
        } else if (isLinear) {
            handleLinearLight(pointLightEx, value);
        } else {
            level.getManager().environment.add(pointLightEx);
            LightingComponent.getInstance().putLight(pointLightEx);
        }
    }

    private void handleFlickLight(PointLightEx pointLightEx, JsonValue value) {
        DynamicLight dynamicLight = new DynamicLight(
            value.getFloat("min"),
            value.getFloat("speed"),
            pointLightEx.intensity,
            value.getFloat("speedchange")
        );
        dynamicLight.set(pointLightEx);
        LightingComponent.getInstance().putLight(dynamicLight);
        level.getManager().environment.add(dynamicLight);
    }

    private void handleLinearLight(PointLightEx pointLightEx, JsonValue value) {
        LinearLight linearLight = new LinearLight(
            value.getFloat("min"),
            value.getFloat("speed"),
            pointLightEx.intensity,
            0f,
            value.getFloat("time")
        );
        linearLight.set(pointLightEx);
        LightingComponent.getInstance().putLight(linearLight);
        level.getManager().environment.add(linearLight);
    }

    private void processPathfindingNode(GLTFNode node) {
        var currentNode = new com.hexedrealms.utils.NPCPathfinder.Node(node.translation);
        currentNode.name = node.name;
        graph.addNode(currentNode);
    }

    public NodeGraph getGraph() {
        return graph;
    }

    public void cleanUp() {
        iterator = null;
        level = null;
        graph = null;
    }

    public static class DecorationData {
        String name;
        public boolean isRigid;
        boolean isStatic;
    }
}
