/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IBakedQuad {
    float meteor$getX(int vertexI);

    float meteor$getY(int vertexI);

    float meteor$getZ(int vertexI);
}
