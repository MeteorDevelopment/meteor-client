/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;

public class MeteorTextHud {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Hud.GROUP, "text", "Displays arbitrary text with Starscript.", MeteorTextHud::create);

    public static final HudElementInfo<TextHud>.Preset FPS;
    public static final HudElementInfo<TextHud>.Preset TPS;
    public static final HudElementInfo<TextHud>.Preset PING;
    public static final HudElementInfo<TextHud>.Preset SPEED;
    public static final HudElementInfo<TextHud>.Preset DURABILITY;
    public static final HudElementInfo<TextHud>.Preset POSITION;
    public static final HudElementInfo<TextHud>.Preset OPPOSITE_POSITION;
    public static final HudElementInfo<TextHud>.Preset LOOKING_AT;
    public static final HudElementInfo<TextHud>.Preset LOOKING_AT_WITH_POSITION;
    public static final HudElementInfo<TextHud>.Preset BREAKING_PROGRESS;
    public static final HudElementInfo<TextHud>.Preset SERVER;
    public static final HudElementInfo<TextHud>.Preset BIOME;
    public static final HudElementInfo<TextHud>.Preset WORLD_TIME;
    public static final HudElementInfo<TextHud>.Preset REAL_TIME;
    public static final HudElementInfo<TextHud>.Preset ROTATION;
    public static final HudElementInfo<TextHud>.Preset MODULE_ENABLED;
    public static final HudElementInfo<TextHud>.Preset MODULE_ENABLED_WITH_INFO;
    public static final HudElementInfo<TextHud>.Preset WATERMARK;
    public static final HudElementInfo<TextHud>.Preset BARITONE;

    static {
        addPreset("Empty", null);
        FPS = addPreset("FPS", "FPS: #1{fps}", 0);
        TPS = addPreset("TPS", "TPS: #1{round(server.tps, 1)}");
        PING = addPreset("Ping", "Ping: #1{ping}");
        SPEED = addPreset("Speed", "Speed: #1{round(player.speed, 1)}", 0);
        DURABILITY = addPreset("Durability", "Durability: #1{player.hand_or_offhand.durability}");
        POSITION = addPreset("Position", "Pos: #1{floor(camera.pos.x)}, {floor(camera.pos.y)}, {floor(camera.pos.z)}", 0);
        OPPOSITE_POSITION = addPreset("Opposite Position", "{player.opposite_dimension != \"End\" ? player.opposite_dimension + \":\" : \"\"} #1{player.opposite_dimension != \"End\" ? \"\" + floor(camera.opposite_dim_pos.x) + \", \" + floor(camera.opposite_dim_pos.y) + \", \" + floor(camera.opposite_dim_pos.z) : \"\"}", 0);
        LOOKING_AT = addPreset("Looking at", "Looking at: #1{crosshair_target.value}", 0);
        LOOKING_AT_WITH_POSITION = addPreset("Looking at  with position", "Looking at: #1{crosshair_target.value} {crosshair_target.type != \"miss\" ? \"(\" + \"\" + floor(crosshair_target.value.pos.x) + \", \" + floor(crosshair_target.value.pos.y) + \", \" + floor(crosshair_target.value.pos.z) + \")\" : \"\"}", 0);
        BREAKING_PROGRESS = addPreset("Breaking progress", "Breaking progress: #1{round(player.breaking_progress * 100)}%", 0);
        SERVER = addPreset("Server", "Server: #1{server}");
        BIOME = addPreset("Biome", "Biome: #1{player.biome}", 0);
        WORLD_TIME = addPreset("World time", "Time: #1{server.time}");
        REAL_TIME = addPreset("Real time", "Time: #1{time}");
        ROTATION = addPreset("Rotation", "{camera.direction} #1({round(camera.yaw, 1)}, {round(camera.pitch, 1)})", 0);
        MODULE_ENABLED = addPreset("Module enabled", "Kill Aura: {meteor.is_module_active(\"kill-aura\") ? #2 \"ON\" : #3 \"OFF\"}", 0);
        MODULE_ENABLED_WITH_INFO = addPreset("Module enabled with info", "Kill Aura: {meteor.is_module_active(\"kill-aura\") ? #2 \"ON\" : #3 \"OFF\"} #1{meteor.get_module_info(\"kill-aura\")}", 0);
        WATERMARK = addPreset("Watermark", "Meteor Client #1{version}", Integer.MAX_VALUE);
        BARITONE = addPreset("Baritone", "Baritone: #1{baritone.process_name}");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static HudElementInfo<TextHud>.Preset addPreset(String title, String text, int updateDelay) {
        return INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
            if (updateDelay != -1) textHud.updateDelay.set(updateDelay);
        });
    }

    private static HudElementInfo<TextHud>.Preset addPreset(String title, String text) {
        return addPreset(title, text, -1);
    }
}
