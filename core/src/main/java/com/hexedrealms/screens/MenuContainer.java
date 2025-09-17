package com.hexedrealms.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.components.*;
import com.hexedrealms.components.specialmeshes.CloudMesh;
import com.hexedrealms.configurations.GraphicConfiguration;
import com.hexedrealms.configurations.PostProcessorConfiguration;
import com.hexedrealms.configurations.Quality;
import com.hexedrealms.engine.*;
import net.mgsx.gltf.data.scene.GLTFNode;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.lights.SpotLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.LightUtils;

public class MenuContainer implements Screen {

    // Constants
    private static final float CAMERA_NEAR = 0.1f;
    private static final float CAMERA_FAR = 3000f;
    private static final float CAMERA_MOVE_SPEED = 0.1f;

    // Camera animation variables
    private final Vector3 initialCameraPosition = new Vector3();
    private final Vector3 initialLookAtPoint = new Vector3(-113f, -435f, 160f);
    private float cameraAngle = 0f;
    private float cameraRadius = 5f;
    private float cameraSpeed = 1f;
    private float cameraHeight = 2f;
    private float cameraAnimationAmplitude = 2f;

    // Components
    private final MenuStage mainMenu;
    private final Texture texture;
    private final SpriteBatch spriteBatch;
    private final ShaderProgram shaderProgram;
    private final SceneManager manager;
    private FrameBuffer frameBuffer;
    private final PerspectiveCamera camera;

    // State
    private Scene scene;
    private CloudsBatch batch;
    private SceneAsset mapModel;
    private PostProcessorComponent postProcessorComponent;
    private EntityComponent entityComponent;
    private ParticlesComponent particlesComponent;
    private float stateTime;
    private float width, height;
    private boolean isPaused;

    // Color animation
    private final ColorAnimator cloudColorAnimator = new ColorAnimator();

    public MenuContainer() {
        batch = new CloudsBatch();
        batch.color.set(cloudColorAnimator.getColor()); // Initialize with animated color

        // Initialize display settings
        Graphics.DisplayMode mode = SettingsComponent.getInstance()
            .displayModes.get((Integer) GraphicConfiguration.DIMENSION.getValue());
        width = mode.width;
        height = mode.height;

        AudioComponent.getInstance().loadMainMusic("audio/music/mainmenu.mp3");
        AudioComponent.getInstance().setMusicState(AudioComponent.MusicState.EXPLORATION);

        // Initialize rendering components
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int) width, (int) height, true, false);
        manager = new SceneManager();
        shaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/blink.vert"),
            Gdx.files.internal("shaders/blink.frag")
        );

        // Initialize GUI
        mainMenu = GUIComponent.getInstance().getMenuStage(MenuStage.class);
        GUIComponent.getInstance().putStage(mainMenu);

        // Initialize texture
        texture = new Texture(Gdx.files.internal("textures/splash/logo-game.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setAnisotropicFilter(16f);

        // Initialize sprite batch
        spriteBatch = new SpriteBatch();

        // Initialize camera
        camera = new PerspectiveCamera(90f, width, height);
        camera.near = CAMERA_NEAR;
        camera.far = CAMERA_FAR;

        loadBackgroundMap();
    }

    private class ColorAnimator {
        private final Color colorA = new Color(0.4f, 0.1f, 0.1f, 0.5f);
        private final Color colorB = new Color(0.1f, 0.1f, 0.1f, 1f);
        private final Color currentColor = new Color();
        private float duration = 5f; // Full cycle duration
        private float time = 0f;

        public void update(float delta) {
            time += delta;
            // Smooth ping-pong animation using sine wave
            float t = (MathUtils.sin(time * (MathUtils.PI2 / duration)) + 1f) / 2f;

            currentColor.set(
                MathUtils.lerp(colorA.r, colorB.r, t),
                MathUtils.lerp(colorA.g, colorB.g, t),
                MathUtils.lerp(colorA.b, colorB.b, t),
                MathUtils.lerp(colorA.a, colorB.a, t)
            );
        }

        public Color getColor() {
            return currentColor;
        }
    }

    private ShaderProvider createShaderProvider() {
        LightUtils.LightsInfo info = LightUtils.getLightsInfo(new LightUtils.LightsInfo(), manager.environment);
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.vertexShader = Gdx.files.internal("shaders/cel.main_v.glsl").readString();
        config.fragmentShader = Gdx.files.internal("shaders/cel.main_f.glsl").readString();
        config.numPointLights = info.pointLights;

        return PBRShaderProvider.createDefault(config);
    }

    private void loadBackgroundMap() {
        GLTFLoader loader = new GLTFLoader();
        entityComponent = EntityComponent.getInstance(camera);
        particlesComponent = ParticlesComponent.getInstance(camera);

        // Load model
        mapModel = loader.load(Gdx.files.internal("intro/intro.gltf"), true);
        mapModel.textures.forEach(texture -> {
            texture.setFilter(Texture.TextureFilter.MipMapNearestLinear, Texture.TextureFilter.MipMapNearestLinear);
            texture.setAnisotropicFilter(16F);
        });

        // Setup scene
        scene = new Scene(mapModel.scene);
        processNodes();
        manager.addScene(scene, false);

        // Setup camera
        Node node = scene.cameras.keys().next();
        scene.modelInstance.nodes.removeValue(node, true);
        camera.position.set(node.translation);
        camera.rotate(node.rotation);
        camera.update();

        // Save initial camera position and setup look at point
        initialCameraPosition.set(camera.position);
        initialLookAtPoint.set(0, camera.position.y, 0);

        // Setup shaders and environment
        ShaderProvider colorShader = createShaderProvider();
        manager.setShaderProvider(colorShader);
        manager.setCamera(camera);
        initializeCubemaps();
    }

    private void processNodes() {
        Array.ArrayIterator<Node> iterator = scene.modelInstance.nodes.iterator();

        while (iterator.hasNext()) {
            Node nodeBone = iterator.next();
            GLTFNode node = findMatchingGLTFNode(nodeBone);

            if (node != null && processNode(node, nodeBone)) {
                iterator.remove();
            }
        }
    }

    private boolean processNode(GLTFNode node, Node nodeBone) {
        if (isDecorationNode(node)) {
            processDecorationNode(node, nodeBone);
            return true;
        } else if (isLightNode(node)) {
            processLightNode(node);
            return true;
        } else if (isParticleNode(node)) {
            processParticleNode(node);
            return true;
        } else if (isCloud(node)) {
            processingCloudNode(nodeBone);
            return true;
        }
        return false;
    }

    private void processLightNode(GLTFNode node) {
        Node lightNode = mapModel.scene.model.getNode(node.name);
        var light = mapModel.scene.lights.get(lightNode);

        if (light instanceof PointLightEx) {
            PointLightEx pointLightEx = LightingComponent.getInstance().getPointLightPool().obtain();
            pointLightEx.set((PointLightEx) light);
            pointLightEx.setPosition(lightNode.translation);
            processPointLight(node, pointLightEx);
        } else if (light instanceof SpotLightEx) {
            manager.environment.add((SpotLightEx) light);
        }
    }

    private void processPointLight(GLTFNode node, PointLightEx pointLightEx) {
        JsonValue value = node.extras != null ? node.extras.value : null;
        boolean isFlick = value != null && value.getBoolean("flick", false);
        boolean isLinear = value != null && value.getBoolean("linear", false);

        if (isFlick) {
            handleFlickLight(pointLightEx, value);
        } else if (isLinear) {
            handleLinearLight(pointLightEx, value);
        } else {
            manager.environment.add(pointLightEx);
            LightingComponent.getInstance().putLight(pointLightEx);
        }
    }

    private void handleFlickLight(PointLightEx pointLightEx, JsonValue value) {
        DynamicLight dynamicLight = new DynamicLight(
            value.getFloat("min"),
            value.getFloat("speed"),
            pointLightEx.intensity,
            value.getFloat("speedchange")
        );
        dynamicLight.set(pointLightEx);
        LightingComponent.getInstance().putLight(dynamicLight);
        manager.environment.add(dynamicLight);
    }

    private void handleLinearLight(PointLightEx pointLightEx, JsonValue value) {
        LinearLight linearLight = new LinearLight(
            value.getFloat("min"),
            value.getFloat("speed"),
            pointLightEx.intensity,
            0f,
            value.getFloat("time")
        );
        linearLight.set(pointLightEx);
        LightingComponent.getInstance().putLight(linearLight);
        manager.environment.add(linearLight);
    }

    private void processParticleNode(GLTFNode node) {
        if (node.extras != null) {
            JsonValue jsonValue = node.extras.value;
            String name = jsonValue.getString("name");
            ParticleEffect effect = ParticlesComponent.getInstance(null).findEffect(name);

            if (effect != null) {
                effect.init();
                effect.start();
                effect.translate(new Vector3(node.translation));
                ParticlesComponent.getInstance(null).addEffect(effect);
            }
        }
    }

    private NodeComposer.DecorationData extractDecorationData(GLTFNode node) {
        NodeComposer.DecorationData data = new NodeComposer.DecorationData();
        if (node.extras != null) {
            JsonValue value = node.extras.value;
            data.isRigid = value.getBoolean("rigid");
        }
        return data;
    }

    private void processDecorationNode(GLTFNode node, Node nodeBone) {
        NodeComposer.DecorationData data = extractDecorationData(node);
        entityComponent.addNodeEntity(nodeBone, false, false);
    }

    private boolean isDecorationNode(GLTFNode node) {
        return node.name.toLowerCase().contains("decoration");
    }

    private boolean isParticleNode(GLTFNode node) {
        return node.name.toLowerCase().contains("particle");
    }

    private boolean isLightNode(GLTFNode node) {
        return node.name.toLowerCase().contains("light");
    }

    private GLTFNode findMatchingGLTFNode(Node nodeBone) {
        for (GLTFNode node : mapModel.data.nodes) {
            if (node.name.equals(nodeBone.id)) {
                return node;
            }
        }
        return null;
    }

    private Cubemap loadCubemapFromDirectory(String directoryPath) {
        // Проверяем, существует ли путь
        if (!Gdx.files.internal(directoryPath).exists()) {
            Gdx.app.error("CubemapLoader", "Directory not found: " + directoryPath);
            return null;
        }

        try {
            // Загружаем каждую грань кубемапа
            Pixmap right = new Pixmap(Gdx.files.internal(directoryPath + "/cube_right.png"));
            Pixmap left = new Pixmap(Gdx.files.internal(directoryPath + "/cube_left.png"));
            Pixmap up = new Pixmap(Gdx.files.internal(directoryPath + "/cube_up.png"));
            Pixmap down = new Pixmap(Gdx.files.internal(directoryPath + "/cube_down.png"));
            Pixmap back = new Pixmap(Gdx.files.internal(directoryPath + "/cube_back.png"));
            Pixmap front = new Pixmap(Gdx.files.internal(directoryPath + "/cube_front.png"));

            // Создаем Cubemap
            Cubemap cubemap = new Cubemap(
                right, left,
                up, down,
                back, front
            );

            // Освобождаем ресурсы Pixmap
            right.dispose();
            left.dispose();
            up.dispose();
            down.dispose();
            back.dispose();
            front.dispose();

            return cubemap;
        } catch (Exception e) {
            Gdx.app.error("CubemapLoader", "Failed to load cubemap: " + e.getMessage());
            return null;
        }
    }

    private void initializeCubemaps() {
        String pathSkybox = "levels/CityOnFire/skybox";

        Cubemap cubemap = loadCubemapFromDirectory(pathSkybox);

        manager.environment.set(new BlendingAttribute(true, 1f));
        manager.environment.set(new FloatAttribute(FloatAttribute.AlphaTest));

        SceneSkybox skybox = new SceneSkybox(cubemap);
        manager.setSkyBox(skybox);

        postProcessorComponent = PostProcessorComponent.getInstance();
        postProcessorComponent.setColorAmbient(new Color(0.3f, 0.3f, 0.3f, 0.4f));
        postProcessorComponent.applyBrightness((Float) PostProcessorConfiguration.BRIGHTNESS.getValue());

        manager.environment.set(postProcessorComponent.getColorAttribute());
    }

    private boolean isCloud(GLTFNode node) { return node.name.toLowerCase().contains("cloud");}

    private void processingCloudNode(Node nodeBone) {
        CloudsBatch.getCloudsMeshes().add(new CloudMesh(nodeBone));
    }

    @Override
    public void render(float delta) {
        // Update cloud color animation
        cloudColorAnimator.update(delta);
        batch.color.set(cloudColorAnimator.getColor());

        AudioComponent.getInstance().update();
        stateTime += delta;
        handleCameraMovement();
        camera.update();
        manager.update(delta);
        LightingComponent.getInstance().update(delta);

        // Render to framebuffer
        frameBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        manager.renderColors();
        batch.render(delta, camera, isPaused);
        entityComponent.render(camera, delta, isPaused);
        particlesComponent.render(manager.getBatch(), manager.environment, delta * 2f, isPaused);
        frameBuffer.end();

        // Post-processing
        postProcessorComponent.cleanUpBuffers();
        postProcessorComponent.beginCapture(delta);

        // Render framebuffer to screen
        spriteBatch.enableBlending();
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        spriteBatch.begin();
        spriteBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, width, height, 0, 0, 1f, 1f);
        spriteBatch.end();

        // Finalize post-processing
        postProcessorComponent.endCapture();
        postProcessorComponent.applyEffects();
        postProcessorComponent.renderToScreen();

        spriteBatch.setShader(shaderProgram);
        shaderProgram.bind();
        shaderProgram.setUniformf("u_time", stateTime);

        spriteBatch.begin();
        spriteBatch.draw(texture, 50, height - height * 0.2f - 50, width * 0.17f, height * 0.2f);
        spriteBatch.end();
        spriteBatch.setShader(null);

        // Render GUI
        GUIComponent.getInstance().render(delta);

        isPaused = Gdx.graphics.getWidth() == 0 || Gdx.graphics.getHeight() == 0 || Gdx.input.isKeyPressed(Input.Keys.SYM);
    }

    private void handleCameraMovement() {
        cameraAngle += Gdx.graphics.getDeltaTime() * cameraSpeed;
        float x = initialCameraPosition.x + (float)Math.sin(cameraAngle) * cameraAnimationAmplitude;
        float y = initialCameraPosition.y;
        float z = initialCameraPosition.z;
        camera.position.set(x, y, z);
    }

    @Override
    public void resize(int width, int height) {
        if(width > 0 && height > 0) {
            float quality = Quality.values()[(int) PostProcessorConfiguration.QUALITY.getValue()].getValue();
            this.width = (int) (width * quality);
            this.height = (int) (height * quality);
            if (frameBuffer != null) {
                frameBuffer.dispose();
            }
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int) this.width, (int) this.height, true, true);

            if (postProcessorComponent != null) {
                postProcessorComponent.resize((int) this.width, (int) this.height);
            }

            camera.viewportWidth = this.width;
            camera.viewportHeight = this.height;

            manager.updateViewport((int) this.width, (int) this.height);
            GUIComponent.getInstance().resize(width, height);
        }
    }

    @Override
    public void show() {}

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
        isPaused = false;
    }

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
        if (mapModel != null) {
            mapModel.dispose();
        }
        if (texture != null) {
            texture.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (shaderProgram != null) {
            shaderProgram.dispose();
        }
    }
}
