/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.authlib.GameProfile;
import motordevelopment.motorclient.mixininterface.IChatHudLineVisible;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.Visible.class)
public abstract class ChatHudLineVisibleMixin implements IChatHudLineVisible {
    @Shadow @Final private OrderedText content;
    @Unique private int id;
    @Unique private GameProfile sender;
    @Unique private boolean startOfEntry;

    @Override
    public String motor$getText() {
        StringBuilder sb = new StringBuilder();

        content.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        return sb.toString();
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

    @Override
    public boolean motor$isStartOfEntry() {
        return startOfEntry;
    }

    @Override
    public void motor$setStartOfEntry(boolean start) {
        startOfEntry = start;
    }
}
