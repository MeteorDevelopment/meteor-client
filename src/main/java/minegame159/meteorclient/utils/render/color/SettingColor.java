/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render.color;

import net.minecraft.nbt.CompoundTag;

public class SettingColor extends Color {

    public boolean rainbow;

    public SettingColor() {
        super();
    }

    public SettingColor(int r, int g, int b) {
        super(r, g, b);
    }

    public SettingColor(int r, int g, int b, boolean rainbow) {
        this(r, g, b, 255, rainbow);
    }

    public SettingColor(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    public SettingColor(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public SettingColor(int r, int g, int b, int a, boolean rainbow) {
        super(r, g, b, a);
        this.rainbow = rainbow;
    }

    public SettingColor(SettingColor color) {
        super(color);
        this.rainbow = color.rainbow;
    }

    public SettingColor rainbow(boolean rainbow) {
        this.rainbow = rainbow;
        return this;
    }

    public void update() {
        if (rainbow) set(RainbowColors.GLOBAL.r, RainbowColors.GLOBAL.g, RainbowColors.GLOBAL.b, a);
    }

    @Override
    public SettingColor set(Color value) {
        super.set(value);
        if (value instanceof SettingColor) rainbow = ((SettingColor) value).rainbow;

        return this;
    }

    @Override
    public Color copy() {
        return new SettingColor(r, g, b, a, rainbow);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.putBoolean("rainbow", rainbow);
        return tag;
    }

    @Override
    public SettingColor fromTag(CompoundTag tag) {
        super.fromTag(tag);
        rainbow = tag.getBoolean("rainbow");
        return this;
    }
}
