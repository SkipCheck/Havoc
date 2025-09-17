package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.hexedrealms.Main;
import com.hexedrealms.components.DisplayResolution;
import com.hexedrealms.configurations.*;
import com.hexedrealms.screens.Level;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsComponent {
    private static final String PREFS_NAME = "Havoc";
    private static SettingsComponent instance;

    public Preferences prefs;
    public List<Graphics.DisplayMode> displayModes;
    public Set<Integer> refreshRates;
    public static final int[] DEFAULT_REFRESH_RATES = {30, 60, 75, 90, 120, 144, 240, 300};

    public static SettingsComponent getInstance() {
        if (instance == null) {
            instance = new SettingsComponent();
        }
        return instance;
    }

    private SettingsComponent() {
        displayModes = getFilteredDisplayModes();
        refreshRates = getSupportedRefreshRates();

        prefs = Gdx.app.getPreferences(PREFS_NAME);
        initializeConfigurations();
        loadSettings();
    }

    private void initializeConfigurations() {
        if (!prefs.contains("graphics")) {
            prefs.putInteger("graphics", 1);
            setDefaultGraphicsSettings();
            saveGraphics();
        }
        if (!prefs.contains("postprocessor")) {
            prefs.putInteger("postprocessor", 1);
            savePostProcessor();
        }
        if (!prefs.contains("audio")) {
            prefs.putInteger("audio", 1);
            saveAudio();
        }
        if (!prefs.contains("controls")) {
            prefs.putInteger("controls", 1);
            saveControls();
        }
        if (!prefs.contains("crosshair")) {
            prefs.putInteger("crosshair", 1);
            saveCrosshair();
        }
    }

    private void setDefaultGraphicsSettings() {
        GraphicConfiguration.DIMENSION.setValue(displayModes.size() - 1);
        GraphicConfiguration.FREQUENCY.setValue(refreshRates.size() - 1);
        GraphicConfiguration.FPS.setValue(DEFAULT_REFRESH_RATES.length - 1);
    }

    public void applyGraphicSettings(int displayIndex, int refreshIndex, int fpsIndex,
                                     int qualityIndex, float brightness, boolean vsync,
                                     boolean bloom, boolean blur) {
        // Set graphic configs
        GraphicConfiguration.DIMENSION.setValue(displayIndex);
        GraphicConfiguration.FREQUENCY.setValue(refreshIndex);
        GraphicConfiguration.VSYNC.setValue(vsync);
        GraphicConfiguration.FPS.setValue(fpsIndex);

        // Set post-processor configs
        PostProcessorConfiguration.QUALITY.setValue(qualityIndex);
        PostProcessorConfiguration.BLOOM.setValue(bloom);
        PostProcessorConfiguration.BRIGHTNESS.setValue(brightness);
        PostProcessorConfiguration.MOTION_BLUR.setValue(blur);

        saveGraphics();
        savePostProcessor();
        loadGraphics();
        loadPostProcessor();
        Main.getInstance().resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void loadSettings() {
        loadGraphics();
        loadPostProcessor();
        loadAudio();
        loadControls();
        loadCrosshair();
    }

    public void loadCrosshair(){

        CrossHairConfiguration.COLOR.setValue(Color.valueOf(prefs.getString("color")));
        CrossHairConfiguration.DISTANCE.setValue(prefs.getFloat("distance"));
        CrossHairConfiguration.SIZE.setValue(prefs.getFloat("size"));
        CrossHairConfiguration.THICKNESS.setValue(prefs.getFloat("thickness"));
    }

    public void loadControls() {
        // Загружаем значения из Preferences и сохраняем в перечисления ControlsConfiguration
        ControlsConfiguration.MOVE_FORWARD.setValue(prefs.getInteger("forward"));
        ControlsConfiguration.MOVE_BACKWARD.setValue(prefs.getInteger("backward"));
        ControlsConfiguration.MOVE_LEFT.setValue(prefs.getInteger("left"));
        ControlsConfiguration.MOVE_RIGHT.setValue(prefs.getInteger("right"));
        ControlsConfiguration.JUMP.setValue(prefs.getInteger("space"));
        ControlsConfiguration.EVENT.setValue(prefs.getInteger("event"));
        ControlsConfiguration.ATACK.setValue(prefs.getInteger("attack"));
        ControlsConfiguration.RELOAD.setValue(prefs.getInteger("reload"));
        ControlsConfiguration.FAST_GUN.setValue(prefs.getInteger("fast_gun"));
        ControlsConfiguration.FAST_LOAD.setValue(prefs.getInteger("fast_load"));
        ControlsConfiguration.FAST_SAVE.setValue(prefs.getInteger("fast_save"));
        ControlsConfiguration.FOV.setValue(prefs.getFloat("fov"));
        ControlsConfiguration.MOUSE_SENSIVITY.setValue(prefs.getFloat("mouse_sensivity"));
    }

    private void loadAudio() {
        AudioConfiguration.COMMON.setValue(prefs.getFloat("common"));
        AudioConfiguration.MUSIC.setValue(prefs.getFloat("music"));
        AudioConfiguration.SOUND.setValue(prefs.getFloat("sound"));
    }

    public void loadGraphics() {

        GraphicConfiguration.DIMENSION.setValue(prefs.getInteger("dimension"));
        GraphicConfiguration.FREQUENCY.setValue(prefs.getInteger("frequency"));
        GraphicConfiguration.VSYNC.setValue(prefs.getBoolean("vsync"));
        GraphicConfiguration.FPS.setValue(prefs.getInteger("fps"));

        // Apply display settings
        applyDisplaySettings();

        // Apply VSync and FPS
        Gdx.graphics.setVSync(prefs.getBoolean("vsync"));
        if (!prefs.getBoolean("vsync")) {
            int targetFPS = DEFAULT_REFRESH_RATES[prefs.getInteger("fps")];
            Gdx.graphics.setForegroundFPS(targetFPS);
            Gdx.graphics.setForegroundFPS(5000);
        } else {
            Gdx.graphics.setForegroundFPS(-1);
        }
    }

    private void applyDisplaySettings() {
        Graphics.DisplayMode selectedMode = displayModes.get(prefs.getInteger("dimension"));
        int selectedRefresh = (Integer) refreshRates.toArray()[prefs.getInteger("frequency")];

        Graphics.DisplayMode targetMode = findDisplayMode(selectedMode.width, selectedMode.height, selectedRefresh);

        if (targetMode != null) {
            Gdx.graphics.setFullscreenMode(targetMode);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            Gdx.app.error("Settings", "Display mode not found, using default");
        }
    }

    public void loadPostProcessor() {
        PostProcessorConfiguration.QUALITY.setValue(prefs.getInteger("quality"));
        PostProcessorConfiguration.BLOOM.setValue(prefs.getBoolean("bloom"));
        PostProcessorConfiguration.BRIGHTNESS.setValue(prefs.getFloat("brightness"));
        PostProcessorConfiguration.MOTION_BLUR.setValue(prefs.getBoolean("motionblur"));

        PostProcessorComponent processor = PostProcessorComponent.getInstance();
        processor.enableBloom(prefs.getBoolean("bloom"));
        processor.enableMotionBlur(prefs.getBoolean("motionblur"));
        processor.applyBrightness(prefs.getFloat("brightness"));
    }

    public Graphics.DisplayMode findDisplayMode(int width, int height, int refreshRate) {
        return Arrays.stream(Gdx.graphics.getDisplayModes())
            .filter(mode -> mode.width == width &&
                mode.height == height &&
                mode.refreshRate == refreshRate)
            .findFirst()
            .orElse(null);
    }

    // Configuration saving methods
    public void saveGraphics() {
        prefs.putInteger("dimension", (Integer) GraphicConfiguration.DIMENSION.getValue());
        prefs.putInteger("frequency", (Integer) GraphicConfiguration.FREQUENCY.getValue());
        prefs.putBoolean("vsync", (Boolean) GraphicConfiguration.VSYNC.getValue());
        prefs.putInteger("fps", (Integer) GraphicConfiguration.FPS.getValue());
        prefs.flush();
    }

    public void saveCrosshair() {
        prefs.putString("color", CrossHairConfiguration.COLOR.getValue().toString());
        prefs.putFloat("distance", (Float) CrossHairConfiguration.DISTANCE.getValue());
        prefs.putFloat("size",(Float) CrossHairConfiguration.SIZE.getValue());
        prefs.putFloat("thickness",(Float) CrossHairConfiguration.THICKNESS.getValue());
        prefs.flush();
    }

    public void savePostProcessor() {
        prefs.putInteger("quality", (Integer) PostProcessorConfiguration.QUALITY.getValue());
        prefs.putBoolean("bloom", (Boolean) PostProcessorConfiguration.BLOOM.getValue());
        prefs.putFloat("brightness", (Float) PostProcessorConfiguration.BRIGHTNESS.getValue());
        prefs.putBoolean("motionblur", (Boolean) PostProcessorConfiguration.MOTION_BLUR.getValue());
        prefs.flush();
    }

    public void saveAudio() {
        prefs.putFloat("common", AudioConfiguration.COMMON.getValue());
        prefs.putFloat("music", AudioConfiguration.MUSIC.getValue());
        prefs.putFloat("sound", AudioConfiguration.SOUND.getValue());
        prefs.flush();
    }

    public void saveControls() {
        prefs.putInteger("forward", (Integer) ControlsConfiguration.MOVE_FORWARD.getValue());
        prefs.putInteger("backward", (Integer) ControlsConfiguration.MOVE_BACKWARD.getValue());
        prefs.putInteger("left", (Integer) ControlsConfiguration.MOVE_LEFT.getValue());
        prefs.putInteger("right", (Integer) ControlsConfiguration.MOVE_RIGHT.getValue());
        prefs.putInteger("space", (Integer) ControlsConfiguration.JUMP.getValue());
        prefs.putInteger("event", (Integer) ControlsConfiguration.EVENT.getValue());
        prefs.putInteger("attack", (Integer) ControlsConfiguration.ATACK.getValue());
        prefs.putInteger("reload", (Integer) ControlsConfiguration.RELOAD.getValue());
        prefs.putInteger("fast_gun", (Integer) ControlsConfiguration.FAST_GUN.getValue());
        prefs.putInteger("fast_load", (Integer) ControlsConfiguration.FAST_LOAD.getValue());
        prefs.putInteger("fast_save", (Integer) ControlsConfiguration.FAST_SAVE.getValue());
        prefs.putFloat("fov", (Float) ControlsConfiguration.FOV.getValue());
        prefs.putFloat("mouse_sensivity", (Float) ControlsConfiguration.MOUSE_SENSIVITY.getValue());
        prefs.flush();
    }

    // Display mode filtering methods
    private List<Graphics.DisplayMode> getFilteredDisplayModes() {
        return Arrays.stream(Gdx.graphics.getDisplayModes())
            .filter(mode -> mode.refreshRate >= 60 && (float) mode.width / mode.height >= 1.7f)
            .collect(Collectors.groupingBy(
                mode -> new DisplayResolution(mode.width, mode.height),
                Collectors.collectingAndThen(
                    Collectors.maxBy(Comparator.comparingInt(mode -> mode.refreshRate)),
                    optionalMode -> optionalMode.orElse(null)
                )
            ))
            .values().stream()
            .sorted(Comparator
                .comparingInt((Graphics.DisplayMode mode) -> mode.width)
                .thenComparingInt(mode -> mode.height))
            .collect(Collectors.toList());
    }

    private Set<Integer> getSupportedRefreshRates() {
        return Arrays.stream(Gdx.graphics.getDisplayModes())
            .filter(mode -> mode.refreshRate >= 60)
            .map(mode -> mode.refreshRate)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
