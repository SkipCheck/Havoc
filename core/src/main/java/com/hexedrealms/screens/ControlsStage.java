package com.hexedrealms.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.PostProcessorComponent;
import com.hexedrealms.engine.SettingsComponent;
import com.hexedrealms.visuicomponents.CustomVerticalGroup;
import com.hexedrealms.visuicomponents.HoverListener;
import com.hexedrealms.visuicomponents.SliderActor;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;

import javax.naming.ldap.Control;

public class ControlsStage extends ModalStage{

    public static final String [] namesMouse = new String[] { "Left Mouse Button", "Right Mouse Button", "Mouse Wheel", "Mouse Wheel Down", "Mouse Wheel Up"};

    private Table tableKeys;
    private Array<Actor> first, second;
    private Array<KeyBinding> keyBindings;
    private SliderActor fov;
    private SliderActor sensivity;
    private float valueFOV;
    private boolean isListen;
    private int index;

    public ControlsStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);
        calcVerticalGroupSize();

        createKeyBindings();
        createButtons();
        createFirst();
        createSecond();
        addButtonsToGroup();
    }

    protected void createKeyBindings() {
        // Пример списка действий и их текущих клавиш (можно загружать из настроек)
        keyBindings.add(new KeyBinding("Движение вперед", ControlsConfiguration.MOVE_FORWARD));
        keyBindings.add(new KeyBinding("Движение назад", ControlsConfiguration.MOVE_BACKWARD));
        keyBindings.add(new KeyBinding("Движение влево", ControlsConfiguration.MOVE_LEFT));
        keyBindings.add(new KeyBinding("Движение вправо", ControlsConfiguration.MOVE_RIGHT));
        keyBindings.add(new KeyBinding("Прыжок", ControlsConfiguration.JUMP));
        keyBindings.add(new KeyBinding("Взаимодействие", ControlsConfiguration.EVENT));
        keyBindings.add(new KeyBinding("Атака", ControlsConfiguration.ATACK));
        keyBindings.add(new KeyBinding("Перезарядка", ControlsConfiguration.RELOAD));
        keyBindings.add(new KeyBinding("Быстрое оружие", ControlsConfiguration.FAST_GUN));
        keyBindings.add(new KeyBinding("Быстрое сохранение", ControlsConfiguration.FAST_SAVE));
        keyBindings.add(new KeyBinding("Быстрая загрузка", ControlsConfiguration.FAST_LOAD));
        valueFOV = (float) ControlsConfiguration.FOV.getValue();
    }

    protected void createSecond(){
        second = new Array<>();
        fov = new SliderActor("Угол обзора", 60F, 120F, 1F, (Float) ControlsConfiguration.FOV.getValue());
        fov.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = fov.getValue();
                fov.setText(String.format("%.0f", value));
                ControlsConfiguration.FOV.setValue(value);
                checkForChanges();
            }
        });

        fov.setText(String.format("%.0f", (Float) ControlsConfiguration.FOV.getValue()));
        sensivity = new SliderActor("Чувствительность мыши", 0f, 1f, 0.01f, (Float) ControlsConfiguration.MOUSE_SENSIVITY.getValue());
        sensivity.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                checkForChanges();
            }
        });

        second.add(fov);
        second.add(sensivity);
    }

    protected void createFirst() {
        first = new Array<>();
        tableKeys = new Table(VisUI.getSkin());
        tableKeys.defaults().pad(2).fillX();

        // Создаем белый Drawable программно
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        Drawable whiteDrawable = new TextureRegionDrawable(new TextureRegion(texture));

        // Заголовки таблицы
        VisLabel actionLabel = new VisLabel("Действие");
        VisLabel keyLabel = new VisLabel("Назначение");
        keyLabel.setAlignment(Align.center);

        // Добавляем заголовки
        tableKeys.add(actionLabel).width(300);
        tableKeys.add(keyLabel).width(300);
        tableKeys.row();

        // Добавляем разделительную линию
        Table divider = new Table();
        divider.setBackground(whiteDrawable);
        tableKeys.add(divider).colspan(2).height(1).fillX().padTop(2).padBottom(4);
        tableKeys.row();

        // Заполняем таблицу привязками
        for (KeyBinding binding : keyBindings) {
            tableKeys.add(new VisLabel(binding.getAction())).left();

            VisTextButton keyButton = new VisTextButton(binding.getKey() < 5 ? namesMouse[binding.getKey()] : Input.Keys.toString(binding.getKey()));
            binding.putButton(keyButton);
            keyButton.addListener(new HoverListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    keyButton.setChecked(false);

                    if(isListen && keyButton.isDisabled() || isListen && index == 1){
                        return;
                    }else if(!isListen && keyButton.isDisabled()){
                        keyButton.setDisabled(false);
                        index = 0;
                        return;
                    }else if(index == 1){
                        index = 0;
                        return;
                    }

                    // Ожидание нового ввода клавиши
                    keyButton.setText("Нажмите клавишу...");
                    keyButton.setChecked(true);
                    keyButton.setDisabled(true);
                    isListen = true;
                    index = 1;

                    Stage stage = keyButton.getStage();
                    if (stage != null) {
                        stage.setKeyboardFocus(keyButton);
                    }

                    // Ловим новую клавишу
                    InputListener keyListener = new InputListener() {
                        @Override
                        public boolean keyDown(InputEvent event, int keycode) {
                            processingBinding(keycode, binding);
                            binding.setKey(keycode);
                            keyButton.setText(Input.Keys.toString(keycode));
                            keyButton.getStage().removeListener(this);
                            keyButton.setChecked(false);
                            applyButton.setDisabled(false);
                            checkForChanges();
                            isListen = false;
                            index = 0;
                            return true;
                        }

                        @Override
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                            // Обработка кнопок мыши
                            if (button == Input.Buttons.LEFT || button == Input.Buttons.RIGHT) {
                                processingBinding(button, binding);
                                binding.setKey(button);
                                keyButton.setText(namesMouse[binding.getKey()]);
                                keyButton.getStage().removeListener(this);
                                applyButton.setDisabled(false);
                                keyButton.setChecked(false);
                                checkForChanges();
                                isListen = false;
                                return true;
                            }
                            return false;
                        }
                    };

                    keyButton.getStage().addListener(keyListener);
                }
            });

            tableKeys.add(keyButton).right();
            tableKeys.row();
        }

        first.add(tableKeys);

        events.center();
        for(Actor actor : first)
            events.addActor(actor);
    }

    public void processingBinding(int keycode, KeyBinding keyBinding){
        for(KeyBinding key : keyBindings){
            if(key.getKey() == keycode){
                key.setKey(keyBinding.getKey());
                key.getTextButton().setText(key.getKey() < 5 ? namesMouse[key.getKey()] : Input.Keys.toString(key.getKey()));
            }
        }
    }

    protected void initComponents() {
        buttons = new Array<>();
        keyBindings = new Array<>();

        verticalGroup = new CustomVerticalGroup(VisUI.getSkin().get("modal", Window.WindowStyle.class).background);
        buttonsGroup = new VerticalGroup();
        horizontalGroup = new HorizontalGroup().space(55);
        horizontalGroup.center();
        bottomGroup = new HorizontalGroup().space(55);
        events = new VerticalGroup().fill().left().padLeft(20).padTop(-30);
    }

    @Override
    public boolean preventDefault(){
        if(!applyButton.isDisabled()) {
            DialogStage dialogStage = (DialogStage) GUIComponent.getInstance().getMenuStage(DialogStage.class);
            dialogStage.setText("Изменения не сохранены.\nВы уверены?");
            dialogStage.menuStage = ControlsStage.this;
            GUIComponent.getInstance().putStage(dialogStage);

            dialogStage.apply.addListener(new HoverListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);

                    revertToInitialSettings();

                    GUIComponent.getInstance().removeStage(dialogStage);
                    GUIComponent.getInstance().removeStage(ControlsStage.this);
                    applyButton.setDisabled(true);
                }
            });

            return true;
        }
        return false;
    }

    public void revertToInitialSettings() {
        // Восстанавливаем начальные значения для всех привязок клавиш
        for (KeyBinding binding : keyBindings) {
            // Восстанавливаем значение по умолчанию
            binding.setKey(binding.getCurrentKey());

            // Обновляем текст на соответствующей кнопке
            binding.getTextButton().setText(
                binding.getKey() < 5 ?
                    namesMouse[binding.getKey()] :
                    Input.Keys.toString(binding.getKey())
            );
        }

        ControlsConfiguration.FOV.setValue(ControlsConfiguration.FOV.getDefaultKey());
        ControlsConfiguration.MOUSE_SENSIVITY.setValue(ControlsConfiguration.MOUSE_SENSIVITY.getDefaultKey());

        fov.setValue((Float) ControlsConfiguration.FOV.getDefaultKey());
        sensivity.setValue((Float) ControlsConfiguration.MOUSE_SENSIVITY.getDefaultKey());

        // Применяем настройки (если нужно)
        SettingsComponent.getInstance().saveControls();

        // Деактивируем кнопку "Применить", так как вернули исходные настройки
        applyButton.setDisabled(true);

        // Обновляем состояние кнопки "Применить" (хотя она уже деактивирована)
        checkForChanges();
    }

    protected void createButtons() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("modal", VisImageTextButton.VisImageTextButtonStyle.class);

        VisImageTextButton inputButton = createButton("Клавиатура и мышь", style);
        inputButton.getLabelCell().padBottom(15);
        inputButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(isListen) return;
                super.clicked(event, x, y);
                events.clear();
                for(Actor actor : first)
                    events.addActor(actor);
            }
        });

        VisImageTextButton reviewButton = createButton("Настройка обзора", style);
        reviewButton.getLabelCell().padBottom(15);
        reviewButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(isListen) return;
                super.clicked(event, x, y);
                events.clear();
                for(Actor actor : second)
                    events.addActor(actor);
            }
        });

        buttons.add(inputButton);
        buttons.add(reviewButton);

        // Back Button
        VisImageTextButton.VisImageTextButtonStyle backStyle = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle applyStyle = VisUI.getSkin().get("apply", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton backButton = createButton("Назад", backStyle);
        backButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(isListen) return;
                super.clicked(event, x, y);
                if(!preventDefault()) GUIComponent.getInstance().removeStage(ControlsStage.this);
                backButton.setChecked(false);
            }
        });

        VisImageTextButton defaultButton = createButton("По умолчанию", applyStyle);
        defaultButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(isListen) return;
                super.clicked(event, x, y);
                // Сброс всех значений к значениям по умолчанию
                for (KeyBinding binding : keyBindings) {
                    binding.setKey(binding.getDefaultKey());
                    binding.getTextButton().setText(
                        binding.getKey() < 5 ?
                            namesMouse[binding.getKey()] :
                            Input.Keys.toString(binding.getKey())
                    );
                }

                ControlsConfiguration.FOV.setValue(ControlsConfiguration.FOV.getDefaultKey());
                ControlsConfiguration.MOUSE_SENSIVITY.setValue(ControlsConfiguration.MOUSE_SENSIVITY.getDefaultKey());

                fov.setValue((Float) ControlsConfiguration.FOV.getDefaultKey());
                sensivity.setValue((Float) ControlsConfiguration.MOUSE_SENSIVITY.getDefaultKey());

                SettingsComponent.getInstance().saveControls();
                applyButton.setDisabled(true); // После сброса к значениям по умолчанию кнопка "Применить" отключается
                defaultButton.setChecked(false);
            }
        });

        applyButton = createButton("Применить", applyStyle);
        applyButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(isListen) return;
                if(applyButton.isDisabled()) return;
                super.clicked(event, x, y);

                valueFOV = (float) ControlsConfiguration.FOV.getValue();
                ControlsConfiguration.MOUSE_SENSIVITY.setValue(sensivity.getValue());

                SettingsComponent.getInstance().saveControls();
                applyButton.setDisabled(true);
            }
        });
        applyButton.setDisabled(true);

        bottomGroup.addActor(backButton);
        bottomGroup.addActor(defaultButton);
        bottomGroup.addActor(applyButton);
        bottomGroup.space(5);

        bottomGroup.center();
        setFocusOnButton(buttons.get(0));
        setupButtonListeners();
    }

    protected void updateGroupLayout() {
        calcVerticalGroupSize();

        verticalGroup.setPosition(getViewport().getWorldWidth() - width, 0);
        verticalGroup.space(height * 0.06f);
        verticalGroup.padTop(height * 0.05f);

        horizontalGroup.setSize(width, height);
        bottomGroup.setSize(width, height);
        buttonsGroup.setSize(width, height);

        events.setWidth(width * 0.75f);

        fov.setWidth(events.getWidth());
        sensivity.setWidth(events.getWidth());

        updateCheckBoxStyle();
        verticalGroup.invalidateHierarchy();
        verticalGroup.layout();
        events.invalidateHierarchy();
        events.layout();
    }

    private void checkForChanges() {
        boolean hasChanges = valueFOV != (Float) ControlsConfiguration.FOV.getValue() || sensivity.getValue() != (Float) ControlsConfiguration.MOUSE_SENSIVITY.getValue();

        for (KeyBinding binding : keyBindings) {
            if (binding.getKey() != binding.getCurrentKey()) {
                hasChanges = true;
                break;
            }
        }

        applyButton.setDisabled(!hasChanges);
    }

    private static class KeyBinding {
        private String action;
        private ControlsConfiguration control;
        private int defaultKey;
        private int currentKey;
        private VisTextButton textButton;

        public KeyBinding(String action, ControlsConfiguration control) {
            this.action = action;
            this.control = control;
            this.defaultKey = (Integer) control.getDefaultKey();
            this.currentKey = (Integer) control.getValue();
        }

        public void putButton(VisTextButton button){ this.textButton = button; }
        public VisTextButton getTextButton(){ return textButton;}
        public String getAction() { return action; }
        public int getKey() { return (Integer) control.getValue(); }
        public int getDefaultKey() { return defaultKey; }
        public int getCurrentKey() { return currentKey; }
        public void setKey(int key) {  control.setValue(key); }
    }
}
