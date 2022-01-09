/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public class Waypoint implements ISerializable<Waypoint> {
    public String name = "Meteor on Crack!";
    public String icon = "Square";
    public SettingColor color = new SettingColor(225, 25, 25);

    public int x, y, z;

    public boolean visible = true;
    public int maxVisibleDistance = 1000;
    public double scale = 1;

    public boolean overworld, nether, end;

    public Dimension actualDimension;

    public void validateIcon() {
        Map<String, AbstractTexture> icons = Waypoints.get().icons;

        AbstractTexture texture = icons.get(icon);
        if (texture == null && !icons.isEmpty()) {
            icon = icons.keySet().iterator().next();
        }
    }

    public void renderIcon(double x, double y, double a, double size) {
        validateIcon();

        AbstractTexture texture = Waypoints.get().icons.get(icon);
        if (texture == null) return;

        int preA = color.a;
        color.a *= a;

        GL.bindTexture(texture.getGlId());
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, size, size, color);
        Renderer2D.TEXTURE.render(null);

        color.a = preA;
    }

    private int findIconIndex() {
        int i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (this.icon.equals(icon)) return i;
            i++;
        }

        return -1;
    }

    private int correctIconIndex(int i) {
        if (i < 0) return Waypoints.get().icons.size() + i;
        else if (i >= Waypoints.get().icons.size()) return i - Waypoints.get().icons.size();
        return i;
    }

    private String getIcon(int i) {
        i = correctIconIndex(i);

        int _i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (_i == i) return icon;
            _i++;
        }

        return "Square";
    }

    public void prevIcon() {
        icon = getIcon(findIconIndex() - 1);
    }

    public void nextIcon() {
        icon = getIcon(findIconIndex() + 1);
    }

    public Vec3 getCoords() {
        double x = this.x;
        double y = this.y;
        double z = this.z;

        if (actualDimension == Dimension.Overworld && PlayerUtils.getDimension() == Dimension.Nether) {
            x = x / 8f;
            z = z / 8f;
        }
        else if (actualDimension == Dimension.Nether && PlayerUtils.getDimension() == Dimension.Overworld) {
            x = x * 8;
            z = z * 8;
        }

        return new Vec3(x, y, z);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putString("icon", icon);
        tag.put("color", color.toTag());

        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);

        tag.putBoolean("visible", visible);
        tag.putInt("maxVisibleDistance", maxVisibleDistance);
        tag.putDouble("scale", scale);

        tag.putString("dimension", actualDimension.name());

        tag.putBoolean("overworld", overworld);
        tag.putBoolean("nether", nether);
        tag.putBoolean("end", end);

        return tag;
    }

    @Override
    public Waypoint fromTag(NbtCompound tag) {
        name = tag.getString("name");
        icon = tag.getString("icon");
        color.fromTag(tag.getCompound("color"));

        x = tag.getInt("x");
        y = tag.getInt("y");
        z = tag.getInt("z");

        visible = tag.getBoolean("visible");
        maxVisibleDistance = tag.getInt("maxVisibleDistance");
        scale = tag.getDouble("scale");

        actualDimension = Dimension.valueOf(tag.getString("dimension"));

        overworld = tag.getBoolean("overworld");
        nether = tag.getBoolean("nether");
        end = tag.getBoolean("end");

        if (!Waypoints.get().icons.containsKey(icon)) icon = "Square";

        return this;
    }
}
