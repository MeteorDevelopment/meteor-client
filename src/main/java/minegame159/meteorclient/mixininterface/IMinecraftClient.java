/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import net.minecraft.client.util.Session;

import java.net.Proxy;

public interface IMinecraftClient {
    void leftClick();

    void rightClick();

    void setItemUseCooldown(int cooldown);

    Proxy getProxy();

    void setSession(Session session);

    int getFps();
}
