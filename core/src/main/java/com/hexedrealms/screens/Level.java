package com.hexedrealms.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.crashinvaders.vfx.effects.LevelsEffect;
import com.hexedrealms.Main;
import com.hexedrealms.components.*;
import com.hexedrealms.components.specialmeshes.DoorMesh;
import com.hexedrealms.configurations.*;
import com.hexedrealms.engine.*;
import com.hexedrealms.utils.savedata.LevelData;
import com.hexedrealms.weapons.Weapon;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.mgsx.gltf.scene3d.utils.LightUtils;
import org.apache.fury.util.GraalvmSupport;

import java.io.Serializable;
import java.security.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class Level implements Screen{

    private  SceneAsset mapModel;
    private Player player;
    private DecalComponent decalComponent;
    private ParticlesComponent particlesComponent;
    private BallisticComponent ballisticComponent;
    private FrameBuffer frameBuffer;
    private ShaderProgram darkShader;
    private SpriteBatch frameBatch;
    private Array<DoorMesh> doorMeshes;
    private AudioComponent audioComponent;
    private EntityComponent entityComponent;
    private OverlaysComponent overlaysComponent;
    private PhysicComponent physicComponent;
    private NPCComponent npcComponent;
    private LightingComponent lightingComponent;
    private DamageComponent damageComponent;
    private TriggerComponent triggerComponent;
    private SceneManager manager;
    private Scene scene;
    private NodeComposer composer;
    private ItemsComponent itemsComponent;
    private com.hexedrealms.utils.savedata.Level levelData;
    private SoundBuffer soundBuffer;
    public float multiplier;

    private CustomRenderableProvider renderableProvider;
    private static Level instance;
    public static Color AMBIENT_COLOR = new Color(0.6f, 0.6f, 0.6f, 1f);

    private VisUIComponent visUIComponent;
    private PostProcessorComponent postProcessorComponent;
    private WaterBatch waterBatch;
    private CloudsBatch cloudBatch;
    private boolean isPaused;
    public boolean isShowed;
    private int widthRender, heightRender;

    public int slotId, levelId;

    Level() {
        this.init();
    }

    Level(com.hexedrealms.utils.savedata.Level levelData) {
        this.levelData = levelData;
        this.init();
    }

    private void init(){

        initializeDimensions();
        initializePhysics();
        initializeBuffers();
        initializePlayer();
        initializeMap();
        initializeShaders();
        initializeBatches();
        soundBuffer = SoundLoader.load(Gdx.files.internal("audio/music/fail.wav"));

        System.gc();
        Gdx.gl.glFinish();

    }

    public void setDifficulty(int difficultyId) {
        switch(difficultyId) {
            case 0: // Легкая
                multiplier = 0.8f;
                break;
            case 1: // Нормальная
                multiplier = 1f;
                break;
            case 2: // Сложная
                multiplier = 1.2f;
                break;
            case 3: // Сложная
                multiplier = 1.4f;
                break;
        }
    }

    public void setLevelData(com.hexedrealms.utils.savedata.Level levelData){
        this.levelData = levelData;
        AMBIENT_COLOR.set(ResourcesLoader.convertColor(levelData.ambientColor));
        postProcessorComponent.setColorAmbient(AMBIENT_COLOR);
        postProcessorComponent.applyBrightness((Float) PostProcessorConfiguration.BRIGHTNESS.getValue());
        manager.environment.set(postProcessorComponent.getColorAttribute());
    }

    public static Level getInstance(com.hexedrealms.utils.savedata.Level levelData) {
        if(instance == null) {
            instance = new Level(levelData);
        }

        return instance;
    }

    public static Level getInstance() {
        if(instance == null)
            instance = new Level();

        return instance;
    }

    private void initializeDimensions() {
        widthRender = Gdx.graphics.getWidth();
        heightRender = Gdx.graphics.getHeight();
        visUIComponent = VisUIComponent.getInstance();
    }

    private void initializeAudio() {
        audioComponent = AudioComponent.getInstance();
        audioComponent.clear();
        audioComponent.loadMainMusic(levelData.ambientMusic);
        audioComponent.loadCombatMusic(levelData.fightMusic);

        if(audioComponent.targetState != null){
            audioComponent.setMusicState(audioComponent.targetState);
        }else {
            audioComponent.setMusicState(AudioComponent.MusicState.EXPLORATION);
        }
    }

    private void initializePhysics() {
        physicComponent = PhysicComponent.getInstance();
    }

    private void initializeBuffers() {
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, widthRender, heightRender, true, false);

        manager = new SceneManager();
    }

    private void initializeShaders() {
        Array<PointLightEx> lights = LightingComponent.getInstance().getPointLightArray();
        postProcessorComponent = PostProcessorComponent.getInstance();
        postProcessorComponent.setColorAmbient(AMBIENT_COLOR);
        postProcessorComponent.applyBrightness((Float) PostProcessorConfiguration.BRIGHTNESS.getValue());
        manager.environment.set(postProcessorComponent.getColorAttribute());
        manager.environment.set(postProcessorComponent.getFogAttribute());

        ShaderProvider colorShader = createShaderProvider();
        manager.setShaderProvider(colorShader);
        waterBatch = new WaterBatch(player, lights);
        cloudBatch = new CloudsBatch();
    }

    public void updateEnvironment(int id){
        PointLightEx pointLightEx = LightingComponent.getInstance().getPointLightArray().get(id);
//        waterBatch.updateEnvironment(pointLightEx, id);
    }

    private void initializePlayer() {
        player = new Player((Float) ControlsConfiguration.FOV.getValue(), 3000f);
        manager.setCamera(player.getCamera());
    }

    public Scene getScene() {
        return scene;
    }

    private void initializeMap() {
        doorMeshes = new Array<>();

        Camera camera = player.getCamera();

        overlaysComponent = OverlaysComponent.getInstance();
        lightingComponent = LightingComponent.getInstance();
        itemsComponent = ItemsComponent.getInstance();
        triggerComponent = TriggerComponent.getInstance();
        entityComponent = EntityComponent.getInstance(camera);
        decalComponent = DecalComponent.getInstance(camera);
        particlesComponent = ParticlesComponent.getInstance(camera);
        ballisticComponent = BallisticComponent.getInstance(camera);

        damageComponent = DamageComponent.getInstance();
        itemsComponent = ItemsComponent.getInstance();

        loadMapModel();
        initializeEnvironment();
        initializeCubemaps();

        player.initWeapons(manager.environment);
    }

    private void loadMapModel() {
        GLTFLoader loader = new GLTFLoader();

        String name = levelData.model.split("/")[1];

        mapModel = loader.load(Gdx.files.internal(levelData.model+name+".gltf"), true);
        mapModel.textures.forEach(texture -> {
            texture.setFilter(Texture.TextureFilter.MipMapNearestLinear, Texture.TextureFilter.MipMapNearestLinear);
            texture.setAnisotropicFilter(8F);
        });

        scene = new Scene(mapModel.scene);
        processNodes();
        Array<Renderable> renderables = new Array<>();
        Pool<Renderable> pool = new Pool<Renderable>() {
            @Override
            protected Renderable newObject() {
                return new Renderable();
            }
        };
        scene.getRenderables(renderables, pool);

        renderableProvider = new CustomRenderableProvider(renderables, pool);

        manager.addScene(scene, false);
        manager.getRenderableProviders().clear();
        manager.getRenderableProviders().add(renderableProvider);

        Node node = scene.cameras.keys().next();
        scene.modelInstance.nodes.removeValue(node, true);
        player.initBody(node.globalTransform);

    }

    private void initializeEnvironment() {
        physicComponent.addCollisionObject(scene.modelInstance);
    }

    private void processNodes() {
        composer = new NodeComposer(scene.modelInstance.nodes.iterator(), this);
        composer.handlingNodes();

        npcComponent = NPCComponent.getInstance(composer.getGraph(), player);

        composer.cleanUp();
    }

    private Cubemap loadCubemapFromDirectory(String directoryPath) {
        // Проверяем, существует ли путь
        if (!Gdx.files.internal(directoryPath).exists()) {
            Gdx.app.error("CubemapLoader", "Directory not found: " + directoryPath);
            return null;
        }

        try {
            // Загружаем каждую грань кубемапа
            Pixmap right = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_right.png"));
            Pixmap left = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_left.png"));
            Pixmap up = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_up.png"));
            Pixmap down = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_down.png"));
            Pixmap back = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_back.png"));
            Pixmap front = new Pixmap(Gdx.files.internal(directoryPath + "skybox/cube_front.png"));

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

        Cubemap cubemap = loadCubemapFromDirectory(levelData.model);

        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);

        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        Cubemap diffuseCubemap = iblBuilder.buildIrradianceMap(20);
        Cubemap specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();


        Texture brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));
        manager.environment.set(new BlendingAttribute(true, 1f));
        manager.environment.set(new FloatAttribute(FloatAttribute.AlphaTest));
        manager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        manager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        manager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        SceneSkybox skybox = new SceneSkybox(cubemap);
        manager.setSkyBox(skybox);
    }

    private ShaderProvider createShaderProvider() {
        LightUtils.LightsInfo info = LightUtils.getLightsInfo(new LightUtils.LightsInfo(), manager.environment);
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.vertexShader = Gdx.files.internal("shaders/cel.main_v.glsl").readString();
        config.fragmentShader = Gdx.files.internal("shaders/cel.main_f.glsl").readString();
        config.numPointLights = info.pointLights;
        config.defaultCullFace = GL30.GL_BACK;

        return PBRShaderProvider.createDefault(config);
    }

    public SceneAsset getMapModel() {
        return mapModel;
    }

    public SceneManager getManager() {
        return manager;
    }

    private void initializeBatches() {
        darkShader = new ShaderProgram(Gdx.files.internal("shaders/post_filter_underworld.vert"), Gdx.files.internal("shaders/post_filter_underworld.frag"));
        frameBatch = new SpriteBatch();
    }

    private void captureScene(float delta) {
        Frustum frustum = player.getCamera().frustum;
        Gdx.gl.glViewport(0,0, widthRender, heightRender);

        Camera camera = player.getCamera();

        frameBuffer.begin();
        clearScreen();

        manager.renderColors();
        cloudBatch.render(delta, camera, isPaused);
        waterBatch.render(delta, camera, isPaused, lightingComponent.getPointLightArray());
        particlesComponent.render(manager.getBatch(), manager.environment, delta, isPaused);
        decalComponent.render(delta, frustum);
        entityComponent.render(camera, delta * 15f, isPaused);
        if(isPaused && player.getPlayerBody().getCurrentHealth() <= 0)
            overlaysComponent.uploadOverlay(overlaysComponent.pain);
        overlaysComponent.render(delta);

        frameBuffer.end();
    }

    private void clearScreen() {
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT | GL30.GL_STENCIL_BUFFER_BIT);
        Gdx.gl30.glClearDepthf(1f);
    }

    @Override
    public void show() {
        if(!isShowed){
            initializeAudio();
            isShowed = true;
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 60f);

        if(!isPaused) {
            if (Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.FAST_SAVE.getValue())) {
                LoaderComponent loaderComponent = new LoaderComponent();
                loaderComponent.saveGame("slot" + slotId);
                player.getHUD().setMessage("Сохранение создано", Color.WHITE);
            }

            if (Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.FAST_LOAD.getValue())) {
                dispose();
                LoaderStage stage = new LoaderStage("textures/backgrounds/e1m1.png", slotId, 0, false);
                Main.getInstance().setScreen(stage);
                return;
            }
        }

        if(player.getPlayerBody().getCurrentHealth() <= 0 && !isPaused){
            audioComponent.clear();
            soundBuffer.play(AudioConfiguration.SOUND.getValue());
            handlePause();
        }

        updateGameObjects(delta);
        player.update(delta, isPaused);

        captureScene(delta);
        renderPostProcessing(delta);

        if(!isPaused) {
            handleInput();
            return;
        }

        if(GUIComponent.getInstance().isEmpty()) {
            isPaused = false;
            Gdx.input.setInputProcessor(player.getInputMultiplexer());
            Gdx.input.setCursorCatched(true);
            player.ignoreNextMouseDelta = true;
            resume();
        }
        GUIComponent.getInstance().render(delta);
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyPressed(Input.Keys.SYM) ||
            Gdx.graphics.getWidth() == 0 || Gdx.graphics.getHeight() == 0) {
            handlePause();
        }
    }

    private void handlePause(){
        PauseStage stage = (PauseStage) GUIComponent.getInstance().getMenuStage(PauseStage.class);
//        stage.setFailed(player.getEnemy().getCurrentHealth() <= 0);
        GUIComponent.getInstance().putStage(stage);
        pause();
    }

    private void updateAudio() {
        audioComponent.getAudio().getListener().setPosition(player.getCamera().position);
        audioComponent.getAudio().getListener().setOrientation(player.getCamera());
        audioComponent.update();
    }

    private void updateGameObjects(float delta) {
        updateAudio();
        if(isPaused) return;

        physicComponent.updateSimulation(delta);
        ballisticComponent.Update(delta);
        manager.update(delta);
        lightingComponent.update(delta);
        npcComponent.update(delta);
        triggerComponent.update(player);

        for(DoorMesh doorMesh : doorMeshes) {
            doorMesh.update(delta);
        }

    }

    private void renderPostProcessing(float delta) {
        postProcessorComponent.cleanUpBuffers();
        postProcessorComponent.beginCapture(delta);

        frameBatch.enableBlending();
        frameBatch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        frameBatch.begin();
        frameBatch.draw(frameBuffer.getColorBufferTexture(),0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0,1f, 1f );
        frameBatch.end();

        postProcessorComponent.endCapture();
        postProcessorComponent.applyEffects();
        postProcessorComponent.renderToScreen();
        player.renderEntity(delta, isPaused);
        visUIComponent.render(delta);

        if(Gdx.input.isKeyPressed(Input.Keys.Q)){
            physicComponent.render(player.getCamera());
        }
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void resize(int width, int height) {
        if(width > 0 && height > 0) {
            float quality = Quality.values()[(int) PostProcessorConfiguration.QUALITY.getValue()].getValue();
            widthRender = (int) (width * quality);
            heightRender = (int) (height  * quality);
            if (frameBuffer != null) {
                frameBuffer.dispose();
            }
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, widthRender, heightRender, true, true);

            // Обновляем постпроцессинг
            if (postProcessorComponent != null) {
                postProcessorComponent.resize(widthRender, heightRender);
            }

            // Обновляем камеру
            if (player != null && player.getCamera() != null) {
                player.getCamera().viewportWidth = widthRender;
                player.getCamera().viewportHeight = heightRender;
                player.getCamera().update();

                player.getHUD().resize(width, height);
            }

            overlaysComponent.resize(width, height);

            manager.updateViewport(widthRender, heightRender);
            visUIComponent.resize(widthRender, heightRender);
            GUIComponent.getInstance().resize(width, height);
        }
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        handlePause();
    }

    @Override
    public void dispose() {

        if (itemsComponent != null) {
            itemsComponent.dispose();
            itemsComponent = null;
        }

        // Dispose all components in reverse order of initialization
        if (player != null) {
            player.dispose();
            player = null;
        }

        if (frameBuffer != null) {
            frameBuffer.dispose();
            frameBuffer = null;
        }

        if (postProcessorComponent != null) {
            postProcessorComponent.dispose();
            postProcessorComponent = null;
        }

        if (lightingComponent != null) {
            lightingComponent.dispose();
            lightingComponent = null;
        }

        if (frameBatch != null) {
            frameBatch.dispose();
            frameBatch = null;
        }

        if (decalComponent != null) {
            decalComponent.dispose();
            decalComponent = null;
        }

        if (particlesComponent != null) {
            particlesComponent.dispose();
            particlesComponent = null;
        }

        if (ballisticComponent != null) {
            ballisticComponent.dispose();
            ballisticComponent = null;
        }

        if (npcComponent != null) {
            npcComponent.dispose();
            npcComponent = null;
        }

        if (entityComponent != null) {
            entityComponent.dispose();
            entityComponent = null;
        }

        if (overlaysComponent != null) {
            overlaysComponent.dispose();
            overlaysComponent = null;
        }

        if (damageComponent != null) {
            damageComponent.dispose();
            damageComponent = null;
        }

        if (triggerComponent != null) {
            triggerComponent.dispose();
            triggerComponent = null;
        }

        if (visUIComponent != null) {
            visUIComponent.dispose();
            visUIComponent = null;
        }

        if (waterBatch != null) {
            waterBatch.dispose();
            waterBatch = null;
        }

        if (cloudBatch != null) {
            cloudBatch.dispose();
            cloudBatch = null;
        }

        if (manager != null) {
            manager.environment.clear();
            manager.dispose();
            manager = null;
        }

        if (scene != null) {
            scene.modelInstance.model.dispose();
            scene = null;
        }

        if (mapModel != null) {
            mapModel.dispose();
            mapModel = null;
        }

        if (darkShader != null) {
            darkShader.dispose();
            darkShader = null;
        }

        if (doorMeshes != null) {
            for (DoorMesh doorMesh : doorMeshes) {
                doorMesh.dispose();
            }
            doorMeshes.clear();
            doorMeshes = null;
        }

        if (renderableProvider != null) {
            renderableProvider.dispose();
            renderableProvider = null;
        }

        if (audioComponent != null) {
            audioComponent.dispose();
            audioComponent = null;
        }

        if (physicComponent != null) {
            physicComponent.dispose();
            physicComponent = null;
        }

        // Reset singleton instance
        instance = null;

        // Force garbage collection
        System.gc();
    }

    public Array<DoorMesh> getDoorMeshes() {
        return doorMeshes;
    }
}
