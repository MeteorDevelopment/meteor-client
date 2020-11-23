/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.macros;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.KeyAction;
import minegame159.meteorclient.utils.NbtUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Macro implements Listenable, ISerializable<Macro> {
    public String name = "";
    public List<String> messages = new ArrayList<>(1);
    public int key = -1;

    public void addMessage(String command) {
        messages.add(command);
    }

    public void removeMessage(int i) {
        messages.remove(i);
    }

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (event.action == KeyAction.Press && event.key == key && MinecraftClient.getInstance().currentScreen == null) {
            for (String command : messages) {
                MinecraftClient.getInstance().player.sendChatMessage(command);
            }
            event.cancel();
        }
    });

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        // General
        tag.putString("name", name);
        tag.putInt("key", key);

        // Messages
        ListTag messagesTag = new ListTag();
        for (String message : messages) messagesTag.add(StringTag.of(message));
        tag.put("messages", messagesTag);

        return tag;
    }

    @Override
    public Macro fromTag(CompoundTag tag) {
        name = tag.getString("name");
        key = tag.getInt("key");
        messages = NbtUtils.listFromTag(tag.getList("messages", 8), Tag::asString);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Macro macro = (Macro) o;
        return Objects.equals(name, macro.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
