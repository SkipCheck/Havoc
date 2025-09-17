package com.hexedrealms.engine;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.hexedrealms.screens.Level;

import java.util.concurrent.atomic.AtomicReference;

public class VisUIComponent implements Disposable {

    private static final AtomicReference<VisUIComponent> instance = new AtomicReference<>();
    private Stage stage;

    public static VisUIComponent getInstance() {
        VisUIComponent result = instance.get();
        if (result == null) {
            synchronized (VisUIComponent.class) {
                result = instance.get();
                if (result == null) {
                    result = new VisUIComponent();
                    instance.set(result);
                }
            }
        }
        return result;
    }

    public Stage getStage() {
        return stage;
    }

    private VisUIComponent(){
        Graphics.DisplayMode displayMode = SettingsComponent.getInstance().displayModes.getLast();
        ExtendViewport screenViewport = new ExtendViewport(displayMode.width, displayMode.height);
        stage = new Stage(screenViewport);
    }

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }

    public void appendActor(Actor actor){
        stage.addActor(actor);
    }

    public void render(float delta){
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        // Dispose the stage and all its actors
        if (stage != null) {
            stage.clear(); // Remove all actors first
            stage.dispose();
            stage = null;
        }

        // Reset singleton instance
        instance.set(null);
    }
}
