package com.hexedrealms.screens;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.AudioComponent;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.SettingsComponent;
import com.hexedrealms.visuicomponents.HoverListener;
import com.hexedrealms.visuicomponents.SliderActor;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;

public class AudioStage extends ModalStage{
    private SliderActor common;
    private SliderActor music;
    private SliderActor sound;

    public AudioStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);

        createButtons();
        addButtonsToGroup();
    }

    protected void addButtonsToGroup() {
        clear();

        updateGroupLayout();
        verticalGroup.addActor(events);
        verticalGroup.addActor(bottomGroup);


        addActor(verticalGroup);
    }

    protected void createButtons() {
        VisImageTextButton back = new VisImageTextButton("Назад", VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        back.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                GUIComponent.getInstance().removeStage(AudioStage.this);
                preventDefault();
            }
        });

        common = new SliderActor("Общая громкость", 0f, 1f, 0.01f, AudioConfiguration.COMMON.getValue());
        common.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(common.getValue() == 0f) common.setText("Выкл");
                AudioConfiguration.COMMON.setValue(common.getValue());
                AudioComponent.getInstance().setCommonVolume(common.getValue());
            }
        });

        music = new SliderActor("Громкость музыки", 0f, 1f, 0.01f, AudioConfiguration.MUSIC.getValue());
        music.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(music.getValue() == 0f) music.setText("Выкл");
                AudioConfiguration.MUSIC.setValue(music.getValue());
                AudioComponent.getInstance().setMusicVolume(music.getValue());
            }
        });

        sound = new SliderActor("Громкость звуков", 0f, 1f, 0.01f, AudioConfiguration.SOUND.getValue());
        sound.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(sound.getValue() == 0f) sound.setText("Выкл");
                AudioConfiguration.SOUND.setValue(sound.getValue());
            }
        });

        events.addActor(common);
        events.addActor(music);
        events.addActor(sound);
        events.space(30);

        bottomGroup.addActor(back);
        bottomGroup.center();
    }

    @Override
    public boolean preventDefault() {
        SettingsComponent.getInstance().saveAudio();
        return false;
    }

    protected void updateGroupLayout() {
        calcVerticalGroupSize();


        verticalGroup.setPosition(getViewport().getWorldWidth() - width, 0);
        verticalGroup.space(height * 0.06f);
        verticalGroup.padTop(height * 0.05f);

        events.padTop(height * 0.05f);

        horizontalGroup.setSize(width, height);
        bottomGroup.setSize(width, height);
        buttonsGroup.setSize(width, height);

        events.setWidth(width * 0.75f);

        common.setWidth(events.getWidth());
        music.setWidth(events.getWidth());
        sound.setWidth(events.getWidth());

        updateCheckBoxStyle();
        verticalGroup.invalidateHierarchy();
        verticalGroup.layout();
        events.invalidateHierarchy();
        events.layout();
    }
}
