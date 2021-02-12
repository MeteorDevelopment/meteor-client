/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.Proxy;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("currentFps")
    int getFps();

    @Accessor("session")
    void setSession(Session session);

    @Accessor("netProxy")
    Proxy getProxy();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int itemUseCooldown);

    @Invoker("doAttack")
    void leftClick();
}
