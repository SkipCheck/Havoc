package com.hexedrealms.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.hexedrealms.screens.Level;

public class CustomRenderableProvider implements RenderableProvider {
    private final Pool<Renderable> renderablePool;
    private final Pool<BoundingBox> boundingBoxPool = new Pool<BoundingBox>() {
        @Override
        protected BoundingBox newObject() {
            return new BoundingBox();
        }
    };

    private final Array<RenderableEntry> renderableEntries;
    private final Vector3 tmpVec = new Vector3();

    // Кэширование видимых объектов
    private int lastFrame = -1;
    private final Array<Renderable> lastVisible = new Array<>();

    private static class RenderableEntry {
        final Renderable renderable;
        final BoundingBox boundingBox;
        final String id;

        RenderableEntry(Renderable renderable, BoundingBox boundingBox) {
            this.renderable = renderable;
            this.boundingBox = boundingBox;
            this.id = renderable.meshPart.id;
        }
    }

    public CustomRenderableProvider(Array<Renderable> renderables, Pool<Renderable> renderablePool) {
        this.renderablePool = renderablePool;
        this.renderableEntries = new Array<>(renderables.size);

        // Предварительное вычисление bounding box
        for (Renderable r : renderables) {
            BoundingBox box = boundingBoxPool.obtain();
            r.meshPart.mesh.calculateBoundingBox(box);
            box.mul(r.worldTransform);
            RenderableEntry entry = new RenderableEntry(r, box);
            renderableEntries.add(entry);
        }
    }

    private void collectVisible(Array<Renderable> result, Camera camera) {
        for (RenderableEntry entry : renderableEntries) {
            if (camera.frustum.boundsInFrustum(entry.boundingBox)) {
                result.add(entry.renderable);
            }
        }
    }

    private BoundingBox calculateBoundingBox(Renderable renderable) {
        BoundingBox box = boundingBoxPool.obtain();
        renderable.meshPart.mesh.calculateBoundingBox(box);
        box.mul(renderable.worldTransform);

        RenderableEntry entry = new RenderableEntry(renderable, box);
        renderableEntries.add(entry);

        return box;
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        int currentFrame = (int) Gdx.graphics.getFrameId();
        if (currentFrame == lastFrame) {
            renderables.addAll(lastVisible);
            return;
        }

        lastVisible.clear();
        collectVisible(lastVisible, Level.getInstance().getPlayer().getCamera());
        renderables.addAll(lastVisible);
        lastFrame = currentFrame;
    }

    public void dispose() {
        this.renderableEntries.clear();
        this.renderablePool.clear();

        for (RenderableEntry entry : renderableEntries) {
            boundingBoxPool.free(entry.boundingBox);
        }
        boundingBoxPool.clear();
    }
}
