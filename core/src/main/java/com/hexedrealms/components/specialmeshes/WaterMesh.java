package com.hexedrealms.components.specialmeshes;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;

public class WaterMesh {
    protected Texture texture;
    protected Mesh mesh;
    protected Matrix4 transform;
    protected BoundingBox boundingBox;

    public WaterMesh(Node node) {
        boundingBox = new BoundingBox();

        node.parts.forEach(part -> {
            if (part.material.has(TextureAttribute.Diffuse)) {
                node.calculateBoundingBox(boundingBox);

                TextureAttribute attribute = (TextureAttribute) part.material.get(TextureAttribute.Diffuse);
                this.texture = attribute.textureDescription.texture;
                this.mesh = part.meshPart.mesh;
                this.transform = node.globalTransform;

                texture.setWrap(attribute.textureDescription.uWrap, attribute.textureDescription.vWrap);
                texture.setFilter(attribute.textureDescription.minFilter, attribute.textureDescription.magFilter);
            }
        });
    }

    // Существующие методы остаются без изменений
    public Mesh getMesh() {
        return mesh;
    }

    public Matrix4 getTransform() {
        return transform;
    }

    public Texture getTexture() {
        return texture;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
