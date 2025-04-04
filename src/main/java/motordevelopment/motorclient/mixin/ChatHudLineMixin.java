/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.authlib.GameProfile;
import motordevelopment.motorclient.mixininterface.IChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChatHudLine.class)
public abstract class ChatHudLineMixin implements IChatHudLine {
    @Shadow @Final private Text content;
    @Unique private int id;
    @Unique private GameProfile sender;

    @Override
    public String motor$getText() {
        return content.getString();
    }

    @Override
    public int motor$getId() {
        return id;
    }

    @Override
    public void motor$setId(int id) {
        this.id = id;
    }

    @Override
    public GameProfile motor$getSender() {
        return sender;
    }

    @Override
    public void motor$setSender(GameProfile profile) {
        sender = profile;
    }
}
