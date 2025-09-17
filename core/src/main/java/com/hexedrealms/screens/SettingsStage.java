package com.hexedrealms.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;

public class SettingsStage extends MenuStage{

    public SettingsStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    protected void createButtons() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("default", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle back = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);

        VisImageTextButton backButton = createButton("Назад", back);
        backButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                GUIComponent.getInstance().removeStage(SettingsStage.this);
            }
        });

        VisImageTextButton graphicsButton = createButton("Настройки графики", style);
        graphicsButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage stage = GUIComponent.getInstance().getMenuStage(ModalStage.class);
                GUIComponent.getInstance().putStage(stage);
            }
        });

        VisImageTextButton audioButton = createButton("Настройки звука", style);
        audioButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage stage = GUIComponent.getInstance().getMenuStage(AudioStage.class);
                GUIComponent.getInstance().putStage(stage);
            }
        });

        VisImageTextButton controlsButton = createButton("Настройки управления", style);
        controlsButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage stage = GUIComponent.getInstance().getMenuStage(ControlsStage.class);
                GUIComponent.getInstance().putStage(stage);
            }
        });

        VisImageTextButton crosshairButton = createButton("Настройки прицела", style);
        crosshairButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage stage = GUIComponent.getInstance().getMenuStage(CrosshairStage.class);
                GUIComponent.getInstance().putStage(stage);
            }
        });

        buttons.add(graphicsButton);
        buttons.add(audioButton);
        buttons.add(controlsButton);
        buttons.add(crosshairButton);
        buttons.add(backButton);

        setFocusOnButton(buttons.get(0));
    }
}
