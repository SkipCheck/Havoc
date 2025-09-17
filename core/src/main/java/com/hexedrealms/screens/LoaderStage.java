package com.hexedrealms.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.hexedrealms.Main;
import com.hexedrealms.components.CloudsBatch;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.engine.*;
import com.kotcrab.vis.ui.widget.VisLabel;

public class LoaderStage implements Screen {

    private final int slotId;
    private final int difficultyId;
    private final Texture texture;
    private final SpriteBatch batch;
    private final ShaderProgram shader;
    private final Stage stage;
    private final VisLabel levelLabel;
    private final VisLabel pressELabel;
    private final LoaderComponent loaderComponent;

    private float time, alpha;
    private Thread loadingThread;
    private boolean loadingComplete = false;
    private boolean showPressE = false;
    private boolean isFirstLaunch;
    private Level level;
    private com.hexedrealms.utils.savedata.Level levelData;
    private MenuContainer container;
    private float loadingProgress = 0;
    private boolean returnToMainMenu, transitionStarted;
    private int levelId;

    // Новые поля для splash-текста
    private boolean showingSplash = false;
    private float splashTimer = 0;
    private int splashCharIndex = 0;
    private VisLabel splashLabel;
    private String splashText = "";

    public LoaderStage(String backgroundName, int slotId, int difficultyId, boolean returnToMainMenu) {
        this.slotId = slotId;
        this.difficultyId = difficultyId;
        this.loaderComponent = new LoaderComponent();
        this.returnToMainMenu = returnToMainMenu;

        String slotName = getSlotName(slotId);
        this.isFirstLaunch = !loaderComponent.saveExists(slotName);

        Graphics.DisplayMode displayMode = SettingsComponent.getInstance().displayModes.getLast();
        ExtendViewport screenViewport = new ExtendViewport(displayMode.width, displayMode.height);
        this.stage = new Stage(screenViewport);

        this.levelLabel = createLevelLabel();
        this.pressELabel = createPressELabel();

        this.texture = new Texture(Gdx.files.internal(backgroundName));
        this.batch = new SpriteBatch();
        this.shader = createShader();

        setupStage();
        AudioComponent.getInstance().dispose();
        AudioComponent.getInstance().loadMainMusic("audio/music/loader.wav");
        AudioComponent.getInstance().setMusicState(AudioComponent.MusicState.EXPLORATION);
    }

    public void putLevelData(com.hexedrealms.utils.savedata.Level level) {
        levelData = level;
        levelLabel.setText(levelData.name);
        if (levelData.splashText != null && !levelData.splashText.isEmpty()) {
            this.splashText = levelData.splashText;
            this.splashLabel = createSplashLabel();
        }
    }

    private String getSlotName(int slotId) {
        return "slot" + slotId;
    }

    private VisLabel createLevelLabel() {
        VisLabel label = new VisLabel("");
        label.setAlignment(Align.center);
        label.setFontScale(1.2f);
        label.setPosition(
            (stage.getWidth() - label.getWidth()) / 2,
            (stage.getHeight() - label.getHeight()) / 2 + stage.getHeight() * 0.2f
        );
        return label;
    }

    private VisLabel createPressELabel() {
        VisLabel label = new VisLabel("Нажмите "+Input.Keys.toString((Integer) ControlsConfiguration.EVENT.getValue())+" чтобы продолжить");
        label.setAlignment(Align.center);
        label.setFontScale(1.0f);
        label.setPosition(
            (stage.getWidth() - label.getWidth()) / 2,
            stage.getHeight() * 0.1f
        );
        label.setColor(1, 1, 1, 0);
        return label;
    }

    private VisLabel createSplashLabel() {
        VisLabel label = new VisLabel("");
        label.setAlignment(Align.center);
        label.setFontScale(1.5f);
        label.setWrap(true);
        label.setWidth(stage.getWidth() * 0.8f);
        label.setPosition(
            (stage.getWidth() - label.getWidth()) / 2,
            stage.getHeight() * 0.5f
        );
        label.setColor(1, 1, 1, 0);
        return label;
    }

    private ShaderProgram createShader() {
        String vertexShader = Gdx.files.internal("shaders/loader.vert").readString();
        String fragmentShader = Gdx.files.internal("shaders/loader.frag").readString();
        ShaderProgram program = new ShaderProgram(vertexShader, fragmentShader);
        if (!program.isCompiled()) {
            Gdx.app.error("Shader", program.getLog());
        }
        return program;
    }

    private void setupStage() {
        stage.addActor(new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                if(!showingSplash) {
                    batch.setShader(shader);
                    shader.setUniformf("u_time", time);
                    shader.setUniformf("u_alpha", alpha);
                    batch.draw(texture, 0, 0, getStage().getWidth(), getStage().getHeight());
                    batch.setShader(null);
                }
            }
        });

        if(!returnToMainMenu)
            stage.addActor(levelLabel);
    }

    public void setLevelId(int id) {
        this.levelId = id;
    }

    private void transitionToNextScreen() {
        dispose();
        if (returnToMainMenu) {
            Main.getInstance().setScreen(container);
        } else {
            Main.getInstance().setScreen(level);
        }
    }

    @Override
    public void show() {
        loadingThread = new Thread(() -> {
            loadingProgress = 1f;
            try {
                Gdx.app.postRunnable(() -> {
                    try {
                        if(!returnToMainMenu) {
                            ResourcesLoader.loadResources();
                            GUIComponent.getInstance().clearQueue();
                            EntityComponent.getInstance(null).dispose();
                            ParticlesComponent.getInstance(null).dispose();
                            CloudsBatch.cloudsMeshes.clear();

                            String slotName = getSlotName(slotId);

                            final Level[] loadedLevel = new Level[1];

                            if (!isFirstLaunch) {
                                loadedLevel[0] = loaderComponent.loadGame(slotName);
                                com.hexedrealms.utils.savedata.Level data = ResourcesLoader.getLevel(loadedLevel[0].levelId);
                                levelLabel.setText(data.name);
                            }

                            if (loadedLevel[0] != null) {
                                level = loadedLevel[0];
                            } else {
                                level = Level.getInstance(levelData);
                                level.setDifficulty(difficultyId);
                                level.levelId = levelId;
                                level.slotId = slotId;

                                LoaderComponent loader = new LoaderComponent();
                                loader.saveGame("slot"+slotId);
                            }
                        } else {
                            container = new MenuContainer();
                        }

                        loadingComplete = true;

                    } catch (Exception e) {
                        Gdx.app.error("LoaderStage", "GL Initialization error", e);
                        handleLoadError();
                    }
                });
            } catch (Exception e) {
                Gdx.app.error("LoaderStage", "Loading thread error", e);
                Gdx.app.postRunnable(this::handleLoadError);
            }
        });
        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    private void handleLoadError() {
        String slotName = getSlotName(slotId);
        level = Level.getInstance();
        level.slotId = slotId;
        loaderComponent.saveGame(slotName);
        level.setDifficulty(difficultyId);
        loadingComplete = true;
    }

    @Override
    public void render(float delta) {
        updateAudio();
        updateTime(delta);
        updateAlpha(delta);

        // Очищаем экран черным цветом
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Рисуем фон только если не показывается splash
        if (!showingSplash) {
            stage.getBatch().begin();
            stage.getBatch().setShader(shader);
            shader.setUniformf("u_time", time);
            shader.setUniformf("u_alpha", alpha);
            stage.getBatch().draw(texture, 0, 0, stage.getWidth(), stage.getHeight());
            stage.getBatch().setShader(null);
            stage.getBatch().end();
        }

        handleLoadingCompletion(delta);

        // Обновляем splash-текст
        if (showingSplash && splashCharIndex < splashText.length()) {
            splashTimer += delta;
            if (splashTimer >= 0.05f) {
                splashTimer = 0;
                splashCharIndex++;
                splashLabel.setText(splashText.substring(0, splashCharIndex));
            }
        }

        stage.act(delta);
        stage.draw();

        if (returnToMainMenu && loadingComplete && !transitionStarted) {
            transitionStarted = true;
            transitionToNextScreen();
        } else {
            handleUserInput();
        }
    }

    private void updateAudio() {
        AudioComponent.getInstance().update();
    }

    private void updateTime(float delta) {
        time += delta;
    }

    private void updateAlpha(float delta) {
        if (alpha < 1) {
            alpha += delta * 0.5f;
        }
    }

    private void handleLoadingCompletion(float delta) {
        if (loadingComplete && !showPressE) {
            showPressE = true;
            stage.addActor(pressELabel);
        }

        if (showPressE) {
            float currentAlpha = pressELabel.getColor().a;
            float targetAlpha = Math.min(1, currentAlpha + delta * 0.8f);
            pressELabel.setColor(1, 1, 1, targetAlpha);

            float blink = (float)(Math.sin(time * 9) + 1) / 2 * 0.5f + 0.5f;
            pressELabel.setColor(1, 1, 1, blink);
        }
    }

    private void handleUserInput() {
        if (showPressE && (Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.EVENT.getValue()))) {
            if (!showingSplash && splashLabel != null && !splashText.isEmpty()) {
                showingSplash = true;
                splashTimer = 0;
                splashCharIndex = 0;
                stage.addActor(splashLabel);
                splashLabel.setText("");
                splashLabel.setColor(1, 1, 1, 1);
                levelLabel.setColor(1, 1, 1, 0);
            } else if (showingSplash) {
                if (splashCharIndex < splashText.length()) {
                    splashCharIndex = splashText.length();
                    splashLabel.setText(splashText);
                } else {
                    transitionToNextScreen();
                }
            } else {
                transitionToNextScreen();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        levelLabel.setPosition(
            (width - levelLabel.getWidth()) / 2,
            (height - levelLabel.getHeight()) / 2 + height * 0.2f
        );
        pressELabel.setPosition(
            (width - pressELabel.getWidth()) / 2,
            height * 0.1f
        );
        if (splashLabel != null) {
            splashLabel.setWidth(width * 0.8f);
            splashLabel.setPosition(
                (width - splashLabel.getWidth()) / 2,
                height * 0.5f
            );
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
            try {
                loadingThread.join(100);
            } catch (InterruptedException e) {
                Gdx.app.error("LoaderStage", "Thread interruption error", e);
            }
        }

        if (texture != null) texture.dispose();
        if (batch != null) batch.dispose();
        if (shader != null) shader.dispose();
        if (stage != null) stage.dispose();
    }

    public int getSlotId() { return slotId; }
    public int getDifficultyId() { return difficultyId; }

    public void setFirstLaunch() {
        isFirstLaunch = true;
    }
}
