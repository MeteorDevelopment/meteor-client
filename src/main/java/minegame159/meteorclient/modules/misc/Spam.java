/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WMinus;
import minegame159.meteorclient.gui.widgets.pressable.WPlus;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class Spam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between specified messages in seconds.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> random = sgGeneral.add(new BoolSetting.Builder()
            .name("random")
            .description("Selects a random message from your spam message list.")
            .defaultValue(false)
            .build()
    );

    private final List<String> messages = new ArrayList<>();
    private int timer;
    private int messageI;

    public Spam() {
        super(Categories.Misc, "spam", "Spams specified messages in chat.");
    }

    @Override
    public void onActivate() {
        timer = delay.get() * 20;
        messageI = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messages.isEmpty()) return;

        if (timer <= 0) {
            int i;
            if (random.get()) {
                i = Utils.random(0, messages.size());
            } else {
                if (messageI >= messages.size()) messageI = 0;
                i = messageI++;
            }

            mc.player.sendChatMessage(messages.get(i));

            timer = delay.get() * 20;
        } else {
            timer--;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        messages.removeIf(String::isEmpty);

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.add(theme.horizontalSeparator("Messages")).expandX();
        table.row();

        // Messages
        for (int i = 0; i < messages.size(); i++) {
            int msgI = i;
            String message = messages.get(i);

            WTextBox textBox = table.add(theme.textBox(message)).minWidth(100).expandX().widget();
            textBox.action = () -> messages.set(msgI, textBox.get());

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                messages.remove(msgI);

                table.clear();
                fillTable(theme, table);
            };

            table.row();
        }

        // New Message
        WPlus add = table.add(theme.plus()).expandCellX().right().widget();
        add.action = () -> {
            messages.add("");

            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        messages.removeIf(String::isEmpty);
        ListTag messagesTag = new ListTag();

        for (String message : messages) messagesTag.add(StringTag.of(message));
        tag.put("messages", messagesTag);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        messages.clear();

        if (tag.contains("messages")) {
            ListTag messagesTag = tag.getList("messages", 8);
            for (Tag messageTag : messagesTag) messages.add(messageTag.asString());
        } else {
            messages.add("Meteor on Crack!");
        }

        return super.fromTag(tag);
    }
}
