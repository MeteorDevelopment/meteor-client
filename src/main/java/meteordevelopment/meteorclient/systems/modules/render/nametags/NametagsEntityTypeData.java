/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.nametags;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.IScreenFactory;
import meteordevelopment.meteorclient.settings.EntityTypeDataSetting;
import meteordevelopment.meteorclient.settings.IEntityTypeData;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

public class NametagsEntityTypeData implements ICopyable<NametagsEntityTypeData>, ISerializable<NametagsEntityTypeData>, IChangeable, IEntityTypeData<NametagsEntityTypeData>, IScreenFactory {
    public double scale;
    public boolean throughWalls;
    public boolean culling;
    public double maxCullRange;
    public double maxCullCount;

    private boolean changed;

    public NametagsEntityTypeData(double scale, boolean throughWalls, boolean culling, double maxCullRange, double maxCullCount) {
        this.scale = scale;
        this.throughWalls = throughWalls;
        this.culling = culling;
        this.maxCullRange = maxCullRange;
        this.maxCullCount = maxCullCount;
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, EntityType entityType, EntityTypeDataSetting<NametagsEntityTypeData> setting) {
        return new NametagsEntityTypeDataScreen(theme, this, entityType, setting);
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme) {
        return new NametagsEntityTypeDataScreen(theme, this, null, null);
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    public void changed() {
        changed = true;
    }

    @Override
    public NametagsEntityTypeData set(NametagsEntityTypeData value) {
        scale = value.scale;
        throughWalls = value.throughWalls;
        culling = value.culling;
        maxCullRange = value.maxCullRange;
        maxCullCount = value.maxCullCount;

        changed = value.changed;

        return this;
    }

    @Override
    public NametagsEntityTypeData copy() {
        return new NametagsEntityTypeData(scale, throughWalls, culling, maxCullRange, maxCullCount);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putDouble("scale", scale);
        tag.putBoolean("onlyVisible", throughWalls);
        tag.putBoolean("culling", culling);
        tag.putDouble("maxCullRange", maxCullRange);
        tag.putDouble("maxCullCount", maxCullCount);

        tag.putBoolean("changed", changed);
        return tag;
    }

    @Override
    public NametagsEntityTypeData fromTag(NbtCompound tag) {
        scale = tag.getDouble("scale");
        throughWalls = tag.getBoolean("onlyVisible");
        culling = tag.getBoolean("culling");
        maxCullRange = tag.getDouble("maxCullRange");
        maxCullCount = tag.getDouble("maxCullCount");

        changed = tag.getBoolean("changed");
        return this;
    }

}
