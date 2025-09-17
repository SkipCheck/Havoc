package com.hexedrealms.screens;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.configurations.GraphicConfiguration;
import com.hexedrealms.configurations.PostProcessorConfiguration;
import com.hexedrealms.configurations.Quality;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.PostProcessorComponent;
import com.hexedrealms.engine.SettingsComponent;
import com.hexedrealms.visuicomponents.CustomVerticalGroup;
import com.hexedrealms.visuicomponents.HoverListener;
import com.hexedrealms.visuicomponents.ScrollActor;
import com.hexedrealms.visuicomponents.SliderActor;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisImageTextButton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class ModalStage extends MenuStage {
    // UI Groups
    protected HorizontalGroup horizontalGroup;
    protected HorizontalGroup bottomGroup;
    protected VerticalGroup events;

    // UI Components
    private Array<Actor> graphicsComponents;
    private Array<Actor> postProcessorComponents;
    private SliderActor qualityActor;
    private ScrollActor resolutionActor;
    private ScrollActor frequencyActor;
    private ScrollActor fpsActor;
    private SliderActor gammaActor;
    private VisCheckBox bloomButton;
    private VisCheckBox blurButton;
    private VisCheckBox vsyncButton;
    protected VisImageTextButton applyButton;

    private int initialResolutionIndex;
    private int initialFrequencyIndex;
    private int initialFpsIndex;
    private int initialQualityValue;
    private float initialBrightness;
    private boolean initialVsync;
    private boolean initialBloom;
    private boolean initialBlur;

    protected float width, height;

    // Data
    private LinkedHashMap<String, Quality> qualityMap;

    public ModalStage(Viewport viewport, String text) {
        super(viewport, text);
    }

    @Override
    protected void initialize() {
        initQualityMap();
        initComponents();
        saveInitialValues();
        setupUI();
    }

    private void saveInitialValues() {
        initialResolutionIndex = (Integer) GraphicConfiguration.DIMENSION.getValue();
        initialFrequencyIndex = (Integer) GraphicConfiguration.FREQUENCY.getValue();
        initialFpsIndex = (Integer) GraphicConfiguration.FPS.getValue();
        initialQualityValue = (Integer) PostProcessorConfiguration.QUALITY.getValue();
        initialBrightness = (Float) PostProcessorConfiguration.BRIGHTNESS.getValue();
        initialVsync = (Boolean) GraphicConfiguration.VSYNC.getValue();
        initialBloom = (Boolean) PostProcessorConfiguration.BLOOM.getValue();
        initialBlur = (Boolean) PostProcessorConfiguration.MOTION_BLUR.getValue();
    }

    private boolean hasChanges() {
        return resolutionActor.getCurrentIndex() != initialResolutionIndex ||
            frequencyActor.getCurrentIndex() != initialFrequencyIndex ||
            fpsActor.getCurrentIndex() != initialFpsIndex ||
            (int)qualityActor.getValue() != initialQualityValue ||
            gammaActor.getValue() != initialBrightness ||
            vsyncButton.isChecked() != initialVsync ||
            bloomButton.isChecked() != initialBloom ||
            blurButton.isChecked() != initialBlur;
    }

    private void updateApplyButtonState() {
        applyButton.setDisabled(!hasChanges());
    }


    private void initQualityMap() {
        qualityMap = new LinkedHashMap<>();
        qualityMap.put("Низкое", Quality.LOW_QUALITY);
        qualityMap.put("Среднее", Quality.MIDDLE_QUALITY);
        qualityMap.put("Высокое", Quality.HIGH_QUALITY);
    }

    protected void initComponents() {
        buttons = new Array<>();
        graphicsComponents = new Array<>();
        postProcessorComponents = new Array<>();

        verticalGroup = new CustomVerticalGroup(VisUI.getSkin().get("modal", Window.WindowStyle.class).background);
        buttonsGroup = new VerticalGroup();
        horizontalGroup = new HorizontalGroup().space(55);
        bottomGroup = new HorizontalGroup().space(55);
        events = new VerticalGroup().fill().left().padLeft(20).padTop(-30);
        events.center();

    }

    protected void setupUI() {
        verticalGroup.fill();
        verticalGroup.addActor(label);
        calcVerticalGroupSize();

        createButtons();
        createGraphicsComponents();
        createPostProcessorComponents();
        addButtonsToGroup();
    }

    private void createGraphicsComponents() {
        // VSync Checkbox
        vsyncButton = createCheckbox("Верт. синхронизация", (Boolean) GraphicConfiguration.VSYNC.getValue());
        vsyncButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                GraphicConfiguration.VSYNC.setValue(vsyncButton.isChecked());
                fpsActor.setVisible(!vsyncButton.isChecked());
                updateApplyButtonState();
            }
        });


        // Resolution options
        List<Graphics.DisplayMode> modes = SettingsComponent.getInstance().displayModes;
        Set<Integer> refreshRates = SettingsComponent.getInstance().refreshRates;

        String[] resolutionKeys = new String[modes.size()];
        String[] frequencyKeys = new String[refreshRates.size()];
        String[] defaultRefresh = new String[SettingsComponent.DEFAULT_REFRESH_RATES.length];

        populateResolutionOptions(modes, resolutionKeys);
        populateFrequencyOptions(refreshRates, frequencyKeys);
        populateDefaultRefreshOptions(defaultRefresh);

        resolutionActor = new ScrollActor("Разрешение", resolutionKeys, (Integer) GraphicConfiguration.DIMENSION.getValue());
        frequencyActor = new ScrollActor("Частота\nобновления", frequencyKeys, (Integer) GraphicConfiguration.FREQUENCY.getValue());
        fpsActor = new ScrollActor("Лимит FPS", defaultRefresh, (Integer) GraphicConfiguration.FPS.getValue());
        fpsActor.setVisible(!vsyncButton.isChecked());

        resolutionActor.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                updateApplyButtonState();
            }
        });

        frequencyActor.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                updateApplyButtonState();
            }
        });

        fpsActor.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                updateApplyButtonState();
            }
        });


        graphicsComponents.add(resolutionActor);
        graphicsComponents.add(frequencyActor);
        graphicsComponents.add(vsyncButton);
        graphicsComponents.add(fpsActor);

        for (Actor actor : graphicsComponents) {
            events.addActor(actor);
        }
    }

    private void populateResolutionOptions(List<Graphics.DisplayMode> modes, String[] resolutionKeys) {
        for (int i = 0; i < modes.size(); i++) {
            resolutionKeys[i] = modes.get(i).width + " x " + modes.get(i).height;
        }
    }

    private void populateFrequencyOptions(Set<Integer> refreshRates, String[] frequencyKeys) {
        int index = 0;
        for (Integer frequency : refreshRates) {
            frequencyKeys[index++] = frequency + "hz";
        }
    }

    private void populateDefaultRefreshOptions(String[] defaultRefresh) {
        for (int i = 0; i < SettingsComponent.DEFAULT_REFRESH_RATES.length; i++) {
            defaultRefresh[i] = String.valueOf(SettingsComponent.DEFAULT_REFRESH_RATES[i]);
        }
    }

    private void createPostProcessorComponents() {

        bloomButton = createCheckbox("Эффект свечения", (Boolean) PostProcessorConfiguration.BLOOM.getValue());
        bloomButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                PostProcessorConfiguration.BLOOM.setValue(bloomButton.isChecked());
                PostProcessorComponent.getInstance().enableBloom(bloomButton.isChecked());
                updateApplyButtonState();
            }
        });
        bloomButton.padTop(7);

        blurButton = createCheckbox("Размытие при движении",  (Boolean) PostProcessorConfiguration.MOTION_BLUR.getValue());
        blurButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                PostProcessorConfiguration.MOTION_BLUR.setValue(blurButton.isChecked());
                PostProcessorComponent.getInstance().enableMotionBlur(blurButton.isChecked());
                updateApplyButtonState();
            }
        });

        gammaActor = new SliderActor("Яркость", -0.5f,1f,0.01f, (Float) PostProcessorConfiguration.BRIGHTNESS.getValue());
        gammaActor.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = gammaActor.getValue();
                PostProcessorComponent.getInstance().applyBrightness(value);
                updateApplyButtonState();
            }
        });

        String[] qualityKeys = qualityMap.keySet().toArray(new String[0]);

        qualityActor = new SliderActor("Качество", 0,2, 1, (Integer) PostProcessorConfiguration.QUALITY.getValue());
        qualityActor.putInterpreted(qualityKeys);
        qualityActor.slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateApplyButtonState();
            }
        });

        postProcessorComponents.add(qualityActor);
        postProcessorComponents.add(gammaActor);
        postProcessorComponents.add(bloomButton);
        postProcessorComponents.add(blurButton);
    }

    @Override
    public boolean preventDefault() {
        if(!applyButton.isDisabled()) {
            DialogStage dialogStage = (DialogStage) GUIComponent.getInstance().getMenuStage(DialogStage.class);
            dialogStage.setText("Изменения не сохранены.\nВы уверены?");
            dialogStage.menuStage = ModalStage.this;
            GUIComponent.getInstance().putStage(dialogStage);

            dialogStage.apply.addListener(new HoverListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);

                    revertToInitialSettings();

                    GUIComponent.getInstance().removeStage(dialogStage);
                    GUIComponent.getInstance().removeStage(ModalStage.this);
                }
            });
            return true;
        }
        return false;
    }

    protected void createButtons() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("modal", VisImageTextButton.VisImageTextButtonStyle.class);

        // Graphics Button
        VisImageTextButton graphicsButton = createButton("Настройки\n графики", style);
        graphicsButton.getLabelCell().padBottom(15);
        graphicsButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                showComponents(graphicsComponents);
            }
        });

        // Post-Processor Button
        VisImageTextButton postProcessorButton = createButton("Качество и\n постобработка", style);
        postProcessorButton.getLabelCell().padBottom(15);
        postProcessorButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                showComponents(postProcessorComponents);
            }
        });

        buttons.add(graphicsButton);
        buttons.add(postProcessorButton);

        // Back Button
        VisImageTextButton.VisImageTextButtonStyle backStyle = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle applyStyle = VisUI.getSkin().get("apply", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton backButton = createButton("Назад", backStyle);
        backButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if(!preventDefault()) GUIComponent.getInstance().removeStage(ModalStage.this);
                backButton.setChecked(false);
            }
        });

        applyButton = createButton("Применить", applyStyle);
        applyButton.addListener(new HoverListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(applyButton.isDisabled()) return;
                super.clicked(event, x, y);
                SettingsComponent.getInstance().applyGraphicSettings(resolutionActor.getCurrentIndex(), frequencyActor.getCurrentIndex(),
                                                                     fpsActor.getCurrentIndex(), (int) qualityActor.getValue(), gammaActor.getValue(),
                                                                     vsyncButton.isChecked(), bloomButton.isChecked(), blurButton.isChecked());
                saveInitialValues();
                applyButton.setDisabled(true);
            }
        });
        applyButton.setDisabled(true);

        bottomGroup.addActor(backButton);
        bottomGroup.addActor(applyButton);
        bottomGroup.center();
        setFocusOnButton(buttons.get(0));

        setupButtonListeners();
    }

    public void revertToInitialSettings() {
        // Восстанавливаем начальные значения
        GraphicConfiguration.DIMENSION.setValue(initialResolutionIndex);
        GraphicConfiguration.FREQUENCY.setValue(initialFrequencyIndex);
        GraphicConfiguration.FPS.setValue(initialFpsIndex);
        GraphicConfiguration.VSYNC.setValue(initialVsync);
        PostProcessorConfiguration.QUALITY.setValue(initialQualityValue);
        PostProcessorConfiguration.BRIGHTNESS.setValue(initialBrightness);
        PostProcessorConfiguration.BLOOM.setValue(initialBloom);
        PostProcessorConfiguration.MOTION_BLUR.setValue(initialBlur);

        // Обновляем UI
        resolutionActor.setCurrentIndex(initialResolutionIndex);
        frequencyActor.setCurrentIndex(initialFrequencyIndex);
        fpsActor.setCurrentIndex(initialFpsIndex);
        vsyncButton.setChecked(initialVsync);
        qualityActor.setValue(initialQualityValue);
        gammaActor.setValue(initialBrightness);
        bloomButton.setChecked(initialBloom);
        blurButton.setChecked(initialBlur);
        fpsActor.setVisible(!initialVsync);

        // Применяем настройки (если нужно)
        SettingsComponent.getInstance().applyGraphicSettings(
            initialResolutionIndex,
            initialFrequencyIndex,
            initialFpsIndex,
            initialQualityValue,
            initialBrightness,
            initialVsync,
            initialBloom,
            initialBlur
        );

        applyButton.setDisabled(true);

        // Обновляем состояние кнопки "Применить"
        updateApplyButtonState();
    }

    private void showComponents(Array<Actor> components) {
        events.clear();
        for (Actor actor : components) {
            events.addActor(actor);
        }
    }

    protected void addButtonsToGroup() {
        clear();
        horizontalGroup.clear();

        for (VisImageTextButton button : buttons) {
            horizontalGroup.addActor(button);
        }

        updateGroupLayout();

        verticalGroup.addActor(horizontalGroup);
        verticalGroup.addActor(events);
        verticalGroup.addActor(bottomGroup);

        addActor(verticalGroup);
    }

    protected void setupButtonListeners() {
        for (VisImageTextButton button : buttons) {
            button.addListener(new HoverListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    index = buttons.indexOf(button, true);
                }
            });
        }
    }

    protected void updateButtonStyle() {
        VisImageTextButton.VisImageTextButtonStyle style = VisUI.getSkin().get("modal", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle back = VisUI.getSkin().get("back", VisImageTextButton.VisImageTextButtonStyle.class);
        VisImageTextButton.VisImageTextButtonStyle apply = VisUI.getSkin().get("apply", VisImageTextButton.VisImageTextButtonStyle.class);

        float width = horizontalGroup.getWidth() * 0.35f;
        float height = horizontalGroup.getHeight() * 0.12f;
        float backWidth = bottomGroup.getWidth() * 0.3f;
        float backHeight = bottomGroup.getHeight() * 0.08f;
        float applyWidth = bottomGroup.getWidth() * 0.3f;
        float applyHeight = bottomGroup.getHeight() * 0.08f;  // Исправлено: было getWidth()

        style.up.setMinWidth(width);
        style.up.setMinHeight(height);
        back.up.setMinWidth(backWidth);
        back.up.setMinHeight(backHeight);
        apply.up.setMinWidth(applyWidth);    // Исправлено: было setMinHeight
        apply.up.setMinHeight(applyHeight);

        updateTextureFilters(style, back, apply);
    }

    void updateTextureFilters(VisImageTextButton.VisImageTextButtonStyle style,
                              VisImageTextButton.VisImageTextButtonStyle back,
                              VisImageTextButton.VisImageTextButtonStyle apply) {
        updateTextureFilter(style.up);
        updateTextureFilter(style.down);
        updateTextureFilter(back.up);
        updateTextureFilter(back.down);
        updateTextureFilter(apply.up);
        updateTextureFilter(apply.down);
    }

    protected void updateCheckBoxStyle() {
        VisCheckBox.VisCheckBoxStyle checkBoxStyle = VisUI.getSkin().get("default", VisCheckBox.VisCheckBoxStyle.class);
        float width = verticalGroup.getWidth() * 0.04f;

        updateCheckBoxPartStyle(checkBoxStyle.checkBackground, width);
        updateCheckBoxPartStyle(checkBoxStyle.checkBackgroundOver, width);
        updateCheckBoxPartStyle(checkBoxStyle.checkBackgroundDown, width);
        updateCheckBoxPartStyle(checkBoxStyle.tick, width);
    }

    private void updateCheckBoxPartStyle(com.badlogic.gdx.scenes.scene2d.utils.Drawable drawable, float width) {
        if (drawable != null) {
            drawable.setMinWidth(width);
            drawable.setMinHeight(width);
            updateTextureFilter(drawable);
        }
    }

    protected VisCheckBox createCheckbox(String text, boolean check) {
        updateCheckBoxStyle();

        VisCheckBox checkBox = new VisCheckBox(text, check);
        checkBox.left();
        checkBox.padBottom(20);
        checkBox.getLabel().setText(" " + text);
        return checkBox;

    }

    protected VisCheckBox createCheckbox(String text, VisCheckBox.VisCheckBoxStyle style) {
        updateCheckBoxStyle();

        VisCheckBox checkBox = new VisCheckBox(text, style);
        checkBox.left();
        checkBox.padBottom(20);
        checkBox.getLabel().setFontScale(1.05f);
        checkBox.getLabel().setText(" " + text);
        return checkBox;
    }

    protected VisImageTextButton createButton(String text, VisImageTextButton.VisImageTextButtonStyle style) {
        VisImageTextButton button = new VisImageTextButton(text, style);
        button.getLabel().setAlignment(Align.center);
        button.padBottom(20);
        return button;
    }

    protected void calcVerticalGroupSize(){
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
        buttonsGroup.setSize(width, height);

        events.setWidth(width * 0.75f);

        // Update scroll actors width
        qualityActor.setWidth(events.getWidth());
        resolutionActor.setWidth(events.getWidth());
        frequencyActor.setWidth(events.getWidth());
        fpsActor.setWidth(events.getWidth());
        gammaActor.setWidth(events.getWidth());

        updateCheckBoxStyle();
        verticalGroup.invalidateHierarchy();
        verticalGroup.layout();
        events.invalidateHierarchy();
        events.layout();
    }

    @Override
    protected void handleInput() {
        super.handleInput();
    }
}
