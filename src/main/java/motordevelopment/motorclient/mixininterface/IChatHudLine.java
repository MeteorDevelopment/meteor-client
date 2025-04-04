/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import com.mojang.authlib.GameProfile;

public interface IChatHudLine {
    String motor$getText();

    int motor$getId();

    void motor$setId(int id);

    GameProfile motor$getSender();

    void motor$setSender(GameProfile profile);
}
