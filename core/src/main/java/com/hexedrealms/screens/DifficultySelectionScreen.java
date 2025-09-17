package com.hexedrealms.screens;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.Main;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.ResourcesLoader;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisTable;

public class DifficultySelectionScreen extends ModalStage {

    private int selectedDifficultyIndex = -1;
    private boolean processingSelection = false;
    private int selectedSlotIndex;

    public DifficultySelectionScreen(Viewport viewport, String text) {
        super(viewport, text);
    }

    public void setSelectedSlotIndex(int selectedSlotIndex){
        this.selectedSlotIndex = selectedSlotIndex;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
    }

    @Override
    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);
        calcVerticalGroupSize();

        createDifficultyButtons();
        addButtonsToGroup();
    }

    private void createDifficultyButtons() {
        String[] difficultyNames = {
            "Новичок",
            "Ветеран",
            "Элита",
            "Легенда"
        };

        Table difficultyTable = new VisTable();
        difficultyTable.defaults().pad(15).width(350).height(60);

        for (int i = 0; i < difficultyNames.length; i++) {
            VisImageTextButton button = new VisImageTextButton(
                difficultyNames[i],
                VisUI.getSkin().get("difficulty", VisImageTextButton.VisImageTextButtonStyle.class)
            );
            button.getLabel().setFontScale(1.1f);
            button.getLabel().setAlignment(Align.center);

            final int index = i;
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (processingSelection) return;

                    processingSelection = true;
                    try {
                        // Сбрасываем выделение у всех кнопок
                        for (Actor child : difficultyTable.getChildren()) {
                            if (child instanceof VisImageTextButton) {
                                ((VisImageTextButton) child).setChecked(false);
                            }
                        }
                        // Выделяем текущую кнопку
                        button.setChecked(true);
                        selectedDifficultyIndex = index;
                    } finally {
                        processingSelection = false;
                    }
                }
            });

            difficultyTable.add(button).row();
        }

        verticalGroup.addActor(difficultyTable);

        // Кнопка Назад
        VisImageTextButton backButton = createButton("Назад",
            VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        backButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                GUIComponent.getInstance().removeStage(DifficultySelectionScreen.this);
                MenuStage savesMenu = GUIComponent.getInstance().getMenuStage(SlotSaves.class);
                GUIComponent.getInstance().putStage(savesMenu);
                backButton.setChecked(false);
            }
        });

        // Кнопка Начать
        VisImageTextButton startButton = createButton("Начать",
            VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class));
        startButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (selectedDifficultyIndex != -1) {
                    GUIComponent.getInstance().removeStage(DifficultySelectionScreen.this);

                    LoaderStage loaderStage = new LoaderStage("textures/backgrounds/e1m1.png", selectedSlotIndex, selectedDifficultyIndex, false);
                    loaderStage.putLevelData(ResourcesLoader.getLevel(0));
                    loaderStage.setFirstLaunch();
                    loaderStage.setLevelId(0);
                    Main.getInstance().setScreen(loaderStage);
                }

                startButton.setChecked(false);
            }
        });

        bottomGroup.addActor(backButton);
        bottomGroup.addActor(startButton);
        bottomGroup.center();
    }

    // Остальные методы остаются такими же, как в SlotSaves
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

    protected void addButtonsToGroup() {
        updateGroupLayout();
        verticalGroup.addActor(horizontalGroup);
        verticalGroup.addActor(bottomGroup);
        addActor(verticalGroup);
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
        verticalGroup.layout();
    }
}
