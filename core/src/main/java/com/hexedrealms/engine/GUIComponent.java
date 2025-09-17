package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.hexedrealms.screens.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class GUIComponent {
    private static final AtomicReference<GUIComponent> instance = new AtomicReference<>();
    private HashMap<Class, MenuStage> stages;
    private Array<MenuStage> queue;
    private int width, height;
    public int x, y;

    public GUIComponent(){
        stages = new HashMap<>();
        queue = new Array<>();

        Graphics.DisplayMode displayMode = SettingsComponent.getInstance().displayModes.getLast();
        ExtendViewport screenViewport = new ExtendViewport(displayMode.width, displayMode.height);
        width = displayMode.width;
        height = displayMode.height;

        stages.put(MenuStage.class, new MenuStage(screenViewport, "ГЛАВНОЕ МЕНЮ"));
        stages.put(PauseStage.class, new PauseStage(screenViewport, "ИГРА ПРИОСТАНОВЛЕНА"));
        stages.put(SettingsStage.class, new SettingsStage(screenViewport, "НАСТРОЙКИ"));
        stages.put(ModalStage.class, new ModalStage(screenViewport, "ГРАФИЧЕСКИЕ НАСТРОЙКИ"));
        stages.put(DialogStage.class, new DialogStage(screenViewport, "ВНИМАНИЕ!"));
        stages.put(AudioStage.class, new AudioStage(screenViewport, "НАСТРОЙКИ ЗВУКА"));
        stages.put(ControlsStage.class, new ControlsStage(screenViewport, "НАСТРОЙКИ УПРАВЛЕНИЯ"));
        stages.put(CrosshairStage.class, new CrosshairStage(screenViewport, "НАСТРОЙКИ ПРИЦЕЛА"));
        stages.put(SlotSaves.class, new SlotSaves(screenViewport, "ВЫБЕРИТЕ СЛОТ"));
        stages.put(DifficultySelectionScreen.class, new DifficultySelectionScreen(screenViewport, "ВЫБЕРИТЕ СЛОЖНОСТЬ"));
    }

    public MenuStage getMenuStage(Class<?> type){
        return stages.get(type);
    }

    public void resize(int width, int height){
        this.width = width;
        this.height = height;

        if(!queue.isEmpty()) {
            for (MenuStage menuStage : queue) {
                menuStage.resize(width, height);
            }
        }
    }

    public void putStage(MenuStage stage){
        queue.add(stage);
        stage.resize(width, height);

        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
    }

    public void removeStage(Class<?> menuStage){
        if(queue.isEmpty()) return;

        for(MenuStage stage : queue){
            if(stage.getClass().equals(menuStage)){
                removeStage(stage);
                return;
            }
        }
    }

    public void removeStage(MenuStage stage){
        queue.removeValue(stage, true);
        if(!queue.isEmpty()) {
            Gdx.input.setInputProcessor(queue.get(queue.size - 1));
        }
    }

    public void render(float delta){
        if(queue.isEmpty()) return;
        queue.get(queue.size-1).act(delta);
        for(MenuStage stage : queue) {
            stage.draw();
        }

        handleEscape();
    }

    private void handleEscape(){
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MenuStage stage = queue.get(queue.size-1);
            if(stage instanceof  DialogStage || stage.preventDefault() || stage.getClass().equals(MenuStage.class)) return;
            removeStage(queue.get(queue.size - 1));
        }
    }

    public void clearQueue(){
        queue.clear();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public static GUIComponent getInstance() {
        GUIComponent result = instance.get();
        if (result == null) {
            synchronized (GUIComponent.class) {
                result = instance.get();
                if (result == null) {
                    result = new GUIComponent();
                    instance.set(result);
                }
            }
        }
        return result;
    }
}
