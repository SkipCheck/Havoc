package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.hexedrealms.components.bulletbodies.*;
import com.hexedrealms.configurations.PlayerConfiguration;
import com.hexedrealms.utils.damage.Enemy;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PhysicComponent implements Disposable {
    public static final short GROUND_FLAG = 1 << 8;
    public static final short OBJECT_FLAG = 1 << 9;
    public static final short ALL_FLAG = -1;
    public static final float FIXED_TIMESTEP = 1f / 60f;
    public static final Vector3 GRAVITY = new Vector3(0, -90f, 0);

    public final static short CF_OCCLUDER_OBJECT = 512;
    public final static short CF_GHOST_OBJECT = 256;

    private static PhysicComponent instance;

    private btDynamicsWorld world;
    private Pool<btRigidBody> bodiesPool;
    private btCollisionConfiguration config;
    private btDispatcher dispatcher;
    private btDbvtBroadphase broadphase;
    private btConstraintSolver solver;
    private DebugDrawer debugDrawer;
    private btRigidBody sensorPlayer;
    private PlayerBody playerBody;
    private SensorListener contactListener;
    private PlayerMotion playerMotion;
    private CustomCallback callback;
    private SpriteBatch spriteBatch;
    private Array<NPCPartBody> npcPartBodies;
    private HashMap<btCollisionObject, Vector3> collisions;

    private Pool<ClosestRayResultCallback> raycastCallbackPool;
    private Pool<ClosestRayResultCallback> npcCallBackPool;
    private Pool<Vector3> vectorPool;
    private Pool<NPCPartBody> npcPartBodyPool;

    private float previousAngle;
    private boolean isOnGround;
    private int groundContactCount = 0;

    private PhysicComponent(){
        this.init();

        vectorPool = new Pool<>(20, 100) {
            @Override
            protected Vector3 newObject() {
                return new Vector3();
            }

            @Override
            public void reset(Vector3 object) {
                object.set(0,0,0);
            }
        };

        raycastCallbackPool = new Pool<>(20, 200) {
            @Override
            protected ClosestRayResultCallback newObject() {
                return new ClosestRayResultCallback(
                    vectorPool.obtain(),
                    vectorPool.obtain()
                ){
                    @Override
                    public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
                        btCollisionObject obj = rayResult.getCollisionObject();

                        if(obj.getCollisionFlags() == CF_GHOST_OBJECT + 1 || obj instanceof WaterBody
                        || obj instanceof BladeBody || obj instanceof ZoneBody || obj instanceof OcclusionBody || obj instanceof PlayerBody || obj instanceof NocallbackBody)
                            return 1f;

                        return super.addSingleResult(rayResult, normalInWorldSpace);
                    }
                };
            }

            @Override
            public void reset(ClosestRayResultCallback object) {
                object.setCollisionObject(null);
                object.setClosestHitFraction(1f);
                object.setRayFromWorld(vectorPool.obtain());
                object.setRayToWorld(vectorPool.obtain());
            }
        };

        npcCallBackPool = new Pool<>(20, 200) {
            @Override
            protected ClosestRayResultCallback newObject() {
                return new ClosestRayResultCallback(
                    vectorPool.obtain(),
                    vectorPool.obtain()
                ){
                    @Override
                    public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
                        btCollisionObject obj = rayResult.getCollisionObject();
                        if(obj.getCollisionFlags() == CF_GHOST_OBJECT + 1 || obj instanceof WaterBody || obj instanceof NPCBody)
                            return 1f;

                        return super.addSingleResult(rayResult, normalInWorldSpace);
                    }
                };
            }

            @Override
            public void reset(ClosestRayResultCallback object) {
                object.setCollisionObject(null);
                object.setClosestHitFraction(1f);
                object.setRayFromWorld(vectorPool.obtain());
                object.setRayToWorld(vectorPool.obtain());
            }
        };

        npcPartBodyPool = new Pool<NPCPartBody>() {
            @Override
            protected NPCPartBody newObject() {
                btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(10.0F, null, null);
                return new NPCPartBody(info);
            }
        };


        npcPartBodies = new Array<>();
        collisions = new HashMap<>();

        spriteBatch = new SpriteBatch();
    }

    public void init() {
        Bullet.init();

        bodiesPool = new Pool<btRigidBody>() {
            @Override
            protected btRigidBody newObject() {
                return createBoxBody(null);
            }
        };
        callback = new CustomCallback();

        config = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(config);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        world = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, config);
        world.setGravity(GRAVITY);

        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        world.setDebugDrawer(debugDrawer);

        contactListener = new SensorListener();
    }

    public static PhysicComponent getInstance(){
        if (instance == null) {
            synchronized (PhysicComponent.class) {
                if (instance == null) {
                    instance = new PhysicComponent();
                }
            }
        }
        return instance;
    }

    public Pool<ClosestRayResultCallback> getRaycastCallbackPool() {
        return raycastCallbackPool;
    }

    public Pool<ClosestRayResultCallback> getRaycastNpcPool() {
        return npcCallBackPool;
    }

    public CustomCallback getCallback(){
        return callback;
    }

    public void disposeRigidBody(btRigidBody body){
        if (body == null) return;

        world.removeRigidBody(body);
        world.removeCollisionObject(body);

        btCollisionShape collisionShape = body.getCollisionShape();
        if (collisionShape != null) {
            collisionShape.dispose();
        }

        if(body instanceof NPCPartBody){
            npcPartBodies.removeValue((NPCPartBody) body, true);
            npcPartBodyPool.free((NPCPartBody) body);

            return;
        }

        body.dispose();
    }

    public void addDynamicEntity(Vector3 position, float weight, Array<TextureAtlas.AtlasRegion> regions, String name){
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = random.nextInt(5, 11);
        float [] weights = {-weight * 0.7f, weight * 0.7f};

        for(int i = 0; i < count; i++) {
            TextureRegion region = regions.get(random.nextInt(0, regions.size));

            float height = region.getRegionHeight() * 0.05f;
            float width = region.getRegionWidth() * 0.05f;
            float depth = height / width;

            ParticleEffect effect = ParticlesComponent.getInstance(null).findEffect(name);
            effect.init();
            effect.start();
            ParticlesComponent.getInstance(null).addEffect(effect);

            btBoxShape collisionShape = new btBoxShape(vectorPool.obtain().set(width, height, depth));

            NPCPartBody npcPartBody = npcPartBodyPool.obtain();
            npcPartBody.init(collisionShape, region, position.add(weights[random.nextInt(0, weights.length)], 0, weights[random.nextInt(0, weights.length)]), effect, width, height);
            npcPartBodies.add(npcPartBody);
            world.addRigidBody(npcPartBody);

            npcPartBody.applyCentralImpulse(vectorPool.obtain().set(
                    random.nextFloat(-1f, 1f) ,
                    random.nextFloat(-1f, 1f),
                    random.nextFloat(-1f, 1f)
                )
                .scl(1000f));
        }
    }

    public btRigidBody createSphereShape(float radius){
        btSphereShape sphereShape = new btSphereShape(radius);

        btRigidBody.btRigidBodyConstructionInfo info =
            new btRigidBody.btRigidBodyConstructionInfo(0f, null, sphereShape);

        btRigidBody body = new btRigidBody(info);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        body.setCustomDebugColor(vectorPool.obtain().set(0.9f, 0.1f, 0.9f));
        return body;
    }

    public void updateSimulation(float delta) {
        for(NPCPartBody npcPartBody : npcPartBodies)
            npcPartBody.update(delta);

        world.stepSimulation(delta, 0, 1f / 60f);
    }

    public btDbvtBroadphase getBroadphase() {
        return broadphase;
    }

    public Pool<btRigidBody> getPool(){
        return bodiesPool;
    }

    public void render(PerspectiveCamera camera) {

        debugDrawer.begin(camera);
        world.debugDrawWorld();
        debugDrawer.end();
    }

    public DoorBody addDoorObject(Node node) {
        // 1. Создаем convex hull shape на основе всех частей ноды
        btConvexHullShape convexShape = new btConvexHullShape();

        // Перебираем все части ноды для сбора вершин
        for (NodePart part : node.parts) {
            Mesh mesh = part.meshPart.mesh;
            VertexAttributes attributes = mesh.getVertexAttributes();

            // Находим offset для позиций вершин
            int positionOffset = 0;
            for (VertexAttribute attribute : attributes) {
                if (attribute.usage == VertexAttributes.Usage.Position) {
                    positionOffset = attribute.offset / 4; // в float-ах
                    break;
                }
            }

            // Получаем все вершины меша
            float[] vertices = new float[mesh.getNumVertices() * attributes.vertexSize / 4];
            mesh.getVertices(vertices);

            // Добавляем точки в convex hull
            for (int i = 0; i < vertices.length; i += attributes.vertexSize / 4) {
                float x = vertices[i + positionOffset];
                float y = vertices[i + positionOffset + 1];
                float z = vertices[i + positionOffset + 2];
                convexShape.addPoint(new Vector3(x, y, z));
            }
        }

        // 2. Оптимизируем форму (уменьшаем количество вершин)
        btShapeHull hullOptimizer = new btShapeHull(convexShape);
        hullOptimizer.buildHull(convexShape.getMargin());
        btConvexHullShape optimizedShape = new btConvexHullShape(hullOptimizer);

        // 3. Настройки rigid body
        btRigidBody.btRigidBodyConstructionInfo info =
            new btRigidBody.btRigidBodyConstructionInfo(0.0F, null, optimizedShape);

        DoorBody doorBody = new DoorBody(info);

        // 4. Настройки коллизии
        doorBody.setCollisionFlags(
            doorBody.getCollisionFlags() |
                btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK |
                btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
        );

        // 5. Настройки CCD
        doorBody.setContactProcessingThreshold(0.0f);
        doorBody.setCcdMotionThreshold(0.05f);
        doorBody.setCcdSweptSphereRadius(0.25f);

        // 6. Физические свойства
        doorBody.setRestitution(0.2f);
        doorBody.setFriction(0.9f);

        // 7. Визуализация
        doorBody.setCustomDebugColor(vectorPool.obtain().set(0.1f, 0.7f, 0.5f));

        // 8. Добавление в мир
        world.addCollisionObject(
            doorBody,
            (short) btBroadphaseProxy.CollisionFilterGroups.StaticFilter,
            (short) (btBroadphaseProxy.CollisionFilterGroups.AllFilter)
        );

        // 9. Установка трансформации
        doorBody.setWorldTransform(node.globalTransform);

        // 10. Очистка
        convexShape.dispose();
        hullOptimizer.dispose();
        info.dispose();

        return doorBody;
    }

    public BladeBody configureBlade(ModelInstance blade){
        btConvexHullShape hullShape = new btConvexHullShape();

        for (Node node : blade.nodes) {
            for (NodePart nodePart : node.parts) {
                Mesh mesh = nodePart.meshPart.mesh;
                VertexAttributes attributes = mesh.getVertexAttributes();

                // Определяем offset для координат
                int positionOffset = getPositionOffset(attributes);

                float[] vertices = new float[mesh.getNumVertices() * attributes.vertexSize / 4];
                mesh.getVertices(vertices);

                for (int i = 0; i < vertices.length; i += attributes.vertexSize / 4) {
                    float x = vertices[i + positionOffset];
                    float y = vertices[i + positionOffset + 1];
                    float z = vertices[i + positionOffset + 2];

                    hullShape.addPoint(new Vector3(x, y, z));
                }
            }
        }

        btRigidBody.btRigidBodyConstructionInfo info =
            new btRigidBody.btRigidBodyConstructionInfo(0f, null, hullShape);

        BladeBody body = new BladeBody(info);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        body.setCustomDebugColor(vectorPool.obtain().set(0.9f, 0.1f, 0.9f));

        return body;
    }

    private int getPositionOffset(VertexAttributes attributes) {
        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attribute = attributes.get(i);
            if (attribute.usage == VertexAttributes.Usage.Position) {
                return attribute.offset / 4;
            }
        }
        throw new IllegalArgumentException("No position attribute found");
    }

    public void removeRigidBody(btCollisionObject object){
        world.removeCollisionObject(object);
    }

    public HashMap<btCollisionObject, Vector3> consumeCollisionBomb(btCollisionObject object, Class classTarget){
        collisions.clear();
        world.addCollisionObject(object);
        world.contactTest(object, new ContactResultCallback() {
            @Override
            public float addSingleResult(btManifoldPoint cp, btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                         btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {

                Vector3 position = vectorPool.obtain();
                cp.getPositionWorldOnA(position);


                if(classTarget != null && (colObj0Wrap.getCollisionObject().equals(classTarget) || colObj1Wrap.getCollisionObject().equals(classTarget)))
                    return 1f;

                if (colObj0Wrap.getCollisionObject().equals(object))
                    collisions.put(colObj1Wrap.getCollisionObject(), position);
                else
                    collisions.put(colObj0Wrap.getCollisionObject(), position);

                return 0;
            }
        });

        return collisions;
    }

    public HashMap<btCollisionObject, Vector3> consumeCollision(btCollisionObject object){
        collisions.clear();
        world.addCollisionObject(object);
        world.contactTest(object, new ContactResultCallback() {
            @Override
            public float addSingleResult(btManifoldPoint cp, btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                         btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {

                Vector3 position = vectorPool.obtain();
                cp.getPositionWorldOnA(position);

                if(colObj0Wrap.getCollisionObject() instanceof PlayerBody || colObj1Wrap.getCollisionObject() instanceof PlayerBody)
                    return 1f;

                if (colObj0Wrap.getCollisionObject().equals(object))
                    collisions.put(colObj1Wrap.getCollisionObject(), position);
                else
                    collisions.put(colObj0Wrap.getCollisionObject(), position);

                return 0;
            }
        });

        return collisions;
    }

    public void addCollisionObject(ModelInstance collisionObject) {
        Array<Node> filteredNodes = new Array<>();
        for (Node node : collisionObject.nodes) {
            if (node.id == null ||
                !node.id.toLowerCase().contains("nocallback") &&
                !node.id.toLowerCase().contains("clouds") &&
                !node.id.toLowerCase().contains("door") &&
                !node.id.toLowerCase().contains("trigger")) {
                filteredNodes.add(node);
            }
        }

        btCollisionShape shape = Bullet.obtainStaticNodeShape(filteredNodes);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0.0F, null, shape);

        MeshBody body = new MeshBody(info);
        body.setRestitution(0.1f);
        body.setFriction(0.6f);
        body.setContactCallbackFlag(GROUND_FLAG);
        body.setContactCallbackFilter(0);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        body.setCustomDebugColor(new Vector3(1f,0f,0f));

        world.addCollisionObject(body, GROUND_FLAG, ALL_FLAG);

        filteredNodes.clear();
    }

    public void addOccluderObject(Node node) {

        BoundingBox boundingBox = node.calculateBoundingBox(new BoundingBox());
        btBoxShape shape = new btBoxShape(boundingBox.getDimensions(vectorPool.obtain()).scl(0.5f));
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0.0F, null, shape);

        OcclusionBody body = new OcclusionBody(info);
        body.setCollisionFlags(body.getCollisionFlags() | CF_OCCLUDER_OBJECT | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        body.translate(node.translation);
        body.setCustomDebugColor(new Vector3(0.2f, 1f, 0.2f));
        body.box = boundingBox;
        world.addCollisionObject(body);

        info.dispose();
    }

    public void addWaterMesh(Node node){
        btCollisionShape collisionShape = Bullet.obtainStaticNodeShape(node, true);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0.0F, null, collisionShape);

        WaterBody waterBody = new WaterBody(info);
        waterBody.setCollisionFlags(waterBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        waterBody.setCustomDebugColor(vectorPool.obtain().set(0.1f, 0.5f, 0.9f));
        world.addCollisionObject(waterBody);

        info.dispose();
    }

    public void addPlayerBody(ModelInstance motion) {
        BoundingBox boundingBox = new BoundingBox();
        motion.calculateBoundingBox(boundingBox);
        Vector3 dimensions = vectorPool.obtain();
        boundingBox.getDimensions(dimensions).scl(0.6f);

        btCapsuleShape shape = new btCapsuleShape(dimensions.len() * 0.3f, dimensions.y);
        float mass = 60.0F;

        playerMotion = new PlayerMotion(motion.transform);

        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, playerMotion, shape);
        playerBody = new PlayerBody(info);
        playerBody.setFriction(0f);
        playerBody.setCcdMotionThreshold(0.1f);
        playerBody.setCcdSweptSphereRadius(0.3f);
        configurePlayerBody(playerBody);
        playerBody.setCustomDebugColor(vectorPool.obtain().set(1f, 1f, 0f));
        playerMotion.setPlayerBody(playerBody);
        world.addRigidBody(playerBody);

        createSensorBody();
        info.dispose();
    }

    public NPCBody addBotBody(ModelInstance bot){
        BoundingBox boundingBox = new BoundingBox();
        bot.calculateBoundingBox(boundingBox);
        Vector3 dimensions = vectorPool.obtain();
        boundingBox.getDimensions(dimensions).scl(0.6f);
        btCapsuleShape shape = new btCapsuleShape(dimensions.len() * 0.3f, dimensions.y);
        float mass = 60.0F;

        NPCMotion motion = new NPCMotion(bot.transform);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, motion, shape);
        NPCBody botBody = new NPCBody(info);
        botBody.setFriction(0.1f);
        configurePlayerBody(botBody);
        botBody.setContactCallbackFlag(GROUND_FLAG);
        world.addRigidBody(botBody, GROUND_FLAG, ALL_FLAG);
        info.dispose();

        return botBody;
    }

    private void configurePlayerBody(btRigidBody body) {
        body.setCcdMotionThreshold(1.0E-7F);
        body.setCcdSweptSphereRadius(0.2F);
        body.setAngularFactor(0.0F);
        body.setDamping(0.7F, 0F);
        body.setActivationState(Collision.DISABLE_DEACTIVATION);
        body.setSleepingThresholds(0.01F, 0.01F);
        body.setContactCallbackFlag(10);
        body.setContactCallbackFilter(11);
        body.setRestitution(0);
    }

    public void addRigidBody(btRigidBody body){
        world.addRigidBody(body, OBJECT_FLAG, GROUND_FLAG);
    }

    public btRigidBody uploadSensorObject(BoundingBox boundingBox, Vector3 position, String name){

        btBoxShape boxShape = new btBoxShape(new Vector3(boundingBox.getWidth() * 1.5f, boundingBox.getHeight(), boundingBox.getDepth() * 1.5f));

        // Создаем информацию для построения тела
        btRigidBody.btRigidBodyConstructionInfo bodyInfo = new btRigidBody.btRigidBodyConstructionInfo(
            0f, // Масса (0 для статического объекта)
            null, // Состояние движения (null для статического объекта)
            boxShape // Форма
        );

        // Создаем тело
        EntityBody body = new EntityBody(bodyInfo);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE); // Устанавливаем флаги столкновений
        body.setCustomDebugColor(new Vector3(0.2f, 1f, 0.9f));
        body.translate(position);
        body.userData = name;
        bodyInfo.dispose();

        // Добавляем тело в мир
        world.addCollisionObject(body, GROUND_FLAG, ALL_FLAG);

        return body;
    }

    public btRigidBody uploadEntityBody(BoundingBox boundingBox, Vector3 position){

        btConvexHullShape hullShape = new btConvexHullShape();

        Vector3 start = vectorPool.obtain();
        start.set(boundingBox.getCenter(vectorPool.obtain()));
        start.y = position.y - boundingBox.getHeight();

        Vector3 end = vectorPool.obtain();
        end.set(boundingBox.getCenter(vectorPool.obtain()));
        end.y = boundingBox.max.y;

        // Добавляем две точки в Convex Hull
        hullShape.addPoint(start);
        hullShape.addPoint(end);

        // Создаем информацию для построения тела
        btRigidBody.btRigidBodyConstructionInfo bodyInfo = new btRigidBody.btRigidBodyConstructionInfo(
            0f, // Масса (0 для статического объекта)
            null, // Состояние движения (null для статического объекта)
            hullShape // Форма
        );

        // Создаем тело
        btRigidBody body = new btRigidBody(bodyInfo);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK); // Устанавливаем флаги столкновений
        body.setCustomDebugColor(new Vector3(0.9f, 0.1f, 0.9f));
        bodyInfo.dispose();

        // Добавляем тело в мир
        world.addCollisionObject(body, GROUND_FLAG, ALL_FLAG);

        return body;
    }

    public TriggerBody addTriggerObject(Node node) {
        // Временное удаление детей

        Array<Node> children = new Array<>();
        System.out.println(node.id + " " + node.getChildCount());
        int size = node.getChildCount();
        for(int i = 0; i < size; i++){
            Node child = node.getChild(0);
            children.add(child);
            node.removeChild(child);
        }

        // Создание коллизионной формы
        btCollisionShape shape = Bullet.obtainStaticNodeShape(node, false);

        // Восстановление детей
        for(Node child : children) {
            node.addChild(child);
        }

        btRigidBody.btRigidBodyConstructionInfo shapeInfo =
            new btRigidBody.btRigidBodyConstructionInfo(0f, null, shape);

        TriggerBody triggerBody = new TriggerBody(shapeInfo);
        triggerBody.setCollisionFlags(triggerBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        triggerBody.setCustomDebugColor(vectorPool.obtain().set(0.1f, 0.5f, 0.1f));
        triggerBody.setWorldTransform(node.globalTransform);
        shapeInfo.dispose();

        world.addCollisionObject(triggerBody);

        return triggerBody;
    }

    public void addNocallbackObject(Node node){
        btCollisionShape shape = Bullet.obtainStaticNodeShape(node, true);
        btRigidBody.btRigidBodyConstructionInfo shapeInfo = new btRigidBody.btRigidBodyConstructionInfo(0f, null, shape);

        NocallbackBody btRigidBody = new NocallbackBody(shapeInfo);
        world.addCollisionObject(btRigidBody);
        btRigidBody.userData = node.parts.get(0).meshPart.id;
        shapeInfo.dispose();
    }

    public btRigidBody createBoxBody(BoundingBox boundingBox){

        btBoxShape box = new btBoxShape(vectorPool.obtain().set(0.5f, 0.5f, 0.5f));
        btRigidBody.btRigidBodyConstructionInfo sensorInfo = new btRigidBody.btRigidBodyConstructionInfo(0f, null, box);

        btRigidBody body = new btRigidBody(sensorInfo);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        body.setAngularFactor(0.0F);
        body.setSpinningFriction(1.5f);
        body.setCcdMotionThreshold(1.0E-7F);
        body.setSleepingThresholds(0.01F, 0.01F);
        body.setCcdSweptSphereRadius(0.2F);
        body.setContactCallbackFlag(OBJECT_FLAG);
        body.setContactCallbackFilter(GROUND_FLAG);

        sensorInfo.dispose();

        return body;
    }

    private void createSensorBody() {
        btCapsuleShape sensorShape = new btCapsuleShape(PlayerConfiguration.PLAYER_WEIGHT.getValue() * 0.7f, 1f);
        btRigidBody.btRigidBodyConstructionInfo sensorInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, sensorShape);


        sensorPlayer = new btRigidBody(sensorInfo);
        sensorPlayer.setCollisionFlags(sensorPlayer.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        sensorPlayer.setContactCallbackFlag(OBJECT_FLAG);
        sensorPlayer.setContactCallbackFilter(GROUND_FLAG);
        sensorPlayer.setCustomDebugColor(vectorPool.obtain().set(0.1f, 0.5f, 0.1f));

        world.addCollisionObject(sensorPlayer, OBJECT_FLAG, GROUND_FLAG);
        sensorInfo.dispose();

        playerMotion.setSensorBody(sensorPlayer);
    }

    private void calculateRotationBody(Vector3 direction) {
        float angle = (float) Math.atan2(direction.z, direction.x);
        Matrix4 transform = playerBody.getWorldTransform();
        transform.rotate(Vector3.Y, -previousAngle);
        transform.rotate(Vector3.Y, angle);
        previousAngle = angle;
    }

    public boolean isRaycastedCustomCallbackRay(Vector3 from, Vector3 to, ClosestRayResultCallback callback){
        callback.setCollisionObject(null);
        callback.setClosestHitFraction(1f);
        callback.setRayToWorld(from);
        callback.setRayFromWorld(to);
        world.rayTest(from, to, callback);

        return callback.hasHit();
    }

    public btRigidBody getPlayerBody(){
        return playerBody;
    }

    public Pool<Vector3> getVectorPool() {
        return vectorPool;
    }

    public void updateCondition(Camera camera) {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), FIXED_TIMESTEP);
        float speed = delta * PlayerConfiguration.MAX_SPEED.getValue();
        float boost = PlayerConfiguration.BOOST.getValue();
        float xSpeed, zSpeed;

        Vector3 direction = vectorPool.obtain();
        Vector3 upVector = vectorPool.obtain();

        direction.set(camera.direction);
        direction.y = 0;
        calculateRotationBody(direction);
        upVector.set(camera.up);

        playerBody.activate();

        if (direction.y == 1) {
            xSpeed = upVector.x / (Math.abs(upVector.x) + Math.abs(upVector.z)) * (-boost);
            zSpeed = upVector.z / (Math.abs(upVector.x) + Math.abs(upVector.z)) * (-boost);
        } else if (direction.y == -1) {
            xSpeed = upVector.x / (Math.abs(upVector.x) + Math.abs(upVector.z)) * boost;
            zSpeed = upVector.z / (Math.abs(upVector.x) + Math.abs(upVector.z)) * boost;
        } else {
            xSpeed = direction.x / (Math.abs(direction.x) + Math.abs(direction.z)) * boost;
            zSpeed = direction.z / (Math.abs(direction.x) + Math.abs(direction.z)) * boost;
        }

        Vector3 speedVector = vectorPool.obtain();
        speedVector.set(xSpeed, 0, zSpeed);

        playerMotion.updateMovement(speedVector, speed, groundContactCount, delta);
    }

    public PlayerMotion getPlayerMotion() {
        return playerMotion;
    }

    public Vector3 getLinearVelocity() {
        return playerBody.getLinearVelocity();
    }

    public btDynamicsWorld getPhysicWorld() {
        return world;
    }

    public Enemy consumeRaycast(float scale, int spread, Camera camera, TextureRegion textureRegion, String effect){

        int randX = spread != 0 ?  ThreadLocalRandom.current().nextInt(-spread, spread) : 0;
        int randY = spread != 0 ?  ThreadLocalRandom.current().nextInt(-spread, spread) : 0;

        int xSpread = Gdx.graphics.getWidth() / 2 + randX;
        int ySpread = Gdx.graphics.getHeight() / 2 + randY;

        Ray raycast = camera.getPickRay(xSpread, ySpread);
        Vector3 rayTo = raycast.direction.cpy().scl(scale).add(raycast.origin);
        ClosestRayResultCallback callback = raycastCallbackPool.obtain();


        callback.setRayToWorld(rayTo);
        callback.setRayFromWorld(raycast.origin);
        world.rayTest(raycast.origin, rayTo, callback);

        if(callback.hasHit() && callback.getCollisionObject() != playerBody) {
            btCollisionObject hitObject = callback.getCollisionObject();
            Vector3 position = vectorPool.obtain();
            callback.getHitPointWorld(position);

            if(hitObject instanceof Enemy){
                Enemy enemy = (Enemy) hitObject;
                enemy.setPositionHit(position);
                return enemy;
            }

            if(hitObject instanceof TriggerBody){
                TriggerBody triggerBody = (TriggerBody) hitObject;
                triggerBody.activateTrigger();
                triggerBody.disposeTrigger();
            }

            Vector3 normal = vectorPool.obtain();
            callback.getHitNormalWorld(normal);
            normal.nor();
            position.add(normal.x * 0.05f, normal.y * 0.05f, normal.z * 0.05f);

            if(textureRegion != null && callback.getCollisionObject() instanceof MeshBody) {

                float pitch = (float) Math.asin(-normal.y); //вычисялем угол по оси X
                float yaw = (float) Math.atan2(normal.x, normal.z); //вычисляем угол по оси Y;

                pitch *= MathUtils.radiansToDegrees;
                yaw *= MathUtils.radiansToDegrees;

                Quaternion quaternion = new Quaternion();
                quaternion.setEulerAngles(yaw, pitch, 0);
                DecalComponent.getInstance(null).putDecal(quaternion, position, textureRegion);
            }

            ParticleEffect shoot = ParticlesComponent.getInstance(null).findEffect(effect);
            shoot.translate(position);
            shoot.init();
            shoot.start();
            ParticlesComponent.getInstance(null).addDynamicEffect(shoot);
        }
        return null;
    }

    @Override
    public void dispose() {
        // Dispose all bullet physics objects
        if (world != null) {
            // Remove and dispose all collision objects
            btCollisionObjectArray collisionObjects = world.getCollisionObjectArray();
            for (int i = collisionObjects.size() - 1; i >= 0; i--) {
                btCollisionObject obj = collisionObjects.atConst(i);
                world.removeCollisionObject(obj);
                if (obj instanceof btRigidBody) {
                    ((btRigidBody)obj).dispose();
                }
                obj.dispose();
            }

            // Dispose world components
            world.dispose();
            world = null;
        }

        if (dispatcher != null) {
            dispatcher.dispose();
            dispatcher = null;
        }

        if (broadphase != null) {
            broadphase.dispose();
            broadphase = null;
        }

        if (solver != null) {
            solver.dispose();
            solver = null;
        }

        if (config != null) {
            config.dispose();
            config = null;
        }

        if (debugDrawer != null) {
            debugDrawer.dispose();
            debugDrawer = null;
        }

        // Dispose pools
        if (bodiesPool != null) {
            Pools.free(bodiesPool);
            bodiesPool = null;
        }

        if (raycastCallbackPool != null) {
            raycastCallbackPool.clear();
            Pools.free(raycastCallbackPool);
            raycastCallbackPool = null;
        }

        if (npcCallBackPool != null) {
            npcCallBackPool.clear();
            Pools.free(npcCallBackPool);
            npcCallBackPool = null;
        }

        if (vectorPool != null) {
            vectorPool.clear();
            Pools.free(vectorPool);
            vectorPool = null;
        }

        if (npcPartBodyPool != null) {
            npcPartBodyPool.clear();
            Pools.free(npcPartBodyPool);
            npcPartBodyPool = null;
        }

        // Clear collections
        if (npcPartBodies != null) {
            npcPartBodies.clear();
            npcPartBodies = null;
        }

        if (collisions != null) {
            collisions.clear();
            collisions = null;
        }

        // Dispose sprite batch
        if (spriteBatch != null) {
            spriteBatch.dispose();
            spriteBatch = null;
        }

        // Dispose player-related objects
        if (playerBody != null) {
            playerBody.dispose();
            playerBody = null;
        }

        if (sensorPlayer != null) {
            sensorPlayer.dispose();
            sensorPlayer = null;
        }

        if (playerMotion != null) {
            playerMotion.dispose();
            playerMotion = null;
        }

        if (contactListener != null) {
            contactListener.dispose();
            contactListener = null;
        }

        if (callback != null) {
            callback = null;
        }

        // Reset singleton instance
        instance = null;
    }

    public class SensorListener extends ContactListener {

        @Override
        public void onContactStarted(btCollisionObject colObj0, btCollisionObject colObj1) {
            if (isContactWithMap(colObj0, colObj1)) {
                groundContactCount++;
                isOnGround = true;
            }
        }

        @Override
        public void onContactEnded(btCollisionObject colObj0, btCollisionObject colObj1) {
            if (isContactWithMap(colObj0, colObj1)) {
                groundContactCount--;
                if (groundContactCount <= 0) {
                    isOnGround = false;
                    groundContactCount = 0;
                }
            }
        }

        private boolean isContactWithMap(btCollisionObject colObj0, btCollisionObject colObj1) {
            return (colObj0 == sensorPlayer && (colObj1.getContactCallbackFlag() & GROUND_FLAG) != 0) || (colObj1 == sensorPlayer && (colObj0.getContactCallbackFlag() & GROUND_FLAG) != 0);
        }
    }


    public class CustomCallback extends ContactResultCallback{
        private boolean collisionDetected;
        private btCollisionObject first;
        private btCollisionObject second;

        @Override
        public float addSingleResult(btManifoldPoint cp, btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                     btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {

            first = colObj1Wrap.getCollisionObject();
            second = colObj1Wrap.getCollisionObject();
            collisionDetected = true;

            return 0;
        }

        public btCollisionObject getFirst() {
            return first;
        }

        public btCollisionObject getSecond() {
            return second;
        }

        public boolean isCollisionDetected(){
            return collisionDetected;
        }

        public void Clear(){
            collisionDetected = false;
            first = null;
            second = null;
        }
    }

    public boolean checkOnGround() {
        return isOnGround;
    }
}
