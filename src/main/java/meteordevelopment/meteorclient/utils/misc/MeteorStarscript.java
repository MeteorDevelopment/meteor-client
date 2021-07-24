/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.starscript.StandardLib;
import meteordevelopment.starscript.Starscript;
import meteordevelopment.starscript.value.Value;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.SharedConstants;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class MeteorStarscript {
    public static Starscript ss = new Starscript();

    public static void init() {
        StandardLib.init(ss);

        // General
        ss.set("version", Value.string(Config.get().version != null ? (Config.get().devBuild.isEmpty() ? Config.get().version.toString() : Config.get().version + " " + Config.get().devBuild) : ""));
        ss.set("mc_version", Value.string(SharedConstants.getGameVersion().getName()));
        ss.set("fps", () -> Value.number(((MinecraftClientAccessor) mc).getFps()));

        // Player
        ss.set("player", Value.map(new ValueMap()
            .set("_toString", () -> Value.string(mc.getSession().getUsername()))
            .set("health", () -> Value.number(mc.player != null ? mc.player.getHealth() : 0))
            .set("hunger", () -> Value.number(mc.player != null ? mc.player.getHungerManager().getFoodLevel() : 0))
            .set("speed", () -> Value.number(Utils.getPlayerSpeed()))
            .set("pos", Value.map(new ValueMap()
                .set("x", () -> Value.number(mc.player != null ? mc.player.getX() : 0))
                .set("y", () -> Value.number(mc.player != null ? mc.player.getY() : 0))
                .set("z", () -> Value.number(mc.player != null ? mc.player.getZ() : 0))
                .set("_toString", MeteorStarscript::playerPosString)
            ))
            .set("yaw", () -> Value.number(mc.player != null ? mc.player.getYaw() : 0))
            .set("pitch", () -> Value.number(mc.player != null ? mc.player.getPitch() : 0))
        ));

        // Server
        ss.set("server", Value.map(new ValueMap()
            .set("_toString", () -> Value.string(Utils.getWorldName()))
            .set("tps", () -> Value.number(TickRate.INSTANCE.getTickRate()))
            .set("time", () -> Value.string(Utils.getWorldTime()))
        ));
    }

    private static Value playerPosString() {
        if (mc.player == null) return Value.string("X: 0 Y: 0 Z: 0");
        return Value.string(String.format("X: %.0f Y: %.0f Z: %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }
}
