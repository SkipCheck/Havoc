package com.hexedrealms.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.hexedrealms.configurations.ControlsConfiguration;
import com.hexedrealms.configurations.PlayerConfiguration;
import com.hexedrealms.configurations.PlayerMotionConfiguration;

public class PlayerMotion extends btMotionState {

    // Movement constants
    private final int MAX_GLIDE = 3;
    private final float MAX_SLEEP_GLIDE = 3f;
    private final float BOUNCE_JUMP_VELOCITY_THRESHOLD = 2f;
    private final float BOUNCE_JUMP_IMPULSE = 15f;
    private final float BOUNCE_JUMP_COOLDOWN = 0.2f;
    private final int MAX_BOUNCE_JUMPS = 1;

    // State variables
    private boolean isJumping = false;
    private float hopTimeRemaining = 0f;
    private int currentGlide = MAX_GLIDE;
    private float time;
    private int currentBounceJumps = 0;
    private float timeSinceLastJump = 0f;
    private boolean canBounceJump = false;
    private boolean wasOnGround = true;

    // Physics references
    private final Matrix4 mTransformPlayer;
    private btRigidBody mPlayer;
    private btRigidBody sensorBody;

    // Vector pools
    private final Vector3 mPosition = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 mMovement = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 mNormalInWorld = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 mNormalAuto = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 mHit = PhysicComponent.getInstance().getVectorPool().obtain();
    private final ClosestRayResultCallback mCallback;

    // Cached vectors
    private final Vector3 cachedForward = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 cachedRightDirection = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 cachedVelocity = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 cachedRayFrom = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 cachedRayTo = PhysicComponent.getInstance().getVectorPool().obtain();
    private final Vector3 lastGroundPoint = PhysicComponent.getInstance().getVectorPool().obtain();
    public final Vector3 mTempPosition = PhysicComponent.getInstance().getVectorPool().obtain();

    public PlayerMotion(Matrix4 matrix4) {
        this.mTransformPlayer = matrix4;
        this.mCallback = PhysicComponent.getInstance().getRaycastCallbackPool().obtain();
        this.mCallback.setRayFromWorld(Vector3.Zero);
        this.mCallback.setRayToWorld(Vector3.Y);
    }

    public void setSensorBody(btRigidBody sensorBody) {
        this.sensorBody = sensorBody;
    }

    public btRigidBody getSensorBody() {
        return sensorBody;
    }

    public void setPlayerBody(btRigidBody pPlayer) {
        this.mPlayer = pPlayer;
    }

    public void updateMovement(Vector3 directionalSpeed, float speed, int groundContactCount, float delta) {
        mMovement.setZero();
        mPlayer.setFriction(0);

        cachedForward.set(directionalSpeed).scl(PlayerConfiguration.BOOST.getValue());
        cachedRightDirection.set(cachedForward).crs(Vector3.Y);
        processRayCast();

        boolean isMoved = processInput(speed, cachedForward, cachedRightDirection);

        if (currentGlide > 0 && !mMovement.isZero() && Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) {
            Vector3 boostDirection = PhysicComponent.getInstance().getVectorPool().obtain().set(mMovement);
            PostProcessorComponent.getInstance().enableRadialBlur(true);
            Vector3 boostImpulse = boostDirection.scl(3000f);
            mPlayer.applyCentralImpulse(boostImpulse);
            currentGlide--;
        }

        if (currentGlide == 0) {
            time += delta;
            if (time > MAX_SLEEP_GLIDE) {
                time = 0;
                currentGlide = MAX_GLIDE;
            }
        }

        if (isMoved) {
            attemptAutoClimb(mMovement.cpy().scl(1f / speed));
        }

        if (PhysicComponent.getInstance().checkOnGround()) {
            lastGroundPoint.set(mPosition);
        }

        updatePlayerCondition(isMoved, groundContactCount, directionalSpeed, delta);
    }

    private boolean processInput(float speed, Vector3 forward, Vector3 rightDirection) {
        boolean isMoved = false;

        if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_FORWARD.getValue())) {
            mMovement.add(forward.scl(speed));
            isMoved = true;
        } else if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_BACKWARD.getValue())) {
            mMovement.sub(forward.scl(speed));
            isMoved = true;
        }

        if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_RIGHT.getValue())) {
            mMovement.add(rightDirection.scl(speed * 1.1f));
            isMoved = true;
        } else if (Gdx.input.isKeyPressed((Integer) ControlsConfiguration.MOVE_LEFT.getValue())) {
            mMovement.sub(rightDirection.scl(speed * 1.1f));
            isMoved = true;
        }

        return isMoved;
    }

    private void updateSensorPosition() {
        Matrix4 sensorTransform = sensorBody.getWorldTransform();
        sensorTransform.setTranslation(mTempPosition);
        sensorBody.setWorldTransform(sensorTransform);
    }

    private void processRayCast() {
        cachedRayFrom.set(mPosition);
        cachedRayTo.set(cachedRayFrom).sub(0, PlayerConfiguration.PLAYER_HEIGHT.getValue(), 0);

        mCallback.setCollisionObject(null);
        mCallback.setClosestHitFraction(1f);
        mCallback.setRayFromWorld(cachedRayFrom);
        mCallback.setRayToWorld(cachedRayTo);

        PhysicComponent.getInstance().getPhysicWorld().rayTest(cachedRayFrom, cachedRayTo, mCallback);
        if (mCallback.hasHit()) {
            mCallback.getHitNormalWorld(mNormalInWorld);
        }
    }

    public boolean isDropped() {
        return lastGroundPoint.y - mPosition.y > 3f && PhysicComponent.getInstance().checkOnGround();
    }

    public void attemptAutoClimb(Vector3 directionMove) {
        float height = PlayerConfiguration.PLAYER_HEIGHT.getValue();

        mCallback.setCollisionObject(null);
        mCallback.setClosestHitFraction(1f);

        cachedRayFrom.set(mPosition).add(0, height * 0.5f, 0);
        cachedRayTo.set(cachedRayFrom).add(directionMove.nor().scl(2f));
        mCallback.setRayFromWorld(cachedRayFrom);
        mCallback.setRayToWorld(cachedRayTo);

        PhysicComponent.getInstance().getPhysicWorld().rayTest(cachedRayFrom, cachedRayTo, mCallback);

        if (!mCallback.hasHit()) {
            cachedRayFrom.set(mPosition).add(directionMove.nor().scl(1.2f)).add(0, height * 0.5f, 0);
            cachedRayTo.set(cachedRayFrom).sub(0, height, 0);

            mCallback.setRayFromWorld(cachedRayFrom);
            mCallback.setRayToWorld(cachedRayTo);

            PhysicComponent.getInstance().getPhysicWorld().rayTest(cachedRayFrom, cachedRayTo, mCallback);

            if (mCallback.hasHit()) {
                mCallback.getHitPointWorld(mHit);
                mCallback.getHitNormalWorld(mNormalAuto);
                mNormalAuto.nor();

                int flag = mCallback.getCollisionObject().getCollisionFlags();

                if (flag != 5) {
                    float hitDST = Math.abs(mHit.y - mPosition.y);
                    if (mNormalAuto.y > PlayerMotionConfiguration.CLIMB_NORMAL_THRESHOLD.getValue() && hitDST < height * 0.45f) {
                        cachedVelocity.set(mPlayer.getLinearVelocity());
                        cachedVelocity.y = 12 + (hitDST * height);
                        mPlayer.setLinearVelocity(cachedVelocity);
                    }
                }
            }
        }
    }

    private void updatePlayerCondition(boolean isMoved, int groundContactCount, Vector3 directionalSpeed, float delta) {
        timeSinceLastJump += delta;
        boolean isOnGround = groundContactCount > 0;

        float frictionDampingThisFrame = (float) Math.pow(PlayerMotionConfiguration.FRICTION_DAMPING.getValue(), delta * 60f);
        float maxSpeed = PlayerConfiguration.MAX_SPEED.getValue();
        float hopSpeedBoost = PlayerMotionConfiguration.HOP_SPEED_BOOST.getValue();
        float hopImpulse = PlayerMotionConfiguration.HOP_IMPULSE.getValue();
        float cornerCutThreshold = PlayerMotionConfiguration.CORNER_CUT_THRESHOLD.getValue();
        float cornerCutSmoothing = PlayerMotionConfiguration.CORNER_CUT_SMOOTHING.getValue();

        mNormalAuto.nor();
        if (!isMoved) {
            cachedVelocity.set(mPlayer.getLinearVelocity());
            float friction = Math.abs(1 - mNormalInWorld.y);
            if (friction > 0.002f && friction < 0.1f) {
                friction = 2.5f;
            }

            mPlayer.setFriction(friction);

            cachedVelocity.set(
                cachedVelocity.x * frictionDampingThisFrame,
                cachedVelocity.y,
                cachedVelocity.z * frictionDampingThisFrame
            );
            mPlayer.setLinearVelocity(cachedVelocity);
        } else {
            // Get current horizontal velocity
            Vector3 currentVel = mPlayer.getLinearVelocity();
            Vector3 horizontalVel = new Vector3(currentVel.x, 0, currentVel.z);

            // Only apply corner cutting when landing (just touched ground)
            if (isOnGround && !wasOnGround) {
                // Normalize movement and velocity vectors
                Vector3 desiredMoveDir = new Vector3(mMovement).nor();
                Vector3 currentMoveDir = new Vector3(horizontalVel).nor();

                // Calculate dot product to determine angle between vectors
                float dot = currentMoveDir.dot(desiredMoveDir);

                // If angle is sharp (dot product below threshold)
                if (dot < cornerCutThreshold) {
                    // Smoothly interpolate between current and desired direction
                    desiredMoveDir.scl(1f - cornerCutSmoothing);
                    currentMoveDir.scl(cornerCutSmoothing);
                    desiredMoveDir.add(currentMoveDir).nor();

                    // Calculate new velocity maintaining speed but with adjusted direction
                    float currentSpeed = horizontalVel.len();
                    horizontalVel.set(desiredMoveDir).scl(currentSpeed);

                    // Apply adjusted velocity
                    currentVel.x = horizontalVel.x;
                    currentVel.z = horizontalVel.z;
                    mPlayer.setLinearVelocity(currentVel);
                }
            }

            if (mPlayer.getLinearVelocity().len() < maxSpeed) {
                mPlayer.applyCentralImpulse(mMovement.scl(maxSpeed));
            }
        }

        // Update ground state for next frame
        wasOnGround = isOnGround;

        // Jump handling (unchanged)
        if (Gdx.input.isKeyJustPressed((Integer) ControlsConfiguration.JUMP.getValue())) {
            if (isOnGround) {
                mPlayer.applyCentralImpulse(new Vector3(0, PlayerMotionConfiguration.JUMP_IMPULSE.getValue(), 0));
                isJumping = true;
                hopTimeRemaining = PlayerMotionConfiguration.HOP_DURATION.getValue();
                currentBounceJumps = 0;
                timeSinceLastJump = 0f;
                canBounceJump = false;
            } else if (canBounceJump && currentBounceJumps < MAX_BOUNCE_JUMPS && timeSinceLastJump > BOUNCE_JUMP_COOLDOWN) {
                cachedVelocity.set(mPlayer.getLinearVelocity());
                cachedVelocity.y = BOUNCE_JUMP_IMPULSE;
                mPlayer.setLinearVelocity(cachedVelocity);
                currentBounceJumps++;
                timeSinceLastJump = 0f;
                canBounceJump = false;
            }
        }

        // Check if we're at peak of jump for bounce jump (unchanged)
        if (!isOnGround &&
            mPlayer.getLinearVelocity().y < BOUNCE_JUMP_VELOCITY_THRESHOLD &&
            mPlayer.getLinearVelocity().y > -BOUNCE_JUMP_VELOCITY_THRESHOLD) {
            canBounceJump = true;
        } else {
            canBounceJump = false;
        }

        // Hop boost (unchanged)
        if (isJumping && mPlayer.getLinearVelocity().y > 0 && hopTimeRemaining > 0) {
            cachedVelocity.set(mPlayer.getLinearVelocity());
            cachedVelocity.x *= hopSpeedBoost;
            cachedVelocity.z *= hopSpeedBoost;
            mPlayer.setLinearVelocity(cachedVelocity);
            mPlayer.applyCentralImpulse(new Vector3(cachedVelocity.x * hopImpulse, 0, cachedVelocity.z * hopImpulse));
            hopTimeRemaining -= delta;
        }

        // Reset on ground contact (unchanged)
        if (isOnGround) {
            isJumping = false;
            hopTimeRemaining = 0f;
            currentBounceJumps = 0;
            canBounceJump = false;
        }
    }

    @Override
    public void getWorldTransform(Matrix4 worldTrans) {
        worldTrans.set(mTransformPlayer);
    }

    @Override
    public void setWorldTransform(Matrix4 worldTrans) {
        mTransformPlayer.set(worldTrans);
        mTransformPlayer.getTranslation(mPosition);
        mPlayer.setWorldTransform(mTransformPlayer);
        mTempPosition.set(mPosition).add(0, -PlayerConfiguration.PLAYER_HEIGHT.getValue() / 2f + 0.6f, 0);

        updateSensorPosition();
    }
}
