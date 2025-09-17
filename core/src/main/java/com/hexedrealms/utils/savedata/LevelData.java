package com.hexedrealms.utils.savedata;

import com.badlogic.gdx.utils.Array;

import java.io.Serializable;
import java.util.List;

public class LevelData implements Serializable {
    private static final long serialVersionUID = 1L;

    //информация об уровне (сложность, номер слота для сохранения, и номер уровня)
    public float difficulty;
    public int slotID, levelID;

    //сериализованные данные полученные при прохождении уровня
    public PlayerSaveData playerSaveData;
    public List<WeaponData> weaponDataArray;
    public List<NPCData> npcDataArray;
    public List<ItemData> itemDataArray;
    public List<TriggerData> triggerDataArray;
}
