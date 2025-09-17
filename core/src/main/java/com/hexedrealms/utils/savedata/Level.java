package com.hexedrealms.utils.savedata;

import java.awt.*;
import java.io.Serializable;

public class Level implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public String ambientMusic;
    public String fightMusic;
    public Color ambientColor;
    public String splashText;
    public String model;

    public Level(String name, String ambientMusic, String fightMusic, Color ambientColor, String splashText, String model) {
        this.name = name;
        this.ambientMusic = ambientMusic;
        this.fightMusic = fightMusic;
        this.ambientColor = ambientColor;
        this.splashText = splashText;
        this.model = model;
    }
}
