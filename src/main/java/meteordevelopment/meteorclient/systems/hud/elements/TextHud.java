/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.Section;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;

import java.util.List;

public class TextHud extends HudElement {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Hud.GROUP, "text", "Displays arbitrary text with Starscript.", TextHud::new);

    // Presets

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

    // Other

    private static final Color WHITE = new Color();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private double originalWidth, originalHeight;
    private boolean needsCompile, recalculateSize;

    private int timer;

    // General

    private final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
        .name("text")
        .description("Text to display with Starscript.")
        .defaultValue("Meteor Client")
        .onChanged(s -> {
            firstTick = true; 
            needsCompile = true;
        })
        .wide()
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder()
        .name("update-delay")
        .description("Update delay in ticks")
        .defaultValue(4)
        .onChanged(integer -> {
            if (timer > integer) timer = integer;
        })
        .min(0)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .onChanged(aBoolean -> recalculateSize = true)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the text.")
        .defaultValue(0)
        .onChanged(integer -> super.setSize(originalWidth + integer * 2, originalHeight + integer * 2))
        .build()
    );

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies custom text scale rather than the global one.")
        .defaultValue(false)
        .onChanged(integer -> recalculateSize = true)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .onChanged(integer -> recalculateSize = true)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private Script script;
    private Section section;

    private boolean firstTick = true;
    private boolean empty = false;

    private TextHud() {
        super(INFO);

        needsCompile = true;
    }

    @Override
    public void setSize(double width, double height) {
        this.originalWidth = width;
        this.originalHeight = height;
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    private void calculateSize(HudRenderer renderer) {
        double width = 0;

        if (section != null) {
            String str = section.toString();
            if (!str.isBlank()) width = renderer.textWidth(str, shadow.get(), getScale());
        }

        if (width != 0) {
            setSize(width, renderer.textHeight(shadow.get(), getScale()));
            empty = false;
        }
        else {
            setSize(100, renderer.textHeight(shadow.get(), getScale()));
            empty = true;
        }
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (recalculateSize) {
            calculateSize(renderer);
            recalculateSize = false;
        }

        if (timer <= 0) {
            runTick(renderer);
            timer = updateDelay.get();
        }
        else timer--;
    }

    private void runTick(HudRenderer renderer) {
        if (needsCompile) {
            Parser.Result result = Parser.parse(text.get());

            if (result.hasErrors()) {
                script = null;
                section = new Section(0, result.errors.get(0).toString());
                calculateSize(renderer);
            }
            else script = Compiler.compile(result);

            needsCompile = false;
        }

        try {
            if (script != null) {
                section = MeteorStarscript.ss.run(script);
                calculateSize(renderer);
            }
        }
        catch (StarscriptError error) {
            section = new Section(0, error.getMessage());
            calculateSize(renderer);
        }

        firstTick = false;
    }

    @Override
    public void render(HudRenderer renderer) {
        if (firstTick) runTick(renderer);

        if (empty && isInEditor()) {
            renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
            renderer.line(x, y + getHeight(), x + getWidth(), y, Color.GRAY);
        }

        if (section == null) return;

        double x = this.x + border.get();
        Section s = section;

        while (s != null) {
            x = renderer.text(s.text, x, y + border.get(), getSectionColor(s.index), shadow.get(), getScale());
            s = s.next;
        }

        if (background.get()) {
            renderer.quad(this.x, y, getWidth(), getHeight(), backgroundColor.get());
        }
    }

    @Override
    public void onFontChanged() {
        recalculateSize = true;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : -1;
    }

    public static Color getSectionColor(int i) {
        List<SettingColor> colors = Hud.get().textColors.get();
        return (i >= 0 && i < colors.size()) ? colors.get(i) : WHITE;
    }

    // Presets

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
