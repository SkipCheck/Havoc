package com.hexedrealms.visuicomponents;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisLabel;

public class ScrollActor extends HorizontalGroup{
    private final String [] labels;
    private VisLabel label, text;
    private VisImageButton prev;
    private VisImageButton next;
    private Table group;
    private int currentIndex, previousIndex;

    public ScrollActor(String name, String [] labels, int selectedIndex){
        super();
        this.labels = labels;
        currentIndex = previousIndex = selectedIndex;
        (this.label = new VisLabel())
            .setText(name);
        (this.text = new VisLabel())
            .setText(labels[labels.length-1]);

        text.setStyle(VisUI.getSkin().get("withback", Label.LabelStyle.class));
        text.setAlignment(Align.center, Align.center);
        text.setText(labels[currentIndex]);

        prev = new VisImageButton(VisUI.getSkin().get("arrow-1", VisImageButton.VisImageButtonStyle.class));
        prev.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                currentIndex--;
                if(currentIndex == -1) currentIndex = labels.length-1;
                text.setText(labels[currentIndex]);
            }
        });

        next = new VisImageButton(VisUI.getSkin().get("arrow-2", VisImageButton.VisImageButtonStyle.class));
        next.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                currentIndex++;
                if(currentIndex == labels.length) currentIndex = 0;
                text.setText(labels[currentIndex]);
            }
        });

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                label.setColor(VisUI.getSkin().get("default", VisCheckBox.VisCheckBoxStyle.class).overFontColor);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                // Если мы переходим на дочерний элемент или его потомка, не изменяем цвет
                if (toActor == null || !isChildOrSelf(toActor)) {
                    label.setColor(VisUI.getSkin().get("default", VisCheckBox.VisCheckBoxStyle.class).fontColor);
                }
            }

            private boolean isChildOrSelf(Actor actor) {
                Actor current = actor;
                while (current != null) {
                    if (current == ScrollActor.this) return true;
                    current = current.getParent();
                }
                return false;
            }
        });

        group = new Table();
        group.add(label);
        group.add(prev);
        group.add(text).padLeft(5f).padRight(5f);
        group.add(next);
        addActor(group);
        pad(0,0,20,0);
    }

    public int getCurrentIndex(){
        return currentIndex;
    }

    protected void updateButtonStyle() {
        VisImageButton.VisImageButtonStyle leftStyle = VisUI.getSkin().get("arrow-1", VisImageButton.VisImageButtonStyle.class);
        VisImageButton.VisImageButtonStyle rightStyle = VisUI.getSkin().get("arrow-2", VisImageButton.VisImageButtonStyle.class);
        Label.LabelStyle labelStyle = VisUI.getSkin().get("withback", Label.LabelStyle.class);

        float width = this.getWidth();

        float heightButtons = width * 0.06f;
        float widthButtons = width * 0.04f;
        float heightLabel = width * 0.1f;
        float widthLabel = width * 0.5f;
        group.getCell(label).width(width * 0.3f);

        leftStyle.up.setMinWidth(widthButtons);
        leftStyle.up.setMinHeight(heightButtons);
        updateTextureFilter(leftStyle.up);
        updateTextureFilter(leftStyle.over);
        updateTextureFilter(leftStyle.down);

        rightStyle.up.setMinWidth(widthButtons);
        rightStyle.up.setMinHeight(heightButtons);
        updateTextureFilter(rightStyle.up);
        updateTextureFilter(rightStyle.over);
        updateTextureFilter(rightStyle.down);

        labelStyle.background.setMinWidth(widthLabel);
        labelStyle.background.setMinHeight(heightLabel);
        updateTextureFilter(labelStyle.background);
    }

    @Override
    public void setWidth(float width){
        super.setWidth(width);
        updateButtonStyle();
    }

    protected void updateTextureFilter(Drawable drawable) {
        if (drawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable textureRegionDrawable = (TextureRegionDrawable) drawable;
            Texture texture = textureRegionDrawable.getRegion().getTexture();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }

    public void setCurrentIndex(int initialResolutionIndex) {
        currentIndex = initialResolutionIndex;
    }
}
