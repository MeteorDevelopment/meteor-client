/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public interface IMessageHandler {
    /** Only valid inside of {@link net.minecraft.client.gui.components.ChatComponent#addMessage(Component, MessageSignature, GuiMessageTag)} call */
    GameProfile meteor$getSender();
}
