package com.hexedrealms.engine;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.hexedrealms.components.CustomCameraGroup;
import com.hexedrealms.components.Entity;

import java.util.HashMap;

public class EntityComponent implements Disposable {

    private static EntityComponent instance;

    private Pool<Entity> mEntityPool;
    private Array<Entity> mEntities;
    private DecalBatch mDecalBatch;
    private CustomCameraGroup customCameraGroup;

    public static EntityComponent getInstance(Camera camera) {
        if(instance == null)
            instance = new EntityComponent(camera);
        return instance;
    }

    private EntityComponent(Camera camera){
        customCameraGroup = new CustomCameraGroup(camera);

        mDecalBatch = new DecalBatch(10, customCameraGroup);
        mEntities = new Array<>();
        mEntityPool = Pools.get(Entity.class);
    }

    public void removeEntity(Entity entity){
        mEntities.removeValue(entity, true);
        if(entity != null)
            entity.reset();
    }

    public CustomCameraGroup getCustomCameraGroup() {
        return customCameraGroup;
    }

    public void addEntity(Entity entity){
        mEntities.add(entity);
    }

    public void addNodeEntity(Node node, boolean isRigid, boolean isStatic) {
        Entity entity = mEntityPool.obtain();
        entity.setStatic(isStatic);

        // Get texture attributes - we'll look for diffuse first, then others if needed
        Array<TextureAtlas.AtlasRegion> regions = new Array<>();

        // First try to get diffuse texture
        for(NodePart part : node.parts) {
            TextureAttribute diffuseAttr = part.material.get(TextureAttribute.class, TextureAttribute.Diffuse);
            if (diffuseAttr != null) {
                processTextureAttribute(diffuseAttr, part.meshPart.mesh, regions);
            }
        }

// Initialize entity with either single region or array of regions
        if (regions.size == 1) {
            entity.init(regions.first(), node.translation, node.rotation, isRigid);
        } else if (regions.size > 1) {
            entity.init(regions, node.translation, node.rotation, isRigid);
        } else {
            // Handle case with no textures (maybe use a default texture?)
            throw new RuntimeException("No textures found for node entity");
        }

        mEntities.add(entity);
    }

    private void processTextureAttribute(TextureAttribute textureAttr, Mesh mesh, Array<TextureAtlas.AtlasRegion> regions) {
        Texture texture = textureAttr.textureDescription.texture;
        VertexAttribute uvAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.TextureCoordinates);

        if (uvAttr == null) {
            // Если нет UV - создаём регион на всю текстуру
            TextureAtlas.AtlasRegion fullRegion = new TextureAtlas.AtlasRegion(new TextureRegion(texture));
            fullRegion.flip(true, false);
            regions.add(fullRegion);
            return;
        }

        // Получаем все вершины меша
        float[] vertices = new float[mesh.getNumVertices() * (mesh.getVertexSize() / 4)];
        mesh.getVertices(vertices);

        int stride = mesh.getVertexSize() / 4;
        int uvOffset = uvAttr.offset / 4;
        int verticesPerChunk = 4; // Количество вершин в одном "подмеше"

        // Обрабатываем меш порциями по verticesPerChunk вершин
        for (int chunkStart = 0; chunkStart < mesh.getNumVertices(); chunkStart += verticesPerChunk) {
            int chunkEnd = Math.min(chunkStart + verticesPerChunk, mesh.getNumVertices());

            // Рассчитываем UV-границы для текущего куска
            float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;

            for (int i = chunkStart; i < chunkEnd; i++) {
                int pos = i * stride + uvOffset;
                float u = vertices[pos];
                float v = 1 - vertices[pos + 1]; // Инвертируем V

                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }

            // Создаём регион для текущего куска
            int x = (int)(minU * texture.getWidth());
            int y = (int)((1 - maxV) * texture.getHeight());
            int width = (int)((maxU - minU) * texture.getWidth());
            int height = (int)((maxV - minV) * texture.getHeight());

            // Гарантируем минимальный размер и корректные границы
            width = Math.max(1, width);
            height = Math.max(1, height);
            x = Math.max(0, Math.min(x, texture.getWidth() - width));
            y = Math.max(0, Math.min(y, texture.getHeight() - height));

            TextureRegion chunkRegion = new TextureRegion(texture, x, y, width, height);
            chunkRegion.flip(true, false);

            regions.add(new TextureAtlas.AtlasRegion(chunkRegion));
        }
    }

    public DecalBatch getDecalBatch(){
        return mDecalBatch;
    }

    public void render(Camera camera, float delta, boolean isPaused){
        if(mEntities.isEmpty()) return;

        for(Entity entity : mEntities) {
            entity.render(mDecalBatch, camera, delta, isPaused);
        }

        mDecalBatch.flush();
    }

    public Pool<Entity> getPool(){
        return mEntityPool;
    }

    @Override
    public void dispose() {
        for(Entity entity : mEntities)
            mEntityPool.free(entity);

        mEntities.clear();
        mDecalBatch.dispose();
        customCameraGroup.dispose();

        mDecalBatch = null;
        mEntities = null;
        customCameraGroup = null;
        instance = null;
    }
}
