package com.hexedrealms.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.Main;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;

public class PauseStage extends MenuStage {
    private boolean isFailed;
    private VisImageTextButton continueButton;
    private VisImageTextButton saveButton;

    public PauseStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    public void setFailed(boolean isFailed){
        this.isFailed = isFailed;
        if(isFailed){
            label.setText("Вы проиграли");
            continueButton.setDisabled(true);
            saveButton.setDisabled(true);
        }
    }

    // In PauseStage.java
    protected void createButtons() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("default", VisImageTextButton.VisImageTextButtonStyle.class);

        // Кнопка "Продолжить"
        continueButton = createButton("Продолжить", style);
        continueButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if(!continueButton.isDisabled()){
                    GUIComponent.getInstance().removeStage(PauseStage.this);
                }
            }
        });

        // Кнопка "Сохранить игру"
        saveButton = createButton("Сохранить игру", style);
        saveButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if(!saveButton.isDisabled()){
                    SlotSaves slotSaves = (SlotSaves) GUIComponent.getInstance().getMenuStage(SlotSaves.class);
                    slotSaves.setMode(SlotSaves.Mode.SAVE);
                    GUIComponent.getInstance().putStage(slotSaves);
                }

            }
        });

        // Кнопка "Загрузить игру"
        VisImageTextButton loadButton = createButton("Загрузить игру", style);
        loadButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                setFailed(false);
                SlotSaves slotSaves = (SlotSaves) GUIComponent.getInstance().getMenuStage(SlotSaves.class);
                slotSaves.setMode(SlotSaves.Mode.LOAD);
                GUIComponent.getInstance().putStage(slotSaves);
            }
        });

        // Кнопка "Настройки"
        VisImageTextButton settingsButton = createButton("Настройки", style);
        settingsButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage settingsMenu = GUIComponent.getInstance().getMenuStage(SettingsStage.class);
                GUIComponent.getInstance().putStage(settingsMenu);
            }
        });

        // Кнопка "Выйти в главное меню"
        VisImageTextButton mainMenuButton = createButton("Выйти в главное меню", style);
        mainMenuButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                returnToMainMenu();
            }
        });

        // Добавляем кнопки в порядке отображения
        buttons.add(continueButton);
        buttons.add(saveButton);
        buttons.add(loadButton);
        buttons.add(settingsButton);
        buttons.add(createButton("Гримуар нечисти", style));
        buttons.add(mainMenuButton);

        setFocusOnButton(buttons.get(0));
    }

    /**
     * Метод для возврата в главное меню
     */
    private void returnToMainMenu() {

        DialogStage dialogStage = (DialogStage) GUIComponent.getInstance().getMenuStage(DialogStage.class);
        dialogStage.setText("Вы уверены?");
        dialogStage.menuStage = PauseStage.this;
        GUIComponent.getInstance().putStage(dialogStage);

        dialogStage.apply.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                Level.getInstance().dispose();
                Main.getInstance().setScreen(new LoaderStage("textures/backgrounds/e1m1.png", -1, -1, true));
                GUIComponent.getInstance().removeStage(dialogStage);
                GUIComponent.getInstance().removeStage(PauseStage.this);
            }
        });


    }
}
