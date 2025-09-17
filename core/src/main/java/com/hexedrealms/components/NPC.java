package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.BlendedSteering;
import com.badlogic.gdx.ai.steer.behaviors.Seek;
import com.badlogic.gdx.ai.steer.behaviors.Separation;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Queue;
import com.hexedrealms.components.bulletbodies.MeshBody;
import com.hexedrealms.components.bulletbodies.NPCBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.*;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.NPCPathfinder.Node;
import com.hexedrealms.utils.NPCPathfinder.NodeGraph;
import com.hexedrealms.utils.NPCSteering.RadiusProximity;
import com.hexedrealms.utils.NPCSteering.WalkerState;
import com.hexedrealms.utils.damage.DamageType;
import com.hexedrealms.weapons.BalisticWeapon;
import com.hexedrealms.weapons.ShootWeapon;
import com.hexedrealms.weapons.Weapon;
import de.pottgames.tuningfork.SoundBuffer;
import de.pottgames.tuningfork.SoundLoader;
import de.pottgames.tuningfork.StreamedSoundSource;
import org.checkerframework.checker.units.qual.A;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class NPC implements Steerable<Vector3>, Pool.Poolable, Disposable {
    // Статические константы
    private float MAX_LINEAR_SPEED;
    private static final float MAX_LINEAR_ACCELERATION = 50f;
    private static final float MAX_ANGULAR_SPEED = 0f;
    private static final float MAX_ANGULAR_ACCELERATION = 0f;
    private static final float SEPARATION_RADIUS = 20f;
    private static final float SEPARATION_DECAY_COEFFICIENT = 50f;
    private static final float MAX_STUCK_TIME = 5f;

    private final Vector3 tempVector = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 velocityVector = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 rayFrom = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 rayTo = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 hit = PhysicComponent.getInstance().getVectorPool().obtain();
    private final SteeringAcceleration<Vector3> steeringOutput = new SteeringAcceleration<>(PhysicComponent.getInstance().getVectorPool().obtain());
    private final Queue<Node> pathQueue = new Queue<>();

    private StateMachine<NPC, WalkerState> stateMachine;
    private NodeGraph nodeGraph;
    private Vector3 position;
    private Node previousNode, endNode, lastNode, start;
    private ModelInstance instance;
    private NPCBody body;
    private Seek<Vector3> seekBehavior;
    private Separation<Vector3> separationBehavior;
    private RadiusProximity proximity;
    private BlendedSteering<Vector3> blendedSteering;
    private ClosestRayResultCallback rayCallback;
    private SoundBuffer portal;
    private Entity entity;
    private Entity teleport;
    private float voiceTimer, voicePred;
    private DamageType damageType;
    private boolean punch;

    private float previousHealth;
    private float livetimer;

    private float stuckTimer = 0f;
    private boolean tagged, reachedTarget;
    private float boundingRadius;
    private float orientation;
    private float delta;
    public float borderPauseTimer;
    public float borderPauseDuration;
    private float height, width;
    private boolean comeback;

    private float baseDamage;
    private float baseArmor;
    private float baseHealth;
    private float agility;
    private float strength;
    private float intelligence;
    private float vitality;

    private Array<TextureAtlas.AtlasRegion>[] stays;
    private Array<TextureAtlas.AtlasRegion>[] atacks;
    private Array<TextureAtlas.AtlasRegion>[] deathes;
    private Array<TextureAtlas.AtlasRegion>[] punches;
    private Array<TextureAtlas.AtlasRegion> parts;

    public SoundBuffer look;
    public SoundBuffer pain;
    public SoundBuffer atack;
    public SoundBuffer death;

    public State<NPC> aiType;

    private boolean isDied, isDamaged;

    private String name, atlasname;

    public NPC(String name, String atlasname, float maxSpeed, DamageType damageType, State<NPC> aiType,
               int baseDamage, float baseArmor, float baseHealth, float agility, float strength, float intelligence, float vitality) {

        this.MAX_LINEAR_SPEED = maxSpeed;
        this.atlasname = atlasname;
        this.baseDamage = baseDamage;
        this.baseArmor = baseArmor;
        this.baseHealth = baseHealth;
        this.agility = agility;
        this.strength = strength;
        this.intelligence = intelligence;
        this.vitality = vitality;
        this.damageType = damageType;
        this.aiType = aiType;
        this.name = name;
        this.orientation = 0;
        this.rayCallback = PhysicComponent.getInstance().getRaycastNpcPool().obtain();
        this.damageType = damageType;
        stateMachine = new DefaultStateMachine<>(this, WalkerState.SPAWN);
        stays = new Array[8];
        atacks = new Array[8];
        deathes = new Array[8];
        punches = new Array[8];

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(atlasname));
        for (int i = 0; i < 8; i++) {
            stays[i] = atlas.findRegions("stay_"+i);
            atacks[i] = atlas.findRegions("atack_"+i);
            deathes[i] = atlas.findRegions("death_"+i);
            punches[i] = atlas.findRegions("smash_"+i);
        }
        parts = atlas.findRegions("part");
        portal = SoundLoader.load(Gdx.files.internal("audio/sfx/teleport_splash.wav"));
    }

    public boolean isDamaged(){
        return isDamaged;
    }

    public State<NPC> getAiType() {
        return aiType;
    }

    public float getCurrentHealth(){
        return body.getCurrentHealth();
    }

    private void initializeModel() {
        height = stays[0].get(0).getRegionHeight() * 0.07f;
        width = stays[0].get(0).getRegionWidth() * 0.01f;
        ModelBuilder builder = new ModelBuilder();
        Material mat = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        Model model = builder.createCapsule(width, height, 30, GL30.GL_TRIANGLES, mat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        instance = new ModelInstance(model);
    }

    public void init() {
        voicePred = ThreadLocalRandom.current().nextFloat(1 , 3);
        this.position = PhysicComponent.getInstance().getVectorPool().obtain().set(start.position);
        this.previousNode = start;
        initializeModel();

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures/atlases/teleport/teleport.atlas"));
        teleport = new Entity();
        teleport.init(atlas.getRegions(), PhysicComponent.getInstance().getVectorPool().obtain().set(start.position).add(0f, 2f, 0f), this.instance.transform.getRotation(new Quaternion()), false);
        teleport.calcScale(0.15f);
        teleport.setSpeed(0.7f);

        EntityComponent entityComponent = EntityComponent.getInstance(null);
        entity = entityComponent.getPool().obtain();
        entity.init(stays[0], PhysicComponent.getInstance().getVectorPool().obtain().set(start.position), this.instance.transform.getRotation(new Quaternion()), false);
        entity.calcScale(0.07f);
        entityComponent.addEntity(teleport);

        setupSteering();
    }

    public void playPortal(){
        portal.play3D(AudioConfiguration.SOUND.getValue(), position);
    }

    private void setupSteering() {
        Array<Steerable<Vector3>> allBots = NPCComponent.getInstance(null, null).getObstacles();
        allBots.add(this);
        proximity = new RadiusProximity(this, allBots, SEPARATION_RADIUS);
        separationBehavior = new Separation<>(this, proximity);
        separationBehavior.setDecayCoefficient(SEPARATION_DECAY_COEFFICIENT);

        seekBehavior = new Seek<>(this);
        blendedSteering = new BlendedSteering<>(this);
        blendedSteering.add(seekBehavior, 0.5f);
        blendedSteering.add(separationBehavior, 1f);
    }

    public void removeTeleport() {
        EntityComponent.getInstance(null).removeEntity(teleport);
        teleport = null;
    }

    public boolean isSpawned() {
        return teleport != null && teleport.getAnimation().isFinalled();
    }

    public ModelInstance getInstance() {
        return instance;
    }

    public void setBody(NPCBody body) {
        this.body = body;
        body.setFriction(1f);
        body.setHeight(height * 0.5f);
        body.setVitality(vitality);
        body.setStrength(strength);
        body.setArmor(baseArmor);
        body.setHealth((int) baseHealth);
        body.setIntelligence(intelligence);
        body.setBasicDamage(baseDamage);
        body.uploadParts(parts);
        previousHealth = baseHealth;
    }

    public NPC copy() {
        // Создаем нового NPC с теми же базовыми параметрами
        NPC copy = new NPC(
            this.name,
            atlasname, // Путь к атласу
            MAX_LINEAR_SPEED,
            this.damageType,
            this.aiType,
            (int)this.baseDamage,
            this.baseArmor,
            this.baseHealth,
            this.agility,
            this.strength,
            this.intelligence,
            this.vitality
        );

        // Копируем состояние
        if (this.position != null) {
            copy.position = new Vector3(this.position);
        }
        copy.orientation = this.orientation;
        copy.boundingRadius = this.boundingRadius;
        copy.borderPauseTimer = this.borderPauseTimer;
        copy.borderPauseDuration = this.borderPauseDuration;
        copy.comeback = this.comeback;
        copy.reachedTarget = this.reachedTarget;
        copy.stuckTimer = this.stuckTimer;
        copy.look = this.look;
        copy.pain = this.pain;
        copy.atack = this.atack;
        copy.death = this.death;

        // Копируем текстуры (они не изменяются, поэтому можно использовать те же ссылки)
        for (int i = 0; i < 8; i++) {
            if (this.stays[i] != null) {
                copy.stays[i] = new Array<>(this.stays[i]);
            }
            if (this.atacks[i] != null) {
                copy.atacks[i] = new Array<>(this.atacks[i]);
            }
        }
        if (this.parts != null) {
            copy.parts = new Array<>(this.parts);
        }

        // Копируем звуки (если они есть)
        if (this.portal != null) {
            copy.portal = this.portal;
        }

        // Инициализируем FSM с тем же состоянием
        copy.stateMachine = new DefaultStateMachine<>(copy,
            this.stateMachine.getCurrentState());

        // Копируем граф (ссылка, так как граф общий для всех NPC)
        copy.nodeGraph = this.nodeGraph;

        return copy;
    }

    private void updateVoiceTimer(float delta){

        if(voiceTimer > voicePred) {
            int rand = ThreadLocalRandom.current().nextInt(0, 2);
            if (rand == 0) {
                look.play3D(AudioConfiguration.SOUND.getValue(), position);
            } else {
                atack.play3D(AudioConfiguration.SOUND.getValue(), position);
            }

            voicePred = ThreadLocalRandom.current().nextFloat(1 , 3);
            voiceTimer = 0f;
        }
        voiceTimer += delta;
    }

    private void updateLiveTimer(float delta){

        if(livetimer > 2f) {
            ResourcesLoader.freeBot(this);
            NPCComponent.getInstance(null, null).removeNPC(this);
            EntityComponent.getInstance(null).removeEntity(entity);

            Array<Weapon> weapons = Level.getInstance().getPlayer().getmWeaponContainer().getWeapons();
            String weaponName = null;
            float minBullets = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                if(weapon instanceof ShootWeapon){
                    ShootWeapon shootWeapon = (ShootWeapon) weapon;
                    float percent = (float) shootWeapon.getTotalBullets() / shootWeapon.getMaxBullets();
                    if(minBullets > percent) {
                        minBullets = percent;
                        weaponName = shootWeapon.getNameObject();
                    }
                }
            }

            if(weaponName != null) {
                ItemEntity itemEntity = ResourcesLoader.findItem(weaponName);
                itemEntity.init(new Vector3(position).add(ThreadLocalRandom.current().nextFloat(-1, 1f), 0.7f, ThreadLocalRandom.current().nextFloat(-1, 1f)), Level.getInstance().getManager().environment);
                ItemsComponent.getInstance().putItem(itemEntity);
            }

            ItemEntity itemHealth = ResourcesLoader.getItem("health");
            itemHealth.init(new Vector3(position).add(ThreadLocalRandom.current().nextFloat(-1, 1f),0.7f,ThreadLocalRandom.current().nextFloat(-1, 1f)), Level.getInstance().getManager().environment);
            ItemsComponent.getInstance().putItem(itemHealth);

            return;
        }
        livetimer += delta;
    }

    public void update(float delta) {
        this.delta = delta;



        if(body == null && previousHealth <= 0 && entity.getAnimation().isFinalled() ){
            updateLiveTimer(delta);
            return;
        }

        if(previousHealth <= 0 && entity.getAnimation().isFinalled()){
            PhysicComponent.getInstance().removeRigidBody(body);
            body = null;
            return;
        }

        if(punch && entity.getAnimation().isFinalled()){
            entity.getAnimation().setFinalled(false);
            punch = false;
        }

        if(body != null && previousHealth != body.getCurrentHealth()){
            pain.play3D(AudioConfiguration.SOUND.getValue(), position, 3f,3f);
            previousHealth = body.getCurrentHealth();
            isDamaged = true;
        }

        if(previousHealth > 0){
            updateVoiceTimer(delta);
        }

        stateMachine.update();
        if (teleport == null) {
            calcRotation(delta);

            Vector3 targetPosition = body.getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain());
            if(isDied) {
                targetPosition.y -= height * 0.4f;
                position = targetPosition;
            }

            entity.setPosition(targetPosition, width);
        }

    }

    public String getName(){
        return name;
    }

    public void attemptAutoClimb(Vector3 directionMove) {
        rayCallback.setCollisionObject(null);
        rayCallback.setClosestHitFraction(1f);

        rayFrom.set(position).add(0, height * 0.5f, 0);
        rayTo.set(rayFrom).add(directionMove.cpy().nor().scl(width * 2f));
        rayCallback.setRayFromWorld(rayFrom);
        rayCallback.setRayToWorld(rayTo);

        PhysicComponent.getInstance().getPhysicWorld().rayTest(rayFrom, rayTo, rayCallback);

        if (!rayCallback.hasHit()) {


            rayCallback.setCollisionObject(null);
            rayCallback.setClosestHitFraction(1f);

            rayFrom.set(position).add(directionMove.cpy().nor().scl(width * 2f)).add(0, height * 0.5f, 0);
            rayTo.set(rayFrom).sub(0, height, 0);
            rayCallback.setRayFromWorld(rayFrom);
            rayCallback.setRayToWorld(rayTo);

            PhysicComponent.getInstance().getPhysicWorld().rayTest(rayFrom, rayTo, rayCallback);

            if (rayCallback.hasHit() && rayCallback.getCollisionObject() instanceof MeshBody) {
                rayCallback.getHitPointWorld(hit);

                float dst = Math.abs(position.y - hit.y);

                if (dst < height * 0.45f) {
                    Vector3 cachedVelocity = body.getLinearVelocity();
                    cachedVelocity.y = 12 + (dst * (height * 0.5f));
                    body.setLinearVelocity(cachedVelocity);

                }
            }
        }
    }

    private void calcRotation(float delta) {
        if(isDied) return;
        // Получаем позицию камеры и НПС
        Vector3 playerPosition = Level.getInstance().getPlayer().getCamera().position;
        Vector3 npcPosition = this.position;

        // Рассчитываем направление вектора от НПС к игроку
        Vector3 toPlayerVector = PhysicComponent.getInstance().getVectorPool().obtain().set(
            playerPosition.x - npcPosition.x,
            0,
            playerPosition.z - npcPosition.z
        );

        // Рассчитываем углы относительно направления к камере и направления движения
        float angleToPlayer = (float) Math.toDegrees(Math.atan2(toPlayerVector.z, toPlayerVector.x));
        angleToPlayer = (angleToPlayer + 360) % 360;

        float movementAngle = (float) Math.toDegrees(Math.atan2(-tempVector.z, -tempVector.x));
        movementAngle = (movementAngle + 180) % 360;

        // Находим итоговый угол
        float targetAngle = movementAngle - angleToPlayer;
        orientation = MathUtils.lerpAngleDeg(orientation, targetAngle, delta * 10f);

        // Рассчитываем индекс кадра для отображения в зависимости от итогового угла
        int directionIndex = (int) ((orientation + 22.5f) / 45) % 8;
        directionIndex = (directionIndex + 8) % 8;

        // Отображаем анимацию из необходимого набора
        if(body.getCurrentHealth() > 0) {
            if (body.getLinearVelocity().len() > 10) {
                entity.setRegions(atacks[directionIndex]);
            } else if (punch) {
                entity.setRegions(punches[directionIndex]);
            } else {
                entity.setRegions(stays[directionIndex]);
            }
        }else{

            isDied = true;
            entity.animation.setSingleUse(true);
            entity.setRegions(deathes[directionIndex]);
            position.y -= 0.5f;
            death.play3D(AudioConfiguration.SOUND.getValue(), position);
        }
    }

    public DamageType getDamageType(){
        return damageType;
    }

    public void setPunch(){
        if(!punch) {
            entity.setFinalled(false);
            punch = true;
        }
    }

    public Seek<Vector3> getSeekBehavior() {
        return seekBehavior;
    }

    public void setCurrentNode(Node node){
        previousNode = node;
    }

    public void reachWalk() {
        Node reachedNode = previousNode;
        Array<Connection<Node>> connections = NPCComponent.getInstance(null, null).getGraph().getConnections(reachedNode);

        int count = ThreadLocalRandom.current().nextInt(1, 5);
        for (int i = 0; i < count; i++) {
            Connection<Node> connection = connections.random();
            if(connection == null) return;

            reachedNode = connections.random().getToNode();

            connections = NPCComponent.getInstance(null, null).getGraph().getConnections(reachedNode);
        }
        reachDestination(reachedNode);
    }

    public boolean isComeback() {
        return comeback;
    }

    public void clearTimer() {
        borderPauseTimer = 0;
        borderPauseDuration = 0;
    }

    public void updateTimer() {
        if (borderPauseDuration == 0f)
            borderPauseDuration = ThreadLocalRandom.current().nextFloat(1f, 1.5f);

        borderPauseTimer += delta;
    }

    public boolean getAnimation(){
        return entity.getAnimation().isFinalled();
    }

    public Vector3 checkPointHit(){
        Vector3 playerPosition = Level.getInstance().getPlayer().getCamera().position;
        PhysicComponent.getInstance().isRaycastedCustomCallbackRay(position, playerPosition, rayCallback);

        Vector3 pointHit = PhysicComponent.getInstance().getVectorPool().obtain();
        rayCallback.getHitPointWorld(pointHit);
        return pointHit;
    }

    public boolean lookAtPlayer() {
        Vector3 playerPosition = Level.getInstance().getPlayer().getCamera().position;

        boolean isViewed = PhysicComponent.getInstance().isRaycastedCustomCallbackRay(position, playerPosition, rayCallback);
        return isViewed && rayCallback.getCollisionObject().equals(PhysicComponent.getInstance().getPlayerBody());
    }

    public Node getEndNode() {
        return endNode;
    }

    public Node getPreviousNode() {
        return previousNode;
    }

    public Queue<Node> getPathQueue() {
        return pathQueue;
    }

    public StateMachine<NPC, WalkerState> getStateMachine() {
        return stateMachine;
    }

    public btRigidBody getBody() {
        return body;
    }

    private void handleIdle() {
        if (body.getLinearVelocity().len() < 15f) {
            stuckTimer += delta;

            if (stuckTimer > MAX_STUCK_TIME) {
                if (lastNode != null) {
                    pathQueue.clear();
                    reachDestination(lastNode);
                    comeback = true;
                }
                stuckTimer = 0f;
            }
        } else {
            stuckTimer = 0f;
        }
    }

    public void updateMovement() {
        if(previousHealth > 0) {
            if (seekBehavior.getTarget() != null && !punch) {
                blendedSteering.calculateSteering(steeringOutput);
                tempVector.set(steeringOutput.linear).scl(delta * MAX_LINEAR_SPEED * 150f);

                float linearVelocityLen = body.getLinearVelocity().len();
                if (linearVelocityLen < MAX_LINEAR_SPEED) {
                    body.activate();
                    body.applyCentralImpulse(tempVector.set(tempVector.x, 0, tempVector.z));
                }

                body.getWorldTransform().getTranslation(position);

                attemptAutoClimb(tempVector);
            }
        }
    }

    public boolean isReachedTarget() {
        return reachedTarget;
    }

    public void updatePath() {
        if (!pathQueue.isEmpty()) {
            Node targetNode = pathQueue.first();
            reachedTarget = position.dst(targetNode.position) < 6f;
            handleIdle();
            if (reachedTarget){
                comeback = false;
                reachNextNode();
            }
        }
    }

    private void reachNextNode() {
        lastNode = previousNode;

        Node nextNode = pathQueue.first();
        position.set(nextNode.position);
        previousNode = nextNode;
        pathQueue.removeFirst();

        if (pathQueue.size != 0) {
            updateSeekBehavior(pathQueue.first());
        }
    }

    public void reachDestination() {
        Node newGoal = NPCComponent.getInstance(null, null).getTargetNode();
        setGoal(newGoal);
        endNode = newGoal;
    }

    private void reachDestination(Node node) {
        Node newGoal = node;
        setGoal(newGoal);
        endNode = newGoal;
    }

    public void setNodeGraph(NodeGraph nodeGraph){
        this.nodeGraph = nodeGraph;
    }

    public void setGoal(Node goal) {
        GraphPath<Node> graphPath = nodeGraph.findPath(previousNode, goal);
        for (int i = 1; i < graphPath.getCount(); i++) {
            pathQueue.addLast(graphPath.get(i));
        }
        if (!pathQueue.isEmpty()) {
            updateSeekBehavior(pathQueue.first());
        }
    }

    public Vector3 getBodyPosition(){
        return body.getWorldTransform().getTranslation(new Vector3());
    }

    private void updateSeekBehavior(Node targetNode) {
        seekBehavior.setTarget(new TargetLocation(targetNode.position));
    }

    @Override
    public void reset() {
        position = null;
        previousNode = null;
        endNode = null;
        pathQueue.clear();
        body = null;
        seekBehavior = null;
        blendedSteering = null;
    }

    // Методы интерфейса Steerable
    @Override
    public Vector3 getPosition() {
        return position;
    }

    @Override
    public float getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    @Override
    public float vectorToAngle(Vector3 vector) {
        return MathUtils.atan2(vector.z, vector.x);
    }

    @Override
    public Vector3 angleToVector(Vector3 outVector, float angle) {
        return outVector.set(MathUtils.cos(angle), 0, MathUtils.sin(angle));
    }

    @Override
    public Vector3 getLinearVelocity() {
        return velocityVector;
    }

    @Override
    public float getAngularVelocity() {
        return 0;
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return 0.01f;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {
    }

    @Override
    public float getMaxLinearSpeed() {
        return MAX_LINEAR_SPEED;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {
    }

    @Override
    public float getMaxLinearAcceleration() {
        return MAX_LINEAR_ACCELERATION;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
    }

    @Override
    public float getMaxAngularSpeed() {
        return MAX_ANGULAR_SPEED;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {
    }

    @Override
    public float getMaxAngularAcceleration() {
        return MAX_ANGULAR_ACCELERATION;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
    }

    @Override
    public Location<Vector3> newLocation() {
        return new TargetLocation(PhysicComponent.getInstance().getVectorPool().obtain());
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void dispose() {

        if (portal != null) {
            portal.dispose();
            portal = null;
        }

        // Dispose of model and instance
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }

        // Dispose of bullet body
        if (body != null) {
            body.dispose();
            body = null;
        }

        // Dispose of entities
        if (entity != null) {
            EntityComponent.getInstance(null).removeEntity(entity);
            entity = null;
        }
        if (teleport != null) {
            EntityComponent.getInstance(null).removeEntity(teleport);
            teleport = null;
        }

        // Return vectors to pools
        PhysicComponent.getInstance().getVectorPool().free(tempVector);
        PhysicComponent.getInstance().getVectorPool().free(velocityVector);
        PhysicComponent.getInstance().getVectorPool().free(rayFrom);
        PhysicComponent.getInstance().getVectorPool().free(rayTo);
        PhysicComponent.getInstance().getVectorPool().free(hit);
        PhysicComponent.getInstance().getVectorPool().free(steeringOutput.linear);

        // Return ray callback to pool
        if (rayCallback != null) {
            PhysicComponent.getInstance().getRaycastNpcPool().free(rayCallback);
            rayCallback = null;
        }

        // Clear path queue
        pathQueue.clear();

        // Clear state machine
        if (stateMachine != null) {
            stateMachine = null;
        }

        // Clear texture atlas arrays
        if (stays != null) {
            for (Array<TextureAtlas.AtlasRegion> stay : stays) {
                if (stay != null) stay.clear();
            }
            stays = null;
        }
        if (atacks != null) {
            for (Array<TextureAtlas.AtlasRegion> atack : atacks) {
                if (atack != null) atack.clear();
            }
            atacks = null;
        }
        if (parts != null) {
            parts.clear();
            parts = null;
        }

        look.dispose();
        look = null;
        pain.dispose();
        pain = null;
        death.dispose();
        death = null;
        atack.dispose();
        atack = null;
    }

    public void setFirstSeenSound(SoundBuffer load) {
        look = load;
    }

    public void setDamageTakenSound(SoundBuffer load) {
        pain = load;
    }

    public void setDeathSound(SoundBuffer load) {
        death = load;
    }

    public void setAttackSound(SoundBuffer load) {
        atack = load;
    }

    public void setPosition(Node start, float boundingRadius) {
        this.start = start;
        this.boundingRadius = boundingRadius;
    }

    public void setHealth(float health) {
        this.previousHealth = health;
        this.body.setCurrentHealth((int) health);
    }

    public static class TargetLocation implements Location<Vector3> {
        private final Vector3 position;

        public TargetLocation(Vector3 position) {
            this.position = position;
        }

        @Override
        public Vector3 getPosition() { return position; }

        @Override
        public float vectorToAngle(Vector3 vector) {
            return MathUtils.atan2(vector.z, vector.x);
        }

        @Override
        public Vector3 angleToVector(Vector3 outVector, float angle) {
            return outVector.set(MathUtils.cos(angle), 0, MathUtils.sin(angle));
        }

        @Override
        public float getOrientation() { return 0; }

        @Override
        public void setOrientation(float orientation) {}

        @Override
        public Location<Vector3> newLocation() {
            return new TargetLocation(PhysicComponent.getInstance().getVectorPool().obtain());
        }
    }
}
