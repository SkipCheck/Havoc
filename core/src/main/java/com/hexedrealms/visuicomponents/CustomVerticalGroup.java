package com.hexedrealms.visuicomponents;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class CustomVerticalGroup extends VerticalGroup {
    private Drawable background;

    public CustomVerticalGroup(Drawable background) {
        this.background = background;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (background != null) {
            background.draw(batch, getX(), getY(), getWidth(), getHeight());
        }

        super.draw(batch, parentAlpha);
    }
}
