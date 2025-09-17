package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.specialmeshes.CloudMesh;
import com.hexedrealms.engine.Player;

public class CloudsBatch implements Disposable {

    private final ShaderProgram cloudsShader;
    public static final Array<CloudMesh> cloudsMeshes = new Array<>();
    private float elapsedTime;
    public Color color = new Color(0.1f,0.1f,0.1f,1f);

    public CloudsBatch(){
        cloudsShader = new ShaderProgram(Gdx.files.internal("shaders/cloud.vert").readString(), Gdx.files.internal("shaders/cloud.frag").readString());
    }

    public static Array<CloudMesh> getCloudsMeshes(){
        return cloudsMeshes;
    }

    public void render(float delta, Camera camera, boolean isPaused){
        if(!isPaused) {
            elapsedTime += delta;
        }

        Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL30.GL_LESS);
        Gdx.gl.glDepthMask(true);

        cloudsShader.bind();
        cloudsShader.setUniformMatrix("u_projViewTrans", camera.combined);
        if(!isPaused) {
            cloudsShader.setUniformf("u_time", elapsedTime);
        }

        Gdx.gl30.glEnable(GL30.GL_TEXTURE_2D);
        Gdx.gl30.glEnable(GL30.GL_BLEND);
        Gdx.gl30.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        for(CloudMesh mesh : cloudsMeshes) {
            cloudsShader.setUniformMatrix("u_worldTrans", mesh.getTransform());
            mesh.getTexture().bind(0);
            cloudsShader.setUniformi("u_texture", 0);
            cloudsShader.setUniformf("u_alpha", 1f);
            cloudsShader.setUniformf("u_color", color); // Пример цвета
            mesh.getMesh().render(cloudsShader, GL30.GL_TRIANGLES);
        }

        Gdx.gl.glDisable(GL30.GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        // Dispose the shader program
        if (cloudsShader != null) {
            cloudsShader.dispose();
        }

        // Clear the clouds meshes array (meshes themselves should be disposed by their owners)
        cloudsMeshes.clear();

        // Reset state variables
        elapsedTime = 0f;
        color = null;
    }
}
