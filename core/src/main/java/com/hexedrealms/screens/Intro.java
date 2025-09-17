package com.hexedrealms.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hexedrealms.Main;
import com.hexedrealms.engine.GUIComponent;
import com.hexedrealms.engine.SettingsComponent;

public class Intro implements Screen {
    private ShaderProgram shader;
    private SpriteBatch batch;
    private Texture logo;
    private OrthographicCamera camera;
    private Viewport viewport;

    private float stateTime = 0;
    private float alpha = 0;

    // Настройки времени анимации (в секундах)
    private static final float FADE_IN_DURATION = 1.5f;
    private static final float SHAKE_DURATION = 3.0f;
    private static final float FADE_OUT_DURATION = 1.5f;

    // Состояния анимации
    private enum State { FADE_IN, SHAKING, FADE_OUT, COMPLETE }
    private State state = State.FADE_IN;

    public Intro() {
        // Инициализация шейдера
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(
            Gdx.files.internal("shaders/logo.vert"),
            Gdx.files.internal("shaders/logo.frag")
        );

        if (!shader.isCompiled()) {
            Gdx.app.error("Shader Error", shader.getLog());
        }

        // Инициализация графики
        batch = new SpriteBatch();
        logo = new Texture(Gdx.files.internal("textures/splash/splash-test.png"));

        Graphics.DisplayMode mode = SettingsComponent.getInstance().displayModes.getLast();

        // Настройка камеры и вьюпорта
        camera = new OrthographicCamera();
        viewport = new FitViewport(mode.width, mode.height, camera);
        camera.position.set(viewport.getWorldWidth()/2, viewport.getWorldHeight()/2, 0);
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
        stateTime += delta * 1.5;

        // Управление состоянием анимации
        switch (state) {
            case FADE_IN:
                alpha = Interpolation.fade.apply(Math.min(1, stateTime / FADE_IN_DURATION));
                if (stateTime >= FADE_IN_DURATION) {
                    state = State.SHAKING;
                    stateTime = 0;
                }
                break;

            case SHAKING:
                if (stateTime >= SHAKE_DURATION) {
                    state = State.FADE_OUT;
                    stateTime = 0;
                }
                break;

            case FADE_OUT:
                alpha = Interpolation.fade.apply(1 - Math.min(1, stateTime / FADE_OUT_DURATION));
                if (stateTime >= FADE_OUT_DURATION) {
                    state = State.COMPLETE;
                    // Здесь можно перейти к следующему экрану
                    // game.setScreen(new MainMenuScreen());
                }
                break;

            case COMPLETE:
                // Ничего не делаем, ждем перехода
                break;
        }

        // Очистка экрана
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Включение прозрачности
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Обновление камеры
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Установка шейдера и параметров
        batch.setShader(shader);
        shader.setUniformf("u_time", stateTime);
        shader.setUniformf("u_alpha", alpha);

        // Отрисовка логотипа по центру
        batch.begin();
        float x = (viewport.getWorldWidth() - logo.getWidth()) / 2;
        float y = (viewport.getWorldHeight() - logo.getHeight()) / 2;
        batch.draw(logo, x, y);
        batch.end();

        // Отключение шейдера
        batch.setShader(null);

        // Пропуск интро по клику
        if (Gdx.input.justTouched() || state == State.COMPLETE) {
            dispose();
            Main.getInstance().setScreen(new MenuContainer());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        logo.dispose();
        shader.dispose();

        viewport = null;
        camera = null;
        batch = null;
        logo = null;
        shader = null;
    }
}
