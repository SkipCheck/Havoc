package com.hexedrealms.utils.NPCSteering;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.steer.behaviors.Seek;
import com.badlogic.gdx.math.Vector3;
import com.hexedrealms.components.NPC;
import com.hexedrealms.components.bulletbodies.PlayerBody;
import com.hexedrealms.configurations.AudioConfiguration;
import com.hexedrealms.engine.DamageComponent;
import com.hexedrealms.engine.EntityComponent;
import com.hexedrealms.engine.NPCComponent;
import com.hexedrealms.engine.PhysicComponent;
import com.hexedrealms.screens.Level;
import com.hexedrealms.utils.damage.Enemy;

public enum WalkerState implements State<NPC> {
    // Состояния базового блуждающего НПС
    SPAWN {
        @Override
        public void update(NPC npc) {
            if (npc.isSpawned()) {
                npc.removeTeleport();
                Vector3 position = npc.getPosition();
                npc.getInstance().transform.setTranslation(position.x, position.y + 2f, position.z);
                npc.setBody(PhysicComponent.getInstance().addBotBody(npc.getInstance()));
                npc.getStateMachine().changeState(WALKER);

                EntityComponent.getInstance(null).addEntity(npc.getEntity());
            }
        }
    },
    SEARCHING {
        @Override
        public void update(NPC npc) {
            handleSeekBehavior(npc);
            Vector3 npcPosition = npc.getBody().getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain());
            Vector3 playerPosition = Level.getInstance().getPlayer().getCamera().position;

            if (npcPosition.dst(playerPosition) < 15) {
                npc.getPathQueue().clear();
                updateTargetAndChangeState(npc, ATACK);
            }
        }
    },
    WALKER {
        @Override
        public void update(NPC npc) {
            handleWalkBehavior(npc);

            if (getDstToPlayer(npc) < 30 && npc.lookAtPlayer() || npc.isDamaged()) {
                npc.look.play3D(AudioConfiguration.SOUND.getValue() * 2f, npc.getPosition(), 20f, 40f);
                updateTargetAndChangeState(npc, SEARCHING);
            }
        }
    },
    ATACK {
        @Override
        public void update(NPC npc) {
            float dst = getDstToPlayer(npc);

            if (dst > 5) {
                if(npc.getAnimation())
                    npc.updateMovement();
            } else {
                if(npc.getAnimation()) {

                    if (npc.getAnimation()) {
                        Vector3 hit = npc.checkPointHit();
                        PlayerBody body = (PlayerBody) Level.getInstance().getPlayer().getEnemy();
                        body.setPositionHit(hit);

                        DamageComponent.getInstance().checkDamage((Enemy) npc.getBody(), body, npc.getDamageType());
                        npc.getEntity().getAnimation().setFinalled(false);
                    }
                    npc.setPunch();
                }
            }

            if (!npc.lookAtPlayer()) {
                resetToSearchingState(npc);
            }
        }
    },
    IDLE {
        @Override
        public void update(NPC npc) {
        }
    };

    // Вспомогательные функции для обновления состояния НПС
    private static void handleSeekBehavior(NPC npc) {
        Seek<Vector3> seek = npc.getSeekBehavior();

        if (seek.getTarget() != null) {
            if (npc.getEndNode() != NPCComponent.getInstance(null, null).getTargetNode() && !npc.isComeback()) {
                npc.getPathQueue().clear();
                npc.reachDestination();
            }
        }

        npc.updateMovement();
        npc.updatePath();
    }
    private static float getDstToPlayer(NPC npc){
        Vector3 npcPosition = npc.getBody().getWorldTransform().getTranslation(PhysicComponent.getInstance().getVectorPool().obtain());
        Vector3 playerPosition = Level.getInstance().getPlayer().getCamera().position;
        return  npcPosition.dst(playerPosition);
    }
    private static void handleWalkBehavior(NPC npc) {
        if (npc.borderPauseTimer > npc.borderPauseDuration) {
            npc.reachWalk();
            npc.clearTimer();
        }

        if (npc.getPathQueue().isEmpty()) {
            npc.updateTimer();
        } else {
            npc.updateMovement();
            npc.updatePath();
        }

    }
    private static void updateTargetAndChangeState(NPC npc, WalkerState newState) {
        Seek<Vector3> seek = npc.getSeekBehavior();
        seek.setTarget(new NPC.TargetLocation(Level.getInstance().getPlayer().getCamera().position));
        npc.getStateMachine().changeState(newState);
    }
    private static void resetToSearchingState(NPC npc) {
        npc.getPathQueue().clear();
        npc.setCurrentNode(NPCComponent.getInstance(null, null).findNearestNode(npc.getPosition()));
        npc.getStateMachine().changeState(SEARCHING);
    }

    // Встроенные функции интерфейса State модуля gdx-ai
    @Override
    public void enter(NPC entity) {
    }

    @Override
    public void exit(NPC entity) {
    }
    @Override
    public boolean onMessage(NPC entity, Telegram telegram) {
        return false;
    }
}
