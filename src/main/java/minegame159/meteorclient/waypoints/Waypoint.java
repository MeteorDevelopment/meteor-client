/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.waypoints;

import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.ISerializable;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.opengl.GL11;

public class Waypoint implements ISerializable<Waypoint> {
    private static final MeshBuilder MB = new MeshBuilder(100);

    public String name = "Meteor on Crack!";
    public String icon = "Square";
    public Color color = new Color(225, 25, 25);

    public int x, y, z;

    public boolean visible = true;
    public int maxVisibleDistance = 1000;
    public double scale = 1;

    public boolean overworld, nether, end;

    public void renderIcon(double x, double y, double z, double a, double size) {
        MB.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        int preA = color.a;
        color.a *= a;

        MB.pos(x, y, z).texture(0, 0).color(color).endVertex();
        MB.pos(x + size, y, z).texture(1, 0).color(color).endVertex();
        MB.pos(x + size, y + size, z).texture(1, 1).color(color).endVertex();

        MB.pos(x, y, z).texture(0, 0).color(color).endVertex();
        MB.pos(x + size, y + size, z).texture(1, 1).color(color).endVertex();
        MB.pos(x, y + size, z).texture(0, 1).color(color).endVertex();

        Waypoints.ICONS.get(icon).bindTexture();
        MB.end(true);

        color.a = preA;
    }

    public void renderIcon(double x, double y, double z) {
        renderIcon(x, y, z, 1, 16);
    }

    private int findIconIndex() {
        int i = 0;
        for (String icon : Waypoints.ICONS.keySet()) {
            if (this.icon.equals(icon)) return i;
            i++;
        }

        return -1;
    }

    private int correctIconIndex(int i) {
        if (i < 0) return Waypoints.ICONS.size() + i;
        else if (i >= Waypoints.ICONS.size()) return i - Waypoints.ICONS.size();
        return i;
    }

    private String getIcon(int i) {
        i = correctIconIndex(i);

        int _i = 0;
        for (String icon : Waypoints.ICONS.keySet()) {
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

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putString("icon", icon);
        tag.put("color", color.toTag());

        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);

        tag.putBoolean("visible", visible);
        tag.putInt("maxVisibleDistance", maxVisibleDistance);
        tag.putDouble("scale", scale);

        tag.putBoolean("overworld", overworld);
        tag.putBoolean("nether", nether);
        tag.putBoolean("end", end);

        return tag;
    }

    @Override
    public Waypoint fromTag(CompoundTag tag) {
        name = tag.getString("name");
        icon = tag.getString("icon");
        color.fromTag(tag.getCompound("color"));

        x = tag.getInt("x");
        y = tag.getInt("y");
        z = tag.getInt("z");

        visible = tag.getBoolean("visible");
        maxVisibleDistance = tag.getInt("maxVisibleDistance");
        scale = tag.getDouble("scale");

        overworld = tag.getBoolean("overworld");
        nether = tag.getBoolean("nether");
        end = tag.getBoolean("end");

        if (!Waypoints.ICONS.containsKey(icon)) icon = "Square";

        return this;
    }
}
