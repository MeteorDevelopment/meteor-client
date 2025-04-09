/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.macros;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Macro implements ISerializable<Macro> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the macro.")
        .build()
    );

    public Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("The messages for the macro to send.")
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<Boolean> useDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("use-delay")
        .description("Use delay between sending messages.")
        .defaultValue(false)
        .build()
    );

    public Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in ticks.")
        .defaultValue(20)
        .visible(useDelay::get)
        .min(1)
        .sliderMax(200)
        .build()
    );


    public Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The bind to run the macro.")
        .build()
    );

    public Macro() {}
    public Macro(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public boolean onAction(boolean isKey, int value, int modifiers) {
        if (!keybind.get().matches(isKey, value, modifiers) || mc.currentScreen != null) return false;
        return onAction();
    }

    private final List<Object[]> toSend = new ArrayList<>();

    public boolean onAction() {
            if (useDelay.get()) {
                if (mc.world == null) return false;
                for (int i = 0; i < messages.get().size(); i++) {
                    toSend.add(new Object[]{mc.world.getTime() + (delay.get() * i), messages.get().get(i)});
                }
            } else {
                for (String message : messages.get()) {
                    Script script = MeteorStarscript.compile(message);
                    if (script != null) {
                        String messageToSend = MeteorStarscript.run(script);
                        ChatUtils.sendPlayerMsg(messageToSend);
                    }
                }
            }

            return true;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null) return;
        for (Object[] row : new ArrayList<>(toSend)) {
            if ((long) row[0] <= mc.world.getTime()) {
                Script script = MeteorStarscript.compile((String) row[1]);
                if (script != null) {
                    String messageToSend = MeteorStarscript.run(script);
                    ChatUtils.sendPlayerMsg(messageToSend);
                }
                toSend.remove(row);
            }
        }
    }

    @EventHandler
    public void onGameLeft(GameLeftEvent event) {
        toSend.clear();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public Macro fromTag(NbtCompound tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompound("settings"));
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Macro macro = (Macro) o;
        return Objects.equals(macro.name.get(), this.name.get());
    }
}
