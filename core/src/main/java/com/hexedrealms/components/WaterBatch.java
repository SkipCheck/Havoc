package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hexedrealms.components.specialmeshes.WaterMesh;
import com.hexedrealms.engine.Player;
import com.hexedrealms.screens.Level;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

public class WaterBatch implements Disposable {

    private final ShaderProgram waterShader;
    private static final Array<WaterMesh> waterMeshes = new Array<>();
    private float elapsedTime;
    private int lightCount;
    private final Array<PointLightEx> currentClosestLights = new Array<>(4);

    public WaterBatch(Player player, Array<PointLightEx> lights) {
        waterShader = new ShaderProgram(
            Gdx.files.internal("shaders/water.vert").readString(),
            Gdx.files.internal("shaders/water.frag").readString()
        );

        if (!waterShader.isCompiled()) {
            Gdx.app.error("Shader", "Water shader not compiled: " + waterShader.getLog());
        }

        configureShader(player, lights);
    }

    public static Array<WaterMesh> getWaterMeshes() {
        return waterMeshes;
    }

    private Array<PointLightEx> getClosestLights(Array<PointLightEx> allLights, Vector3 position) {
        // Создаем копию для сортировки
        Array<PointLightEx> sortedLights = new Array<>(allLights.size);
        sortedLights.addAll(allLights);

        // Сортируем по расстоянию до позиции (обычно позиции камеры)
        sortedLights.sort((light1, light2) -> {
            float dist1 = light1.position.dst2(position);
            float dist2 = light2.position.dst2(position);
            return Float.compare(dist1, dist2);
        });

        // Ограничиваем 4 ближайшими источниками
        if (sortedLights.size > 4) {
            sortedLights.truncate(4);
        }

        return sortedLights;
    }

    private void configureShader(Player player, Array<PointLightEx> lights) {
        Color ambientColor = Level.AMBIENT_COLOR;

        // Получаем ближайшие источники света
        currentClosestLights.clear();
        currentClosestLights.addAll(getClosestLights(lights, player.getCamera().position));
        lightCount = currentClosestLights.size;

        waterShader.bind();
        waterShader.setUniformf("u_waveFrequency", 30.0f);
        waterShader.setUniformf("u_waveAmplitude", 0f);
        waterShader.setUniformf("u_fogStart", 0f);
        waterShader.setUniformf("u_fogEnd", player.getCamera().far);
        waterShader.setUniformf("u_fogColor", ambientColor);
        waterShader.setUniformf("u_waterColor", 0.5f, 0.5f, 0.7f);
        waterShader.setUniformf("u_ambientLightColor", ambientColor);
        waterShader.setUniformf("u_ambientIntensity", 0.3f);

        // Передаем только ближайшие источники света
        waterShader.setUniformi("u_lightCount", lightCount);
        for (int i = 0; i < lightCount; i++) {
            PointLightEx light = currentClosestLights.get(i);
            waterShader.setUniformf("u_lightPositions[" + i + "]", light.position);
            waterShader.setUniformf("u_lightColors[" + i + "]",
                light.color.r, light.color.g, light.color.b, light.intensity * 0.3f);
            waterShader.setUniformf("u_lightIntensities[" + i + "]", light.intensity * 0.3f);
        }
    }

    public void updateEnvironment(Array<PointLightEx> allLights, Camera camera) {
        // Получаем новые ближайшие источники света
        Array<PointLightEx> newClosestLights = getClosestLights(allLights, camera.position);

        // Проверяем, изменились ли ближайшие источники
//        boolean lightsChanged = newClosestLights.size != currentClosestLights.size;
//        if (!lightsChanged) {
//            for (int i = 0; i < newClosestLights.size; i++) {
//                if (!newClosestLights.get(i).equals(currentClosestLights.get(i))) {
//                    lightsChanged = true;
//                    break;
//                }
//            }
//        }

        // Обновляем только если источники изменились
//        if (lightsChanged) {
            currentClosestLights.clear();
            currentClosestLights.addAll(newClosestLights);
            lightCount = currentClosestLights.size;

            waterShader.bind();
            waterShader.setUniformi("u_lightCount", lightCount);

            for (int i = 0; i < lightCount; i++) {
                PointLightEx light = currentClosestLights.get(i);
                waterShader.setUniformf("u_lightPositions[" + i + "]", light.position);
                waterShader.setUniformf("u_lightColors[" + i + "]",
                    light.color.r, light.color.g, light.color.b, light.intensity * 0.3f);
                waterShader.setUniformf("u_lightIntensities[" + i + "]", light.intensity * 0.3f);
            }
//        }
    }

    public void render(float delta, Camera camera, boolean isPaused, Array<PointLightEx> lights) {


        // Обновляем окружение (ближайшие источники света)
        if (!isPaused) {
            elapsedTime += delta;
            updateEnvironment(lights, camera);
        }

        // Настройка depth-теста
        Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL30.GL_LESS);
        Gdx.gl.glDepthMask(true);

        waterShader.bind();

        if (!isPaused) {
            waterShader.setUniformf("u_time", elapsedTime);
        }

        waterShader.setUniformMatrix("u_projViewTrans", camera.combined);
        waterShader.setUniformf("u_cameraPosition", camera.position);

        Gdx.gl30.glEnable(GL30.GL_TEXTURE_2D);
        Gdx.gl30.glEnable(GL30.GL_BLEND);
        Gdx.gl30.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        for (WaterMesh waterMesh : waterMeshes) {
            if (!camera.frustum.boundsInFrustum(waterMesh.getBoundingBox())) continue;

            waterShader.setUniformMatrix("u_worldTrans", waterMesh.getTransform());
            waterMesh.getTexture().bind(0);
            waterShader.setUniformi("u_texture", 0);
            waterMesh.getMesh().render(waterShader, GL30.GL_TRIANGLES);
        }

        Gdx.gl.glDisable(GL30.GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        // Dispose the shader program
        if (waterShader != null) {
            waterShader.dispose();
        }

        // Clear water meshes array (meshes themselves should be disposed by their owners)
        waterMeshes.clear();

        // Clear closest lights array
        currentClosestLights.clear();

        // Reset static fields
        elapsedTime = 0;
        lightCount = 0;
    }
}
