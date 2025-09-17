package com.hexedrealms.utils.NPCSteering;

import com.badlogic.gdx.ai.steer.Proximity;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class RadiusProximity implements Proximity<Vector3> {
    private Steerable<Vector3> owner;  // The NPC using the proximity
    private Array<Steerable<Vector3>> agents;  // All other NPCs to check
    private float radius;  // The radius for detection

    public RadiusProximity(Steerable<Vector3> owner, Array<Steerable<Vector3>> agents, float radius) {
        this.owner = owner;
        this.agents = agents;
        this.radius = radius;
    }

    @Override
    public Steerable<Vector3> getOwner() {
        return owner;
    }

    @Override
    public void setOwner(Steerable<Vector3> owner) {
        this.owner = owner;
    }

    @Override
    public int findNeighbors(ProximityCallback<Vector3> callback) {
        int neighborCount = 0;
        for (Steerable<Vector3> agent : agents) {
            // Skip self
            if (agent == owner) continue;

            // Check if within radius
            if (agent.getPosition().dst(owner.getPosition()) <= radius) {
                // Notify the callback
                if (callback.reportNeighbor(agent)) {
                    neighborCount++;
                }
            }
        }
        return neighborCount;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
