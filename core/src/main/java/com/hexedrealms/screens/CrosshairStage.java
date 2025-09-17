package com.hexedrealms.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.components.HUDCrossHair;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.configurations.CrossHairConfiguration;
import com.hexedrealms.engine.AudioComponent;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.HUD;
import com.hexedrealms.engine.SettingsComponent;
import com.hexedrealms.visuicomponents.HoverListener;
import com.hexedrealms.visuicomponents.SliderActor;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;

public class CrosshairStage extends ModalStage{
    private SliderActor red;
    private SliderActor green;
    private SliderActor blue;
    private SliderActor distance;
    private SliderActor size;
    private SliderActor thickness;
    private HUDCrossHair crossHair;

    public CrosshairStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    protected void setupUI() {
        crossHair = new HUDCrossHair();
        crossHair.setEnabled(true);
        verticalGroup.fill();
        verticalGroup.addActor(label);
        crossHair.setZIndex(Integer.MAX_VALUE);
        verticalGroup.addActor(crossHair);

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
                GUIComponent.getInstance().removeStage(CrosshairStage.this);
                preventDefault();
            }
        });

        Color color = (Color) CrossHairConfiguration.COLOR.getValue();

        red = new SliderActor("Красный", 0f, 1f, 0.01f, color.r);
        red.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((Color)CrossHairConfiguration.COLOR.getValue()).r = red.getValue();
            }
        });

        green = new SliderActor("Зеленый", 0f, 1f, 0.01f, color.g);
        green.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((Color)CrossHairConfiguration.COLOR.getValue()).g = green.getValue();
            }
        });

        blue = new SliderActor("Синий", 0f, 1f, 0.01f, color.b);
        blue.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((Color)CrossHairConfiguration.COLOR.getValue()).b = blue.getValue();
            }
        });

        distance = new SliderActor("Дистанция", 0.1f, 50f, 0.01f, (Float) CrossHairConfiguration.DISTANCE.getValue());
        distance.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = distance.getValue();
                distance.setText(String.format("%.0f", value));
                CrossHairConfiguration.DISTANCE.setValue(value);
            }
        });
        distance.setText(String.format("%.0f", (Float) CrossHairConfiguration.DISTANCE.getValue()));

        size = new SliderActor("Длина линии", 0.1f, 50f, 0.01f,  (Float) CrossHairConfiguration.SIZE.getValue());
        size.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = size.getValue();
                size.setText(String.format("%.0f", value));
                CrossHairConfiguration.SIZE.setValue(value);
            }
        });
        size.setText(String.format("%.0f", (Float) CrossHairConfiguration.SIZE.getValue()));

        thickness = new SliderActor("Толщина линии", 0.1f, 50f, 0.01f,  (Float) CrossHairConfiguration.THICKNESS.getValue());
        thickness.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = thickness.getValue();
                thickness.setText(String.format("%.0f", value));
                CrossHairConfiguration.THICKNESS.setValue(value);
            }
        });
        thickness.setText(String.format("%.0f", (Float) CrossHairConfiguration.THICKNESS.getValue()));

        events.addActor(red);
        events.addActor(green);
        events.addActor(blue);
        events.addActor(distance);
        events.addActor(size);
        events.addActor(thickness);

        bottomGroup.addActor(back);
        bottomGroup.center();
    }



    @Override
    public boolean preventDefault() {
        SettingsComponent.getInstance().saveCrosshair();
        return false;
    }

    @Override
    public void act(float delta){
        super.act(delta);
        crossHair.render(delta);
    }

    protected void updateGroupLayout() {
        calcVerticalGroupSize();

        float positionX = getViewport().getWorldWidth() - width;

        verticalGroup.setPosition(positionX, 0);
        verticalGroup.space(height * 0.06f);
        verticalGroup.padTop(height * 0.05f);

        events.padTop(height * 0.1f);
        events.setWidth(width * 0.75f);

        horizontalGroup.setSize(width, height);
        bottomGroup.setSize(width, height);
        buttonsGroup.setSize(width, height);

        bottomGroup.padTop(height * -0.03f);

        red.setWidth(events.getWidth());
        green.setWidth(events.getWidth());
        blue.setWidth(events.getWidth());
        distance.setWidth(events.getWidth());
        size.setWidth(events.getWidth());
        thickness.setWidth(events.getWidth());



        crossHair.setCenterX(positionX + events.getWidth() / 1.55f);
        crossHair.setCenterY(height - height * 0.22f);

        updateCheckBoxStyle();
        verticalGroup.invalidateHierarchy();
        verticalGroup.layout();
        events.invalidateHierarchy();
        events.layout();
    }
}
