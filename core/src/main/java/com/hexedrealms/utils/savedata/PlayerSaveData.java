package com.hexedrealms.utils.savedata;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.hexedrealms.weapons.Weapon;

import java.io.Serializable;
import java.util.List;

public class PlayerSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public Matrix4 transform;
    public Vector3 cameraUp;
    public Vector3 cameraDirection;
    public List<String> keys;

    public float armorHealth;
    public float health;
    public float armor;
    public float strength;
    public float agility;
    public float vitality;
    public float intelligence;
}
