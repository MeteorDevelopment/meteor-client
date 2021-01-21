/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import org.apache.commons.lang3.StringUtils;

// Credits to snail for helping, Finished in 3 am ez. This was in total 20 hours of work. subscribe to pewdipie. Meteor on top ez
//Gilded moment ez

public class RotationHud extends DoubleTextHudModule {
    public RotationHud(HUD hud) {
        super(hud, "rotation", "Displays your rotation.", "invalid ");
    }

    @Override
    protected String getRight() {
        Direction playerDirection;
        float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
        float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

        if ((yaw >= 0 && yaw < 45) || (yaw <= 0 && yaw > -45)) playerDirection = Direction.South;
        else if ((yaw >= 45 && yaw < 90) || (yaw <= -45 && yaw > -90)) playerDirection = Direction.SouthWest;
        else if ((yaw >= 90 && yaw < 135) || (yaw <= -90 && yaw > -135)) playerDirection = Direction.West;
        else if ((yaw >= 135 && yaw < 180) || (yaw <= -135 && yaw > -180)) playerDirection = Direction.NorthWest;
        else if ((yaw >= 180 && yaw < 225) || (yaw <= -180 && yaw > -225)) playerDirection = Direction.North;
        else if ((yaw >= 225 && yaw < 270) || (yaw <= -225 && yaw > -270)) playerDirection = Direction.NorthEast;
        else if ((yaw >= 270 && yaw < 315) || (yaw <= -270 && yaw > -315)) playerDirection = Direction.East;
        else if ((yaw >= 315 && yaw < 360) || (yaw <= -315 && yaw > -360)) playerDirection = Direction.SouthEast;
        else playerDirection = Direction.Null;

        if (yaw < 0) yaw += 360;
        if (yaw > 180) yaw -= 360;

        if (pitch < 0) pitch += 360;
        if (pitch > 180) pitch -= 360;

        setLeft(String.format("%s %s ", StringUtils.capitalize(playerDirection.name()), playerDirection.getAxis()));
        return String.format("(%.1f, %.1f)", yaw, pitch);
    }

    private enum Direction {
        South("Z+"),
        SouthEast("X+ Z+"),
        West("X-"),
        NorthWest("X- Z-"),
        North("Z-"),
        NorthEast("X+ Z-"),
        East("X+"),
        SouthWest("Z+ X-"),
        Null("NaN");

        String axis;

        Direction(String axis) {
            this.axis = axis;
        }

        public String getAxis() {
            return axis;
        }
    }
}
