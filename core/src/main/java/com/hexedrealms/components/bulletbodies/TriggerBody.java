package com.hexedrealms.components.bulletbodies;

import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.hexedrealms.components.Trigger;

public class TriggerBody extends btRigidBody {
    private Trigger trigger;
    public TriggerBody(btRigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }
    public void setTrigger(Trigger trigger){
        this.trigger = trigger;
    }

    public void activateTrigger(){
        if(trigger != null) {
            trigger.activate();
        }
    }

    public boolean isTriggerActived(){
        return trigger != null && trigger.isActive;
    }

    public void disposeTrigger(){
        trigger = null;
    }
}
