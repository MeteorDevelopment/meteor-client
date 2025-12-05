/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import meteordevelopment.meteorclient.systems.targeting.SavedPlayer;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Deprecated // systems.targeting.SavedPlayer
public class Friend extends SavedPlayer {
    public Friend(String name, @Nullable UUID id) {
        super(name, id);
    }

    public Friend(PlayerEntity player) {
        super(player);
    }

    public Friend(String name) {
        super(name);
    }
}
