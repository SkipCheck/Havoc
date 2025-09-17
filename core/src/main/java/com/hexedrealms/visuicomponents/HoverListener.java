package com.hexedrealms.visuicomponents;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public abstract class HoverListener extends ClickListener {

    @Override
    public void clicked(InputEvent event, float x, float y) {
        super.clicked(event, x, y);
    }
}
