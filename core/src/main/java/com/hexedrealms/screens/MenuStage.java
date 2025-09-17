package com.hexedrealms.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.esotericsoftware.spine.Slot;
import com.hexedrealms.Main;
import com.hexedrealms.components.CloudsBatch;
import com.hexedrealms.engine.EntityComponent;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.LoaderComponent;
import com.hexedrealms.engine.ParticlesComponent;
import com.hexedrealms.visuicomponents.CustomVerticalGroup;
import com.hexedrealms.visuicomponents.HoverListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisLabel;

public class MenuStage extends Stage {
    protected VisLabel label;
    protected CustomVerticalGroup verticalGroup;
    protected VerticalGroup buttonsGroup;
    protected Array<VisImageTextButton> buttons;
    protected int index, previousIndex;

    public MenuStage(Viewport viewport, String text) {
        super(viewport);
        label = new VisLabel();
        label.setText(text);
        label.setAlignment(Align.center);
        label.setColor(0.51f, 0.71f, 0.9f, 1f);
        initialize();
    }

    protected void initialize() {
        buttons = new Array<>();
        verticalGroup = new CustomVerticalGroup(VisUI.getSkin().get("default", List.ListStyle.class).background);
        buttonsGroup = new VerticalGroup();

        verticalGroup
            .fill()
            .padLeft(20);

        buttonsGroup
            .center()
            .space(20);

        verticalGroup.addActor(label);
        createButtons();
        addButtonsToGroup();
    }

    protected void createButtons() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("default", VisImageTextButton.VisImageTextButtonStyle.class);


        VisImageTextButton settingsButton = createButton("Настройки", style);
        settingsButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                MenuStage settingsMenu = GUIComponent.getInstance().getMenuStage(SettingsStage.class);
                GUIComponent.getInstance().putStage(settingsMenu);
            }
        });

        VisImageTextButton exitButton = createButton("Выйти из игры", style);
        exitButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                Gdx.app.exit();
            }
        });

        VisImageTextButton startButton = createButton("Новая игра", style);
        startButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                SlotSaves savesMenu = (SlotSaves) GUIComponent.getInstance().getMenuStage(SlotSaves.class);
                savesMenu.setMode(null);
                GUIComponent.getInstance().putStage(savesMenu);
            }
        });

        VisImageTextButton continueButton = createButton("Продолжить", style);
        continueButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if(continueButton.isDisabled()) return;
                // Находим последнее сохранение
                SaveSlotInfo latestSave = findLatestSaveSlot();
                if (latestSave != null) {
                    // Запускаем LoaderStage с найденным слотом
                    LoaderStage loaderStage = new LoaderStage(
                        "textures/backgrounds/e1m1.png",
                        latestSave.slotNumber,
                        0, false
                    );
                    Main.getInstance().setScreen(loaderStage);
                }
            }
        });

        VisImageTextButton loadButton = createButton("Загрузить игру", style);
        loadButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                SlotSaves slotSaves = (SlotSaves) GUIComponent.getInstance().getMenuStage(SlotSaves.class);
                slotSaves.isMainMenu = true;
                slotSaves.setMode(SlotSaves.Mode.LOAD);
                GUIComponent.getInstance().putStage(slotSaves);
            }
        });

        SaveSlotInfo latestSave = findLatestSaveSlot();
        continueButton.setDisabled(latestSave == null);

        buttons.add(continueButton);
        buttons.add(startButton);
        buttons.add(loadButton);
        buttons.add(settingsButton);
        buttons.add(createButton("Гримуар нечисти", style));
        buttons.add(exitButton);

        setFocusOnButton(buttons.get(0));
    }

    private static class SaveSlotInfo {
        String slotName;
        int slotNumber;
        long timestamp;

        public SaveSlotInfo(String slotName, int slotNumber, long timestamp) {
            this.slotName = slotName;
            this.slotNumber = slotNumber;
            this.timestamp = timestamp;
        }
    }

    private SaveSlotInfo findLatestSaveSlot() {
        LoaderComponent loader = new LoaderComponent();
        SaveSlotInfo latestSave = null;

        // Проверяем все 8 возможных слотов
        for (int i = 0; i < 8; i++) {
            String slotName = "slot" + i;
            if (loader.saveExists(slotName)) {
                // Получаем время последнего изменения файла сохранения
                String savePath = LoaderComponent.SAVES_DIR + slotName + LoaderComponent.SAVE_EXTENSION;
                FileHandle saveFile = Gdx.files.local(savePath);
                long timestamp = saveFile.lastModified();

                // Если это первое найденное сохранение или более новое, чем текущее последнее
                if (latestSave == null || timestamp > latestSave.timestamp) {
                    latestSave = new SaveSlotInfo(slotName, i, timestamp);
                }
            }
        }

        return latestSave;
    }

    protected void setFocusOnButton(VisImageTextButton button) {
        this.setKeyboardFocus(button);
        button.setChecked(true);
    }

    protected void updateButtonStyle() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("default", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle back = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);

        float width = buttonsGroup.getWidth() * 0.85f;
        float height = buttonsGroup.getHeight() * 0.06f;

        float backWidth = buttonsGroup.getWidth() * 0.5f;
        float backHeight = buttonsGroup.getHeight() * 0.07f;

        style.up.setMinWidth(width);
        style.up.setMinHeight(height);
        back.up.setMinWidth(backWidth);
        back.up.setMinHeight(backHeight);

        updateTextureFilter(style.up);
        updateTextureFilter(style.down);
        updateTextureFilter(back.up);
        updateTextureFilter(back.down);
    }

    protected void updateTextureFilter(Drawable drawable) {
        if (drawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable textureRegionDrawable = (TextureRegionDrawable) drawable;
            Texture texture = textureRegionDrawable.getRegion().getTexture();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }

    protected VisImageTextButton createButton(String text, VisImageTextButton.VisImageTextButtonStyle style) {
        VisImageTextButton button = new VisImageTextButton(text, style);
        button.getLabelCell()
            .padBottom(10)
            .align(Align.center);

        return button;
    }

    protected void addButtonsToGroup() {
        clear();

        buttonsGroup.clear();
        for(VisImageTextButton button : buttons)
            buttonsGroup.addActor(button);

        updateGroupLayout();
        verticalGroup.addActor(buttonsGroup);

        addActor(verticalGroup);

        for (VisImageTextButton button : buttons) {
            button.addListener(new HoverListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    index = buttons.indexOf(button, true);
                }
            });
        }
    }

    protected void updateGroupLayout() {
        float width = getViewport().getWorldWidth() * 0.25f; // 25% от виртуальной ширины
        float height = getViewport().getWorldHeight(); // Полная виртуальная высота

        verticalGroup.setWidth(width);
        verticalGroup.setHeight(height);
        verticalGroup.setPosition(getViewport().getWorldWidth() - width, 0); // Позиция справа
        verticalGroup.space(height * 0.07f);
        verticalGroup.padTop(height * 0.035f);

        buttonsGroup.setWidth(width);
        buttonsGroup.setHeight(height);
    }

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        updateButtonStyle();
    }

    public boolean preventDefault(){
        return false;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        handleInput();
    }


    protected void handleInput() {
        if(buttons == null || buttons.size == 0) return;
        buttons.get(previousIndex).setChecked(false);
        previousIndex = index;

        boolean isHorizontal = buttons.get(index).getParent() instanceof HorizontalGroup;

        int buttonNext = isHorizontal ? Input.Keys.RIGHT : Input.Keys.DOWN;
        int buttonPrev = isHorizontal ? Input.Keys.LEFT : Input.Keys.UP;

        if (Gdx.input.isKeyJustPressed(buttonNext)) index++;
        if (Gdx.input.isKeyJustPressed(buttonPrev)) index--;

        if (index < 0) index = buttons.size - 1;
        if (index >= buttons.size) index = 0;

        buttons.get(index).setChecked(true);

        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || (isHorizontal && index != previousIndex)) {
            com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
            event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
            buttons.get(index).fire(event);

            event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
            event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchUp);
            buttons.get(index).fire(event);
        }
    }
}
