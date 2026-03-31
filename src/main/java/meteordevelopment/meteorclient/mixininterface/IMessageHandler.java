/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;

public interface IMessageHandler {
    /**
     * Only valid inside of {@link net.minecraft.client.gui.hud.ChatHud#addMessage(Component, MessageSignature, GuiMessageTag)} call
     */
    GameProfile meteor$getSender();
}
