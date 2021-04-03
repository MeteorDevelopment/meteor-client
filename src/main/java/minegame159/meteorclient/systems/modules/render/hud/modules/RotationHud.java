/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.systems.modules.render.hud.HUD;

// Credits to snail for helping, Finished in 3 am ez. This was in total 20 hours of work. subscribe to pewdipie. Meteor on top ez
// Gilded moment ez
// Gilded did monky code so I fixed :coool: - MineGame159

public class RotationHud extends DoubleTextHudElement {
    public RotationHud(HUD hud) {
        super(hud, "rotation", "Displays your rotation.", "");
    }

    @Override
    protected String getRight() {
        Direction dir;
        float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
        float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

        if (yaw < 0) yaw += 360;
        if (pitch < 0) pitch += 360;

        if (yaw >= 337.5 || yaw < 22.5) dir = Direction.South;
        else if (yaw >= 22.5 && yaw < 67.5) dir = Direction.SouthWest;
        else if (yaw >= 67.5 && yaw < 112.5) dir = Direction.West;
        else if (yaw >= 112.5 && yaw < 157.5) dir = Direction.NorthWest;
        else if (yaw >= 157.5 && yaw < 202.5) dir = Direction.North;
        else if (yaw >= 202.5 && yaw < 247.5) dir = Direction.NorthEast;
        else if (yaw >= 247.5 && yaw < 292.5) dir = Direction.East;
        else if (yaw >= 292.5 && yaw < 337.5) dir = Direction.SouthEast;
        else dir = Direction.NaN;

        if (yaw > 180) yaw -= 360;
        if (pitch > 180) pitch -= 360;

        setLeft(String.format("%s %s ", dir.name, dir.axis));
        return String.format("(%.1f, %.1f)", yaw, pitch);
    }

    private enum Direction {
        South("South", "Z+"),
        SouthEast("South East", "Z+ X+"),
        West("West", "X-"),
        NorthWest("North West", "Z- X-"),
        North("North", "Z-"),
        NorthEast("North East", "Z- X+"),
        East("East", "X+"),
        SouthWest("South West", "Z+ X-"),
        NaN("NaN", "NaN");

        public String name;
        public String axis;

        Direction(String name, String axis) {
            this.axis = axis;
            this.name = name;
        }
    }
}
