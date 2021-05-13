/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.misc.Keybind;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Formatting;

import java.util.Objects;

public abstract class Module implements ISerializable<Module> {
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

    public final Keybind keybind = Keybind.fromKey(-1);
    public boolean toggleOnBindRelease = false;

    public Module(Category category, String name, String description) {
        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle(boolean onToggle) {
        if (!active) {
            active = true;
            Modules.get().addActive(this);

            settings.onActivated();

            if (onToggle) {
                MeteorClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
        else {
            if (onToggle) {
                MeteorClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }

            active = false;
            Modules.get().removeActive(this);
        }
    }

    public void toggle() {
        toggle(true);
    }

    public void sendToggledMsg() {
        if (Config.get().chatCommandsInfo) ChatUtils.info(this.hashCode(), "Toggled (highlight)%s(default) %s(default).", title, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isActive() {
        return active;
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public CompoundTag toTag() {
        if (!serialize) return null;
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.put("settings", settings.toTag());

        tag.putBoolean("active", active);
        tag.putBoolean("visible", visible);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        // General
        if (tag.contains("key")) keybind.set(true, tag.getInt("key"));
        else keybind.fromTag(tag.getCompound("keybind"));

        toggleOnBindRelease = tag.getBoolean("toggleOnKeyRelease");

        // Settings
        Tag settingsTag = tag.get("settings");
        if (settingsTag instanceof CompoundTag) settings.fromTag((CompoundTag) settingsTag);

        boolean active = tag.getBoolean("active");
        if (active != isActive()) toggle(Utils.canUpdate());
        setVisible(tag.getBoolean("visible"));

        return this;
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
