package com.hexedrealms;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.hexedrealms.engine.*;
import com.hexedrealms.screens.Intro;
import com.hexedrealms.screens.Level;
import com.hexedrealms.weapons.Weapon;
import com.kotcrab.vis.ui.VisUI;

/** {@link ApplicationListener} implementation shared by all platforms. */

public class Main extends Game implements ApplicationListener {
    private GUIComponent guiComponent;
    private SettingsComponent settingsComponent;
    private AudioComponent audioComponent;
    private Intro intro;

    private static Main instance;

    public static Main getInstance() {
        if(instance == null)
            instance = new Main();
        return instance;
    }

    @Override
    public void create() {
        if(!VisUI.isLoaded())
            VisUI.load(Gdx.files.internal("skins/skin.json"));

        if (Gdx.graphics.getGLVersion().getType() == GLVersion.Type.OpenGL) {
            if (Gdx.graphics.getGLVersion().getMajorVersion() >= 3) {
                Gdx.gl30 = (GL30) Gdx.gl;
            }
        }
        settingsComponent = SettingsComponent.getInstance();
        audioComponent = AudioComponent.getInstance();
        ResourcesLoader.loadResources();

        guiComponent = GUIComponent.getInstance();

        intro = new Intro();
        setScreen(intro);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {

    }
}
