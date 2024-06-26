/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public interface IEntityRenderer {
    Identifier getTextureInterface(Entity entity);
}
