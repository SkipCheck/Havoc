package com.hexedrealms.utils.savedata;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.hexedrealms.components.NPC;

import java.io.Serializable;

public class NPCData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public Vector3 position;
    public float health;
    public State<NPC> state;
}
