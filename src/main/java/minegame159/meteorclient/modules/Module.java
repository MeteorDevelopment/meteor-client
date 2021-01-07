/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.ModuleScreen;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.player.Chat;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Formatting;

import java.util.Objects;

public abstract class Module implements Listenable, ISerializable<Module> {
    protected final MinecraftClient mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final Color color;

    public final Settings settings = new Settings();

    private boolean active;
    private boolean visible = true;

    public boolean serialize = true;

    private int key = -1;
    public boolean toggleOnKeyRelease = false;

    public Module(Category category, String name, String description) {
        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);
    }

    public WidgetScreen getScreen() {
        return new ModuleScreen(this);
    }

    public WWidget getWidget() {
        return null;
    }

    public void openScreen() {
        mc.openScreen(getScreen());
    }

    public void doAction(boolean onActivateDeactivate) {
        toggle(onActivateDeactivate);
    }
    public void doAction() {
        doAction(true);
    }

    public LiteralArgumentBuilder<CommandSource> buildCommand() {
        LiteralArgumentBuilder<CommandSource> builder;

        builder = LiteralArgumentBuilder.literal(name);

        builder.executes(context -> {
            this.toggle();
            return Command.SINGLE_SUCCESS;
        });

        return builder;
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle(boolean onActivateDeactivate) {
        if (!active) {
            active = true;
            ModuleManager.INSTANCE.addActive(this);

            for (SettingGroup sg : settings) {
                for (Setting setting : sg) {
                    if (setting.onModuleActivated != null) setting.onModuleActivated.accept(setting);
                }
            }

            if (onActivateDeactivate) {
                MeteorClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
        else {
            active = false;
            ModuleManager.INSTANCE.removeActive(this);

            if (onActivateDeactivate) {
                MeteorClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }
        }
    }
    public void toggle() {
        toggle(true);
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public CompoundTag toTag() {
        if (!serialize) return null;
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putInt("key", key);
        tag.putBoolean("toggleOnKeyRelease", toggleOnKeyRelease);
        tag.put("settings", settings.toTag());

        tag.putBoolean("active", active);
        tag.putBoolean("visible", visible);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        // General
        key = tag.getInt("key");
        toggleOnKeyRelease = tag.getBoolean("toggleOnKeyRelease");

        // Settings
        Tag settingsTag = tag.get("settings");
        if (settingsTag instanceof CompoundTag) settings.fromTag((CompoundTag) settingsTag);

        boolean active = tag.getBoolean("active");
        if (active != isActive()) toggle(Utils.canUpdate());
        setVisible(tag.getBoolean("visible"));

        return this;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        MeteorClient.EVENT_BUS.post(EventStore.moduleVisibilityChangedEvent(this));
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isActive() {
        return active;
    }

    public void sendToggledMsg() {
        if (Config.INSTANCE.chatCommandsInfo) Chat.info(159159, null, "Toggled (highlight)%s(default) %s(default).", title, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
    }

    public void setKey(int key, boolean postEvent) {
        this.key = key;
        if (postEvent) MeteorClient.EVENT_BUS.post(EventStore.moduleBindChangedEvent(this));
    }
    public void setKey(int key) {
        setKey(key, true);
    }

    public int getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
