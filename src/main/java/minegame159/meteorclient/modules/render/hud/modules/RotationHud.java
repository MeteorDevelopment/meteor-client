/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.StringUtils;

public class RotationHud extends DoubleTextHudModule {
    public RotationHud(HUD hud) {
        super(hud, "rotation", "Displays your rotation.", "invalid ");
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
        Omniscient("All"),

        Null("NaN");

        String alternate;

        Direction(String alternate) {
            this.alternate = alternate;
        }

        public String getAlternate() {
            return alternate;
        }
    }
// Credits to snail for helping, Finished in 3 am ez. This was in total 20 hours of work. subscribe to pewdipie. Meteor on top ez
    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();

        float determiner = mc.gameRenderer.getCamera().getYaw() % 360;


        Direction playerDirection = Direction.Null;

        if (determiner >= 0 && determiner < 45) playerDirection = Direction.South;
        if (determiner >= 45 && determiner < 90) playerDirection = Direction.SouthWest;
        if (determiner >= 90 && determiner < 135) playerDirection = Direction.West;
        if (determiner >= 135 && determiner < 180) playerDirection = Direction.NorthWest;
        if (determiner >= 180 && determiner < 225) playerDirection = Direction.North;
        if (determiner >= 225 && determiner < 270) playerDirection = Direction.NorthEast;
        if (determiner >= 270 && determiner < 315) playerDirection = Direction.East;
        if (determiner >= 315 && determiner < 360) playerDirection = Direction.SouthEast;

         if (determiner <= 0 && determiner > -45) playerDirection = Direction.South;
         if (determiner <= -45 && determiner > -90) playerDirection = Direction.SouthEast;
         if (determiner <= -90 && determiner > -135) playerDirection = Direction.East;
         if (determiner <= -135 && determiner > -180) playerDirection = Direction.NorthEast;
         if (determiner <= -180 && determiner > -225) playerDirection = Direction.North;
         if (determiner <= -225 && determiner > -270) playerDirection = Direction.NorthWest;
         if (determiner <= -270 && determiner > -315) playerDirection = Direction.West;
         if (determiner <= -315 && determiner > -360) playerDirection = Direction.SouthWest;

        float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw > 180) yaw -= 360;

        float pitch = mc.gameRenderer.getCamera().getPitch() % 360;
        if (pitch < 0) pitch += 360;
        if (pitch > 180) pitch -= 360;

        setLeft(String.format("%s %s ", StringUtils.capitalize(playerDirection.name()), playerDirection.getAlternate()));
        return String.format("(%.1f, %.1f)", yaw, pitch);
    }
}
