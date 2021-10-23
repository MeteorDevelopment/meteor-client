/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayDeque;
import java.util.Queue;

public class Breadcrumbs extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the Breadcrumbs trail.")
        .defaultValue(new SettingColor(225, 25, 25))
        .build()
    );

    private final Setting<Integer> maxSections = sgGeneral.add(new IntSetting.Builder()
        .name("max-sections")
        .description("The maximum number of sections.")
        .defaultValue(1000)
        .min(1)
        .sliderRange(1, 5000)
        .build()
    );

    private final Setting<Double> sectionLength = sgGeneral.add(new DoubleSetting.Builder()
        .name("section-length")
        .description("The section length in blocks.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Pool<Section> sectionPool = new Pool<>(Section::new);
    private final Queue<Section> sections = new ArrayDeque<>();

    private Section section;

    private DimensionType lastDimension;

    public Breadcrumbs() {
        super(Categories.Render, "breadcrumbs", "Displays a trail behind where you have walked.");
    }

    @Override
    public void onActivate() {
        section = sectionPool.get();
        section.set1();

        lastDimension = mc.world.getDimension();
    }

    @Override
    public void onDeactivate() {
        for (Section section : sections) sectionPool.free(section);
        sections.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (lastDimension != mc.world.getDimension()) {
            for (Section sec : sections) sectionPool.free(sec);
            sections.clear();
        }

        if (isFarEnough(section.x1, section.y1, section.z1)) {
            section.set2();

            if (sections.size() >= maxSections.get()) {
                Section section = sections.poll();
                if (section != null) sectionPool.free(section);
            }

            sections.add(section);
            section = sectionPool.get();
            section.set1();
        }

        lastDimension = mc.world.getDimension();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        int iLast = -1;

        for (Section section : sections) {
            if (iLast == -1) {
                iLast = event.renderer.lines.vec3(section.x1, section.y1, section.z1).color(color.get()).next();
            }

            int i = event.renderer.lines.vec3(section.x2, section.y2, section.z2).color(color.get()).next();
            event.renderer.lines.line(iLast, i);
            iLast = i;
        }
    }

    private boolean isFarEnough(double x, double y, double z) {
        return Math.abs(mc.player.getX() - x) >= sectionLength.get() || Math.abs(mc.player.getY() - y) >= sectionLength.get() || Math.abs(mc.player.getZ() - z) >= sectionLength.get();
    }

    private class Section {
        public float x1, y1, z1;
        public float x2, y2, z2;

        public void set1() {
            x1 = (float) mc.player.getX();
            y1 = (float) mc.player.getY();
            z1 = (float) mc.player.getZ();
        }

        public void set2() {
            x2 = (float) mc.player.getX();
            y2 = (float) mc.player.getY();
            z2 = (float) mc.player.getZ();
        }

        public void render(Render3DEvent event) {
            event.renderer.line(x1, y1, z1, x2, y2, z2, color.get());
        }
    }
}
