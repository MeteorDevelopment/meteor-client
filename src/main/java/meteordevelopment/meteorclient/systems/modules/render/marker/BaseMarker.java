/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.MarkerScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

import java.util.Locale;

public abstract class BaseMarker implements ISerializable<BaseMarker> {
    public final Settings settings;

    protected final SettingGroup sgBase;

    public final Setting<String> name;
    protected final Setting<String> description;
    private final Setting<Dimension> dimension;
    private final Setting<Boolean> active;

    public BaseMarker(String type) {
        this.settings = new Settings("marker." + type.toLowerCase(Locale.ROOT));

        this.sgBase = settings.createGroup("base");

        this.name = sgBase.add(new StringSetting.Builder()
            .name("name")
            .build()
        );
        this.name.set(type);

        this.description = sgBase.add(new StringSetting.Builder()
            .name("description")
            .build()
        );

        this.dimension = sgBase.add(new EnumSetting.Builder<Dimension>()
            .name("dimension")
            .defaultValue(Dimension.Overworld)
            .build()
        );
        this.dimension.set(PlayerUtils.getDimension());

        this.active = sgBase.add(new BoolSetting.Builder()
            .name("active")
            .defaultValue(false)
            .build()
        );
    }

    protected void render(Render3DEvent event) {}

    protected void tick() {}

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
        return active.get();
    }

    public boolean isVisible() {
        return isActive() && PlayerUtils.getDimension() == dimension.get();
    }

    public Dimension getDimension() {
        return dimension.get();
    }

    public void toggle() {
        active.set(!active.get());
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public BaseMarker fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }
}
