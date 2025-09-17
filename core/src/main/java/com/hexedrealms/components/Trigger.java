package com.hexedrealms.components;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.hexedrealms.components.bulletbodies.TriggerBody;
import com.hexedrealms.engine.Player;

public class Trigger {
    protected BoundingBox triggerZone;
    public boolean isActive = true;
    protected boolean isSingleUse;
    public Runnable action;

    public Trigger(BoundingBox triggerZone, boolean isSingleUse, Runnable action){
        this.triggerZone = triggerZone;
        this.isSingleUse = isSingleUse;
        this.action = action;
    }

    public BoundingBox getTriggerZone(){
        return triggerZone;
    }

    public void check(Player player) {

        if (!isActive) return;

        if (triggerZone != null && triggerZone.contains(player.getCamera().position)) {
            activate();
        }
    }

    public void activate(){
        if(action != null) action.run();
        if (isSingleUse) deactivate();
    }

    public boolean isActive() {
        return isActive;
    }

    public void deactivate() {
        isActive = false;
    }

    @Override
    public String toString() {
        return "Trigger{" +
            "triggerZone=" + triggerZone +
            '}';
    }
}
