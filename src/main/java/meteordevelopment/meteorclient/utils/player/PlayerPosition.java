/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.util.math.Vec3d;

public record PlayerPosition(Vec3d pos, float yaw, float pitch) {
}
