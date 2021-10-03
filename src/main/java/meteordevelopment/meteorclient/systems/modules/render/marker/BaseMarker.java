/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.MarkerScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

public abstract class BaseMarker implements ISerializable<BaseMarker> {
    protected final MinecraftClient mc;
    public final Settings settings = new Settings();

    protected final SettingGroup sgName = settings.createGroup("Name");
    public final Setting<String> name = sgName.add(new StringSetting.Builder()
        .name("name")
        .description("Custom name for this marker.")
        .defaultValue("")
        .build()
    );
    protected final Setting<String> description = sgName.add(new StringSetting.Builder()
        .name("description")
        .description("Custom description for this marker.")
        .defaultValue("")
        .build()
    );

    private boolean active;

    public BaseMarker(String name) {
        this.mc = MinecraftClient.getInstance();
        this.name.set(name);
    }

    protected void render(Render3DEvent event) {}

    protected void tick() {}

    protected void onKeyPress(int key) {}
    protected void onKeyRelease(int key) {}

    public Screen getScreen(GuiTheme theme) {
        return new MarkerScreen(theme, this);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public String getName() {
        return name.get();
    }

    public String getTypeName() {
        return null;
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        active = !active;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        tag.putBoolean("active", active);
        return tag;
    }

    @Override
    public BaseMarker fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);
        active = tag.getBoolean("active");

        return this;
    }
}
