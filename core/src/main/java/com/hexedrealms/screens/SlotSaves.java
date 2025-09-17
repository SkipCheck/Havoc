package com.hexedrealms.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.Main;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.LoaderComponent;
import com.hexedrealms.engine.ResourcesLoader;
import com.hexedrealms.utils.savedata.Level;
import com.hexedrealms.utils.savedata.LevelData;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.Date;

public class SlotSaves extends ModalStage {

    private static final int COLUMNS = 2;
    private static final int ROWS = 4;
    private Table buttonsGrid;
    private Array<VisImageTextButton> slotButtons;
    private int selectedSlotIndex = -1;
    private boolean processingSelection = false;
    private VisImageTextButton nextButton;

    public boolean isMainMenu;

    public enum Mode {
        SAVE,
        LOAD
    }

    private Mode currentMode;

    public void setMode(Mode mode) {
        this.currentMode = mode;
        nextButton.setText(currentMode == null ? "Далее" : currentMode == Mode.SAVE ? "Сохранить" : currentMode == Mode.LOAD ? "Загрузить" : "");
        updateGroupLayout();
        preventDefault();
    }

    public SlotSaves(Viewport viewport, String text) {
        super(viewport, text);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        slotButtons = new Array<>();
        buttonsGrid = new VisTable();
        buttonsGrid.defaults().pad(20);
    }

    @Override
    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);
        calcVerticalGroupSize();

        createButtons();
        addButtonsToGrid();
        addButtonsToGroup();
    }

    @Override
    public boolean preventDefault() {
        refreshSlotData();
        for (VisImageTextButton btn : slotButtons) {
            btn.setChecked(false);
        }
        return false;
    }

    protected void createButtons() {
        slotButtons.clear();

        LoaderComponent loaderComponent = new LoaderComponent();
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            String date = loaderComponent.getFormattedSaveDate("slot"+i);
            LevelData levelData = loaderComponent.loadMetaData("slot"+i);
            Level level = null;

            if(levelData != null)
                level = ResourcesLoader.getLevel(levelData.levelID);

            VisImageTextButton button = new VisImageTextButton(
                level == null ? "Пусто" : level.name+"\n"+date,
                VisUI.getSkin().get("saveslot", VisImageTextButton.VisImageTextButtonStyle.class)
            );
            button.getLabel().setAlignment(Align.center);

            final int index = i;
            final boolean hasData = levelData != null;
            button.addListener(new HoverListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (processingSelection) return;

                    processingSelection = true;
                    try {
                        // Загружаем актуальные данные при клике
                        LevelData currentLevelData = loaderComponent.loadMetaData("slot"+index);
                        boolean currentHasData = currentLevelData != null;

                        if (currentMode == Mode.SAVE || currentMode == null) {
                            if (currentHasData) {
                                showConfirmationDialog(index, button);
                            } else {
                                selectSlot(index, button);
                            }
                        } else if (currentMode == Mode.LOAD) {
                            if (currentHasData) {
                                selectSlot(index, button);
                            } else {
                                if(!isMainMenu)
                                    com.hexedrealms.screens.Level.getInstance().getPlayer().getHUD().setMessage("Слот пуст", Color.RED);
                                selectedSlotIndex = -1;
                            }
                        }
                    } finally {
                        processingSelection = false;
                    }
                }
            });

            slotButtons.add(button);
        }

        // Кнопка Назад (без изменений)
        VisImageTextButton backButton = createButton("Назад",
            VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        backButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!preventDefault()) {
                    GUIComponent.getInstance().removeStage(SlotSaves.this);
                }
                backButton.setChecked(false);
            }
        });

        // Кнопка Далее с проверкой на валидность выбора
        nextButton = createButton("Далее", VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        // В методе createButtons(), в обработчике nextButton:
        nextButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (selectedSlotIndex == -1) return;

                LoaderComponent loaderComponent = new LoaderComponent();
                LevelData levelData = loaderComponent.loadMetaData("slot" + selectedSlotIndex);
                boolean hasData = levelData != null;

                if (currentMode == Mode.SAVE) {
                    com.hexedrealms.screens.Level levelInstance = com.hexedrealms.screens.Level.getInstance();
                    levelInstance.slotId = selectedSlotIndex;
                    loaderComponent.saveGame("slot" + levelInstance.slotId);

                    // Обновляем данные всех слотов после сохранения
                    refreshSlotData();

                    levelInstance.getPlayer().getHUD().setMessage("Сохранение создано", Color.WHITE);
                    GUIComponent.getInstance().removeStage(SlotSaves.this);
                }
                else if (currentMode == Mode.LOAD && hasData) {
                    if(!isMainMenu) {
                        com.hexedrealms.screens.Level.getInstance().dispose();
                    }
                    LoaderStage stage = new LoaderStage("textures/backgrounds/e1m1.png", selectedSlotIndex, 0, false);
                    Main.getInstance().setScreen(stage);
                    isMainMenu = false;
                }
                else if (currentMode == null) {
                    DifficultySelectionScreen difficultySelectionScreen = (DifficultySelectionScreen) GUIComponent.getInstance().getMenuStage(DifficultySelectionScreen.class);
                    difficultySelectionScreen.setSelectedSlotIndex(selectedSlotIndex);
                    GUIComponent.getInstance().putStage(difficultySelectionScreen);
                    GUIComponent.getInstance().removeStage(SlotSaves.this);
                }

                nextButton.setChecked(false);
            }
        });

        bottomGroup.addActor(backButton);
        bottomGroup.addActor(nextButton);
        bottomGroup.center();
    }

    private void refreshSlotData() {
        LoaderComponent loaderComponent = new LoaderComponent();
        for (int i = 0; i < slotButtons.size; i++) {
            String date = loaderComponent.getFormattedSaveDate("slot"+i);
            LevelData levelData = loaderComponent.loadMetaData("slot"+i);
            Level level = null;

            if(levelData != null) {
                level = ResourcesLoader.getLevel(levelData.levelID);
            }

            VisImageTextButton button = slotButtons.get(i);
            button.setText(level == null ? "Пусто" : level.name+"\n"+date);
        }
    }

    private void showConfirmationDialog(int slotIndex, VisImageTextButton button) {
        DialogStage dialogStage = (DialogStage) GUIComponent.getInstance().getMenuStage(DialogStage.class);
        dialogStage.setText("Вы уверены?\nЭтот слот уже содержит сохранение.\nПерезаписать?");
        dialogStage.menuStage = this;
        GUIComponent.getInstance().putStage(dialogStage);

        dialogStage.apply.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                // Если пользователь подтвердил, выбираем слот
                selectSlot(slotIndex, button);
                GUIComponent.getInstance().removeStage(dialogStage);
            }
        });

        dialogStage.cancel.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                // Если пользователь отменил, снимаем выделение
                button.setChecked(false);
                GUIComponent.getInstance().removeStage(dialogStage);
            }
        });
    }

    private void selectSlot(int slotIndex, VisImageTextButton button) {
        // Сбрасываем выделение у всех кнопок
        for (VisImageTextButton btn : slotButtons) {
            btn.setChecked(false);
        }
        // Выделяем текущую кнопку
        button.setChecked(true);
        selectedSlotIndex = slotIndex;
    }

    private void addButtonsToGrid() {
        buttonsGrid.clear();
        int buttonIndex = 0;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                if (buttonIndex < slotButtons.size) {
                    buttonsGrid.add(slotButtons.get(buttonIndex))
                        .width(250).height(140);
                    buttonIndex++;
                }
            }
            buttonsGrid.row();
        }

        verticalGroup.addActor(buttonsGrid);
    }

    protected void addButtonsToGroup() {
        horizontalGroup.clear();
        for (VisImageTextButton button : buttons) {
            horizontalGroup.addActor(button);
        }

        updateGroupLayout();
        verticalGroup.addActor(horizontalGroup);
        verticalGroup.addActor(bottomGroup);
        addActor(verticalGroup);
    }

    protected VisImageTextButton createButton(String text, VisImageTextButton.VisImageTextButtonStyle style) {
        VisImageTextButton button = new VisImageTextButton(text, style);
        button.getLabel().setAlignment(Align.center);
        button.padBottom(20);
        return button;
    }

    protected void calcVerticalGroupSize() {
        width = getViewport().getWorldWidth() * 0.40f;
        height = getViewport().getWorldHeight();
        verticalGroup.setSize(width, height);
    }



    protected void updateGroupLayout() {
        calcVerticalGroupSize();
        verticalGroup.setPosition(getViewport().getWorldWidth() - width, 0);
        verticalGroup.space(height * 0.06f);
        verticalGroup.padTop(height * 0.05f);
        horizontalGroup.setSize(width, height);
        bottomGroup.setSize(width, height);
        bottomGroup.padTop(-height * 0.1f);
        verticalGroup.invalidateHierarchy();
    }

    @Override
    protected void handleInput() {
        super.handleInput();
    }
}
