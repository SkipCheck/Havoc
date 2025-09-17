package com.hexedrealms.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.visuicomponents.CustomVerticalGroup;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisLabel;

public class DialogStage extends ModalStage{
    private VisLabel labelArea;
    public VisImageTextButton cancel;
    public VisImageTextButton apply;
    public MenuStage menuStage;

    public DialogStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);
        verticalGroup.addActor(labelArea);
        label.setFontScale(1.25f);

        createButtons();
        addButtonsToGroup();
    }

    public void setText(String text){
        labelArea.setText(text);
        updateButtonStyle();
    }

    protected void addButtonsToGroup() {
        clear();

        updateGroupLayout();
        verticalGroup.addActor(bottomGroup);

        addActor(verticalGroup);
    }

    protected void initComponents() {
        buttons = new Array<>();

        verticalGroup = new CustomVerticalGroup(VisUI.getSkin().get("dialog", Window.WindowStyle.class).background);
        buttonsGroup = new VerticalGroup();
        horizontalGroup = new HorizontalGroup().space(55);
        bottomGroup = new HorizontalGroup().space(55).padTop(-10);
        events = new VerticalGroup().fill().left().padLeft(20).padTop(-30);

        labelArea = new VisLabel("Text", VisUI.getSkin().get("labelarea", Label.LabelStyle.class));
        labelArea.setAlignment(Align.center, Align.center);
    }

    protected void createButtons() {
        cancel = new VisImageTextButton("Отмена", VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        cancel.getLabelCell().center();
        cancel.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                GUIComponent.getInstance().removeStage(DialogStage.this);
            }
        });
        apply = new VisImageTextButton("Подтвердить", VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        apply.getLabelCell().center();

        bottomGroup.addActor(cancel);
        bottomGroup.addActor(apply);
        bottomGroup.center();
    }

    protected void calcVerticalGroupSize(){
        width = getViewport().getWorldWidth() * 0.35f;
        height = getViewport().getWorldHeight() * 0.42f;

        verticalGroup.setSize(width, height);
    }

    protected void updateButtonStyle() {

        Label.LabelStyle style =  VisUI.getSkin().get("labelarea", Label.LabelStyle.class);
        style.background.setMinWidth(verticalGroup.getWidth() * 0.7f);
        style.background.setMinHeight(verticalGroup.getHeight() * 0.5f);

        VisImageTextButton.VisImageTextButtonStyle back = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);

        float backWidth = bottomGroup.getWidth() * 0.3f;
        float backHeight = bottomGroup.getHeight() * 0.2f;

        back.up.setMinWidth(backWidth);
        back.up.setMinHeight(backHeight);
        bottomGroup.fill();
        bottomGroup.expand();
    }

    protected void updateGroupLayout() {
        calcVerticalGroupSize();
        verticalGroup.setPosition(getViewport().getWorldWidth() / 2 - width / 2, getViewport().getWorldHeight() / 2 - height / 2);
        verticalGroup.space(height * 0.06f);
        verticalGroup.padTop(height * 0.06f);

        horizontalGroup.setSize(width, height);
        bottomGroup.setSize(width, height);
    }
}
