package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.FrameBufferCubemap;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.hexedrealms.engine.PhysicComponent;
import net.mgsx.gltf.scene3d.utils.FacedMultiCubemapData;

public class CustomIDBuilder implements Disposable {
    public static CustomIDBuilder createOutdoor(DirectionalLight sun) {
        CustomIDBuilder ibl = new CustomIDBuilder();

        ibl.nearGroundColor.set(.5f, .45f, .4f, 1);
        ibl.farGroundColor.set(.3f, .25f, .2f, 1);
        ibl.nearSkyColor.set(.7f, .8f, 1f, 1);
        ibl.farSkyColor.set(.9f, .95f, 1f, 1);

        CustomIDBuilder.Light light = new CustomIDBuilder.Light();
        light.direction.set(sun.direction).nor();
        light.color.set(sun.color);
        light.exponent = 30f;
        ibl.lights.add(light);

        return ibl;
    }

    public static CustomIDBuilder createIndoor(DirectionalLight sun) {
        CustomIDBuilder ibl = new CustomIDBuilder();

        Color tint = new Color(1f, .9f, .8f, 1).mul(.3f);

        ibl.nearGroundColor.set(tint).mul(.7f);
        ibl.farGroundColor.set(tint);
        ibl.farSkyColor.set(tint);
        ibl.nearSkyColor.set(tint).mul(2f);

        CustomIDBuilder.Light light = new CustomIDBuilder.Light();
        light.direction.set(sun.direction).nor();
        light.color.set(1f, .5f, 0f, 1f).mul(.3f);
        light.exponent = 3f;
        ibl.lights.add(light);

        return ibl;
    }

    public static CustomIDBuilder createCustom(DirectionalLight sun) {
        CustomIDBuilder ibl = new CustomIDBuilder();

        CustomIDBuilder.Light light = new CustomIDBuilder.Light();
        light.direction.set(sun.direction).nor();
        light.color.set(sun.color);
        light.exponent = 100f;
        ibl.lights.add(light);

        return ibl;
    }

    public final Color nearGroundColor = new Color();
    public final Color farGroundColor = new Color();
    public final Color nearSkyColor = new Color();
    public final Color farSkyColor = new Color();

    public final Array<CustomIDBuilder.Light> lights = new Array<CustomIDBuilder.Light>();

    public boolean renderSun = true;
    public boolean renderGradient = true;

    private final ShaderProgram sunShader;
    private ShapeRenderer shapes;
    private ShapeRenderer sunShapes;

    private CustomIDBuilder() {
        shapes = new ShapeRenderer(20);
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, 1, 1);

        sunShader = new ShaderProgram(
            Gdx.files.classpath("net/mgsx/gltf/shaders/ibl-sun.vs.glsl"),
            Gdx.files.classpath("net/mgsx/gltf/shaders/ibl-sun.fs.glsl"));
        if(!sunShader.isCompiled()) throw new GdxRuntimeException(sunShader.getLog());

        sunShapes = new ShapeRenderer(20, sunShader);
        sunShapes.getProjectionMatrix().setToOrtho2D(0, 0, 1, 1);
    }

    @Override
    public void dispose() {
        sunShader.dispose();
        sunShapes.dispose();
        shapes.dispose();
    }

    /**
     * Create an environment map, to be used with {@link net.mgsx.gltf.scene3d.scene.SceneSkybox}
     * @param size base size (width and height) for generated cubemap
     * @return generated cubemap, caller is responsible to dispose it when no longer used.
     */
    public Cubemap buildEnvMap(int size) {
        FrameBufferCubemap fbo = new FrameBufferCubemap(Pixmap.Format.RGBA8888, size, size, false) {
            @Override
            protected void disposeColorTexture(Cubemap colorTexture) {
            }
        };

        try {
            fbo.begin();
            while (fbo.nextSide()) {
                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
                Cubemap.CubemapSide side = fbo.getSide();
                renderGradient(side, 0);
                renderLights(side, false);
            }
            fbo.end();

            // Получаем текстуру кубической карты
            Cubemap map = fbo.getColorBufferTexture();

            // Сохраняем каждую сторону как отдельное изображение
            saveCubemapSides(map, "environment_maps/my_env_map");

            return map;
        } finally {
            fbo.dispose();
        }
    }

    private void saveCubemapSides(Cubemap cubemap, String outputPath) {
        Cubemap.CubemapSide[] sides = {
            Cubemap.CubemapSide.PositiveX,
            Cubemap.CubemapSide.NegativeX,
            Cubemap.CubemapSide.PositiveY,
            Cubemap.CubemapSide.NegativeY,
            Cubemap.CubemapSide.PositiveZ,
            Cubemap.CubemapSide.NegativeZ
        };

        String[] sideNames = {
            "px.png", "nx.png",
            "py.png", "ny.png",
            "pz.png", "nz.png"
        };

        for (int i = 0; i < sides.length; i++) {
            // Создаем Pixmap для текущей стороны
            Pixmap pixmap = createPixmapFromCubemapSide(cubemap, sides[i]);

            try {
                // Сохраняем Pixmap в файл
                PixmapIO.writePNG(Gdx.files.local(sideNames[i]), pixmap);
            } catch (Exception e) {
                System.err.println("Error saving cubemap side: " + sideNames[i]);
                e.printStackTrace();
            } finally {
                // Всегда dispose Pixmap
                pixmap.dispose();
            }
        }
    }

    private Pixmap createPixmapFromCubemapSide(Cubemap cubemap, Cubemap.CubemapSide side) {
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, cubemap.getWidth(), cubemap.getHeight(), false);

        fbo.begin();

        // Привязываем текстуру стороны кубической карты
        Gdx.gl.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + side.ordinal(),
            cubemap.getTextureObjectHandle(),
            0
        );

        // Создаем Pixmap из текущего буфера кадра
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, cubemap.getWidth(), cubemap.getHeight());

        fbo.end();
        fbo.dispose();

        return pixmap;
    }

    /**
     * Creates an irradiance map, to be used with {@link net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute#DiffuseEnv}
     * @param size base size (width and height) for generated cubemap
     * @return generated cubemap, caller is responsible to dispose it when no longer used.
     */
    public Cubemap buildIrradianceMap(int size){

        FrameBufferCubemap fbo = new FrameBufferCubemap(Pixmap.Format.RGBA8888, size, size, false){
            @Override
            protected void disposeColorTexture(Cubemap colorTexture) {
            }
        };

        fbo.begin();
        while(fbo.nextSide()){
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
            Cubemap.CubemapSide side = fbo.getSide();
            renderGradient(side, 0.5f);
            renderLights(side, true);
        }
        fbo.end();
        Cubemap map = fbo.getColorBufferTexture();
        fbo.dispose();
        return map;
    }

    /**
     * Creates an radiance map, to be used with {@link net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute#SpecularEnv}
     * generated cubemap contains mipmaps in order to perform roughness in PBR shading
     * @param mipMapLevels how many mipmaps level, eg. 10 levels produce a 1024x1024 cubemap with mipmaps.
     * @return generated cubemap, caller is responsible to dispose it when no longer used.
     */
    public Cubemap buildRadianceMap(final int mipMapLevels){
        Pixmap[] maps = new Pixmap[mipMapLevels * 6];
        int index = 0;
        for(int level=0 ; level<mipMapLevels ; level++){
            int size = 1 << (mipMapLevels - level - 1);
            FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, size, size, false);
            fbo.begin();
            for(int s=0 ; s<6 ; s++){
                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

                Cubemap.CubemapSide side = Cubemap.CubemapSide.values()[s];

                float blur = (float)level / (float)mipMapLevels;

                renderGradient(side, blur);
                renderLights(side, false);

                maps[index] = ScreenUtils.getFrameBufferPixmap(0, 0, size, size);
                index++;
            }
            fbo.end();
            fbo.dispose();
        }
        FacedMultiCubemapData data = new FacedMultiCubemapData(maps, mipMapLevels);
        Cubemap map = new Cubemap(data);
        map.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Linear);
        return map;
    }

    private void renderGradient(Cubemap.CubemapSide side, float blur){
        if(!renderGradient) return;

        Color aveSky = farSkyColor.cpy().lerp(nearSkyColor, .5f);
        Color aveGnd = farGroundColor.cpy().lerp(nearGroundColor, .5f);

        Color ave = aveSky.cpy().lerp(aveGnd, .5f);

        Color aveHorizon = farGroundColor.cpy().lerp(farSkyColor, .5f);

        // blur!
        float t2 = 1 - (float)Math.pow(1 - blur, 4);
        float t = 1 - (float)Math.pow(1 - blur, 1);

        Color ngc = nearGroundColor.cpy().lerp(ave, t);
        Color nsc = nearSkyColor.cpy().lerp(ave, t);

        Color fgc = farGroundColor.cpy().lerp(aveHorizon, t2).lerp(ave, t);
        Color fsc = farSkyColor.cpy().lerp(aveHorizon, t2).lerp(ave, t);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if(side == Cubemap.CubemapSide.PositiveY){
            shapes.rect(0, 0, 1, 1, nsc, nsc, nsc, nsc);
        }
        else if(side == Cubemap.CubemapSide.NegativeY){
            shapes.rect(0, 0, 1, 1, ngc, ngc, ngc, ngc);
        }
        else{
            // draw vertical gradient
            shapes.rect(0, 0, 1, .5f, nsc, nsc, fsc, fsc);
            shapes.rect(0, .5f, 1, .5f, fgc, fgc, ngc, ngc);
        }
        shapes.end();
    }

    public static class Light {

        public final Color color = new Color(1f, 1f, 1f, 1f);
        public final Vector3 direction = PhysicComponent.getInstance().getVectorPool().obtain().set(0,-1,0);
        public float exponent = 30f;

        private static final Vector3 localSunDir = PhysicComponent.getInstance().getVectorPool().obtain();
        private static final Vector3 localDir = PhysicComponent.getInstance().getVectorPool().obtain();
        private static final Vector3 localUp = PhysicComponent.getInstance().getVectorPool().obtain();
        private static final Matrix4 matrix = new Matrix4();

        private void render(Cubemap.CubemapSide side, ShapeRenderer shapes, ShaderProgram shader, float strength){
            render(side, shapes, shader, strength, exponent);
        }
        private void render(Cubemap.CubemapSide side, ShapeRenderer shapes, ShaderProgram shader, float strength, float exponent){
            shader.bind();
            shader.setUniformf("u_exponent", exponent);
            shader.setUniformf("u_ambient", color.r, color.g, color.b, 0f);
            shader.setUniformf("u_diffuse", color.r, color.g, color.b, strength);

            localDir.set(side.direction);
            localUp.set(side.up);

            // XXX patch
            if(side == Cubemap.CubemapSide.NegativeX || side == Cubemap.CubemapSide.PositiveX){
                localDir.x = -localDir.x;
            }

            matrix.setToLookAt(localDir, localUp).tra();
            localSunDir.set(direction).scl(-1, -1, 1).mul(matrix); // XXX patch again

            shader.setUniformf("u_direction", localSunDir);

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.rect(0, 0, 1, 1);
            shapes.end();
        }
    }

    private void renderLights(Cubemap.CubemapSide side, boolean blured){

        Gdx.gl.glEnable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE);

        for(CustomIDBuilder.Light light : lights){
            if(blured){
                light.render(side, sunShapes, sunShader, .5f, 1f);
            }else{
                light.render(side, sunShapes, sunShader, 1f);
            }
        }

        Gdx.gl.glDisable(GL30.GL_BLEND);
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
    }
}
