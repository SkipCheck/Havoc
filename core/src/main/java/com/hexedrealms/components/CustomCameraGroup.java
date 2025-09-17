package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.engine.LightingComponent;
import com.hexedrealms.engine.PhysicComponent;
import net.mgsx.gltf.scene3d.lights.PointLightEx;

import java.util.Comparator;

public class CustomCameraGroup implements GroupStrategy, Disposable {
    private static final int GROUP_OPAQUE = 0;
    private static final int GROUP_BLEND = 1;

    private final Vector3 toCam1;
    private final Vector3 toCam2;

    private final Pool<Array<Decal>> arrayPool = new Pool<>(16) {
        @Override
        protected Array<Decal> newObject() {
            return new Array<>();
        }
    };
    private final Array<Array<Decal>> usedArrays = new Array<>();
    private final ObjectMap<DecalMaterial, Array<Decal>> materialGroups = new ObjectMap<>();

    private Camera camera;
    private ShaderProgram shader;
    private final Comparator<Decal> cameraSorter;

    public CustomCameraGroup(Camera camera) {
        this.camera = camera;
        this.cameraSorter = this::compareCameraDistance;
        this.toCam1 = PhysicComponent.getInstance().getVectorPool().obtain();
        this.toCam2 = PhysicComponent.getInstance().getVectorPool().obtain();

        createDefaultShader();
    }

    public CustomCameraGroup(Camera camera, Comparator<Decal> sorter) {
        this.camera = camera;
        this.cameraSorter = sorter;
        this.toCam1 = PhysicComponent.getInstance().getVectorPool().obtain();
        this.toCam2 = PhysicComponent.getInstance().getVectorPool().obtain();
        createDefaultShader();
    }

    private int compareCameraDistance(Decal o1, Decal o2) {
        // Вектор от камеры к декалю
        toCam1.set(camera.position).add(o1.getPosition());
        toCam2.set(camera.position).add(o2.getPosition());

        // Используем проекцию на направление камеры
        float proj1 = toCam1.dot(camera.direction);
        float proj2 = toCam2.dot(camera.direction);

        // Сравниваем с учетом проекции
        return Float.compare(proj2, proj1);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public int decideGroup(Decal decal) {
        return decal.getMaterial().isOpaque() ? GROUP_OPAQUE : GROUP_BLEND;
    }

    @Override
    public void beforeGroup(int group, Array<Decal> contents) {
        if (group == GROUP_BLEND) {
            Gdx.gl.glEnable(GL30.GL_BLEND);
            Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glDepthMask(false);
            contents.sort(cameraSorter);
        } else {
            Gdx.gl.glStencilFunc(GL30.GL_ALWAYS, 1, 0xFF); // Проверяем, что значение в буфере равно 1
            groupByMaterial(contents);
        }
    }

    private void groupByMaterial(Array<Decal> contents) {
        // Используем быстрый путь для небольшого количества декалей
        if (contents.size < 10) {
            return;
        }

        // Оптимизированная версия с меньшим количеством аллокаций
        materialGroups.clear();
        for (Decal decal : contents) {
            Array<Decal> materialGroup = materialGroups.get(decal.getMaterial());
            if (materialGroup == null) {
                materialGroup = arrayPool.obtain();
                materialGroup.clear();
                materialGroups.put(decal.getMaterial(), materialGroup);
            }
            materialGroup.add(decal);
        }

        contents.clear();
        for (Array<Decal> materialGroup : materialGroups.values()) {
            contents.addAll(materialGroup);
            arrayPool.free(materialGroup); // Освобождаем сразу после использования
        }
        materialGroups.clear();
    }

    @Override
    public void afterGroup(int group) {
        if (group == GROUP_BLEND) {
            Gdx.gl.glDisable(GL30.GL_BLEND);
            Gdx.gl.glDepthMask(true);
        } else {
            Gdx.gl.glStencilFunc(GL30.GL_ALWAYS, 1, 0xFF); // Возвращаем настройки по умолчанию
        }
    }

    @Override
    public void beforeGroups() {
        // Используем более эффективные настройки глубины
        Gdx.gl.glDepthFunc(GL30.GL_LEQUAL);
        Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);

        // Оптимизация теста трафарета
        Gdx.gl.glEnable(GL30.GL_STENCIL_TEST);
        Gdx.gl.glStencilOp(GL30.GL_KEEP, GL30.GL_KEEP, GL30.GL_REPLACE);
        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glStencilFunc(GL30.GL_ALWAYS, 1, 0xFF);

        shader.bind();
        setupShaderUniforms();
    }

    private void setupShaderUniforms() {
        shader.bind();
        shader.setUniformMatrix("u_projectionViewMatrix", camera.combined);
        shader.setUniformi("u_texture", 0);

        Array<PointLightEx> pointLights = LightingComponent.getInstance().getPointLightArray();
        Array<PointLightEx> closestLights = getClosestLights(pointLights, camera.position);
        int lightCount = Math.min(closestLights.size, 4);

        shader.setUniformi("u_lightCount", lightCount);

        float[] lightPositions = new float[lightCount * 3];
        float[] lightColors = new float[lightCount * 3];
        float[] lightIntensities = new float[lightCount];

        for (int i = 0; i < lightCount; i++) {
            PointLightEx light = closestLights.get(i);

            lightPositions[i * 3] = light.position.x;
            lightPositions[i * 3 + 1] = light.position.y;
            lightPositions[i * 3 + 2] = light.position.z;

            lightColors[i * 3] = light.color.r;
            lightColors[i * 3 + 1] = light.color.g;
            lightColors[i * 3 + 2] = light.color.b;

            lightIntensities[i] = light.intensity * (light instanceof DynamicLight ? 0.1f : 0.4f);
        }

        shader.setUniform3fv("u_lightPositions", lightPositions, 0, lightCount * 3);
        shader.setUniform3fv("u_lightColors", lightColors, 0, lightCount * 3);
        shader.setUniform1fv("u_lightIntensities", lightIntensities, 0, lightCount);
        shader.setUniformf("u_ambientColor", 1f, 1f, 1f);
        shader.setUniformf("u_ambientIntensity", 0.9f);
    }

    private Array<PointLightEx> getClosestLights(Array<PointLightEx> allLights, Vector3 position) {
        Array<PointLightEx> sortedLights = new Array<>(allLights);
        sortedLights.sort((light1, light2) -> {
            float dist1 = light1.position.dst2(position);
            float dist2 = light2.position.dst2(position);
            return Float.compare(dist1, dist2);
        });

        if (sortedLights.size > 4) {
            sortedLights.truncate(4);
        }
        return sortedLights;
    }

    @Override
    public void afterGroups() {
        Gdx.gl.glDisable(GL30.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL30.GL_STENCIL_TEST); // Отключаем тест трафарета
        Gdx.gl.glStencilMask(0x00);
    }

    private void createDefaultShader() {
        shader = new ShaderProgram(
            Gdx.files.internal("shaders/decal_vertex.glsl").readString(),
            Gdx.files.internal("shaders/decal_fragment.glsl").readString()
        );
        if (!shader.isCompiled()) {
            throw new IllegalArgumentException("Couldn't compile shader: " + shader.getLog());
        }
    }

    @Override
    public ShaderProgram getGroupShader(int group) {
        return shader;
    }

    @Override
    public void dispose() {
        if (shader != null) shader.dispose();
    }
}
