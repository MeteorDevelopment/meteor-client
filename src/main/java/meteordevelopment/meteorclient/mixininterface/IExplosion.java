/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.util.math.Vec3d;

public interface IExplosion {
    void set(Vec3d pos, float power, boolean createFire);
}
