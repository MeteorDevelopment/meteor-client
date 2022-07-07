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
    private static final Color WHITE = new Color();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private double originalWidth, originalHeight;
    private boolean needsCompile, recalculateSize;

    private int timer;

    // General

    public final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
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

    public final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder()
        .name("update-delay")
        .description("Update delay in ticks")
        .defaultValue(4)
        .onChanged(integer -> {
            if (timer > integer) timer = integer;
        })
        .min(0)
        .build()
    );

    public final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .onChanged(aBoolean -> recalculateSize = true)
        .build()
    );

    public final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the text.")
        .defaultValue(0)
        .onChanged(integer -> super.setSize(originalWidth + integer * 2, originalHeight + integer * 2))
        .build()
    );

    // Scale

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies custom text scale rather than the global one.")
        .defaultValue(false)
        .onChanged(integer -> recalculateSize = true)
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
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

    public final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
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

    public TextHud(HudElementInfo<TextHud> info) {
        super(info);

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
}
