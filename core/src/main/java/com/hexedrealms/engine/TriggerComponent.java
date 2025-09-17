package com.hexedrealms.engine;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TriggerComponent implements Disposable {
    private static final AtomicReference<TriggerComponent> instance = new AtomicReference<>();
    private final List<Trigger> triggers = new ArrayList<>();
    public Pool<Trigger> triggerPool;

    public void putTrigger(Trigger trigger){
        this.triggers.add(trigger);
    }

    public TriggerComponent(){
        triggerPool = new Pool<Trigger>() {
            @Override
            protected Trigger newObject() {
                return new Trigger(null, false, null);
            }
        };
    }

    public void update(Player player) {
        for (Trigger trigger : triggers) {
            trigger.check(player);
        }
        triggers.removeIf(t -> !t.isActive()); // Удаляем деактивированные
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public boolean isContains(Trigger trigger){
        return triggers.contains(trigger);
    }

    public static TriggerComponent getInstance() {
        TriggerComponent result = instance.get();
        if (result == null) {
            synchronized (TriggerComponent.class) {
                result = instance.get();
                if (result == null) {
                    result = new TriggerComponent();
                    instance.set(result);
                }
            }
        }
        return result;
    }

    @Override
    public void dispose() {
        // Clear all triggers
        triggers.clear();

        // Free the trigger pool
        if (triggerPool != null) {
            triggerPool.clear();
            triggerPool = null;
        }

        // Reset singleton instance
        instance.set(null);
    }
}
