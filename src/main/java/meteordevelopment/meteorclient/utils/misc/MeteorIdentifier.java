/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.Identifier;

public class MeteorIdentifier {
    public MeteorIdentifier(String path) {
        Identifier.of(MeteorClient.MOD_ID, path);
    }
}
