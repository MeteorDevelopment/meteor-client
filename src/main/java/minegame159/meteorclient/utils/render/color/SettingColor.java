/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render.color;

import net.minecraft.nbt.CompoundTag;

public class SettingColor extends Color {
    private static final float[] hsb = new float[3];

    public double rainbowSpeed;

    public SettingColor() {
        super();
    }

    public SettingColor(int r, int g, int b) {
        super(r, g, b);
    }

    public SettingColor(int r, int g, int b, double rainbowSpeed) {
        this(r, g, b, 255, rainbowSpeed);
    }

    public SettingColor(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    public SettingColor(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public SettingColor(int r, int g, int b, int a, double rainbowSpeed) {
        super(r, g, b, a);

        this.rainbowSpeed = rainbowSpeed;
    }

    public SettingColor(SettingColor color) {
        super(color);

        this.rainbowSpeed = color.rainbowSpeed;
    }

    public void update() {
        if (rainbowSpeed > 0) {
            java.awt.Color.RGBtoHSB(r, g, b, hsb);
            int c = java.awt.Color.HSBtoRGB(hsb[0] + (float) rainbowSpeed, 1, 1);

            r = Color.toRGBAR(c);
            g = Color.toRGBAG(c);
            b = Color.toRGBAB(c);
        }
    }

    @Override
    public void set(Color value) {
        super.set(value);

        if (value instanceof SettingColor) {
            rainbowSpeed = ((SettingColor) value).rainbowSpeed;
        }
    }

    @Override
    public Color copy() {
        return new SettingColor(r, g, b, a, rainbowSpeed);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        tag.putDouble("rainbowSpeed", rainbowSpeed);

        return tag;
    }

    @Override
    public SettingColor fromTag(CompoundTag tag) {
        super.fromTag(tag);

        rainbowSpeed = tag.getDouble("rainbowSpeed");

        return this;
    }
}
