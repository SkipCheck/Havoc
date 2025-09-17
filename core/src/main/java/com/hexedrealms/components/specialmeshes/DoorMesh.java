package com.hexedrealms.components.specialmeshes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.Trigger;
import com.hexedrealms.components.bulletbodies.DoorBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.engine.AudioComponent;
import com.hexedrealms.engine.PhysicComponent;
import com.hexedrealms.engine.TriggerComponent;
import com.hexedrealms.screens.Level;

import java.security.Key;

public class DoorMesh implements Disposable {
    private DoorBody body;
    private Trigger trigger;
    private Mesh mesh;
    private Vector3 closedPosition = new Vector3();
    private Vector3 openPosition = new Vector3();
    private Matrix4 transform;
    private BoundingBox box;

    public String type;
    public float xMove;
    public float yMove;
    public float zMove;

    private boolean isOpen;

    public DoorMesh(Node node){
        node.calculateWorldTransform();

        box = new BoundingBox();
        node.calculateBoundingBox(box);


        transform = new Matrix4();
        closedPosition.set(node.translation);

        mesh = node.parts.get(0).meshPart.mesh;

        if(node.hasParent()) {
            Matrix4 worldTransform = new Matrix4(node.calculateWorldTransform());
            // 3. Сбрасываем локальную трансформацию
            node.translation.set(0, 0, 0);
            node.rotation.idt();
            node.scale.set(1, 1, 1);
            node.localTransform.idt();

            // 4. Применяем мировую трансформацию
            node.localTransform.set(worldTransform);
        }

        this.body = PhysicComponent.getInstance().addDoorObject(node);

        if(box.getDepth() < box.getWidth()){
            box.min.z -= box.getDepth() * 5f;
            box.max.z += box.getDepth() * 5f;
        }else {
            box.min.x -= box.getWidth() * 5f;
            box.max.x += box.getWidth() * 5f;
        }

        this.trigger = new Trigger(box, false, () -> {

            String message = "Нажмите "+Input.Keys.toString((Integer) ControlsConfiguration.EVENT.getValue());
            Color color = new Color(1,1,1,1);

            if(type != null && !Level.getInstance().getPlayer().containsKey(type)) {
                switch (type) {
                    case "blue_key": {
                        message = "Нужен синий ключ";
                        color = new Color(0.26f, 0.58f, 0.89f, 1f);
                        break;
                    }
                    case "red_key": {
                        message = "Нужен красный ключ";
                        color = new Color(0.89f, 0.26f, 0.26f, 1f);
                        break;
                    }
                    case "green_key": {
                        message = "Нужен зеленый ключ";
                        color = new Color(0.27f, 0.89f, 0.26f, 1f);
                        break;
                    }
                }
            }

            Level.getInstance().getPlayer().getHUD().setMessage(message, color);

            if(Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.EVENT.getValue()) && ((Level.getInstance().getPlayer().containsKey(type)) || type == null)){
                isOpen = true;
                trigger.deactivate();
                AudioComponent.getInstance().openDoor.play(AudioConfiguration.SOUND.getValue());
            }else if(Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.EVENT.getValue())){
                AudioComponent.getInstance().knockDoor.play(AudioConfiguration.SOUND.getValue());
            }
        });


        TriggerComponent.getInstance().putTrigger(trigger);
    }

    public void setTrigger(Trigger trigger){
        this.trigger.deactivate();
        this.trigger = trigger;
        TriggerComponent.getInstance().putTrigger(trigger);
    }

    public void putData(String type, float xMove, float yMove, float zMove){
        this.type = type;
        this.xMove = xMove;
        this.yMove = yMove;
        this.zMove = zMove;

        float positionXClose = xMove > 0 ? box.getWidth()  * 0.5f * 2 : xMove < 0 ? box.getWidth() * 0.5f * -2 : 0;
        float positionYClose = yMove > 0 ? box.getHeight() * 0.5f * 2 : yMove < 0 ? box.getHeight() * 0.5f * -2 : 0;
        float positionZClose = zMove > 0 ? box.getDepth() * 0.5f * 2 : zMove < 0 ? box.getDepth() * 0.5f * -2 : 0;

        openPosition.set(closedPosition).add(positionXClose, positionYClose, positionZClose);
    }

    public void update(float delta){
        if(trigger != null && !TriggerComponent.getInstance().isContains(trigger) && !trigger.isActive){
            trigger = null;
            isOpen = true;
            AudioComponent.getInstance().openDoor.play(AudioConfiguration.SOUND.getValue());
        }
        if(!isOpen) return;

        Vector3 move = new Vector3(xMove * delta * 2f, yMove * delta * 2f,zMove * delta * 2f);

        transform.set(new Matrix4()).translate(move);
        mesh.transform(transform);
        body.translate(move);

        if(openPosition.dst(closedPosition.add(transform.getTranslation(new Vector3()))) < 1.0f){
            isOpen = false;
        }
    }

    @Override
    public void dispose() {
        // Dispose physics body
        if (body != null) {
            PhysicComponent.getInstance().disposeRigidBody(body);
            body = null;
        }

        // Dispose trigger
        if (trigger != null) {
            trigger.deactivate();
            TriggerComponent.getInstance().putTrigger(trigger); // Ensure proper cleanup
            trigger = null;
        }

        // Dispose mesh
        if (mesh != null) {
            mesh = null;
        }

        // Clear references
        closedPosition = null;
        openPosition = null;
        transform = null;
        box = null;
        type = null;
    }
}
