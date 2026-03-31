/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.mixininterface.IChatHudLine;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// TODO(Ravel): can not resolve target class ChatHudLine
// TODO(Ravel): can not resolve target class ChatHudLine
@Mixin(value = ChatHudLine.class)
public abstract class ChatHudLineMixin implements IChatHudLine {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private Component content;
    @Unique
    private int id;
    @Unique
    private GameProfile sender;

    @Override
    public String meteor$getText() {
        return content.getString();
    }

    @Override
    public int meteor$getId() {
        return id;
    }

    @Override
    public void meteor$setId(int id) {
        this.id = id;
    }

    @Override
    public GameProfile meteor$getSender() {
        return sender;
    }

    @Override
    public void meteor$setSender(GameProfile profile) {
        sender = profile;
    }
}
