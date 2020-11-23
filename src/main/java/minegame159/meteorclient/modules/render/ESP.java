/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class ESP extends ToggleModule {
    private static final Color WHITE = new Color(255, 255, 255);

    public enum Mode {
        Lines,
        Sides,
        Both,
        Outline
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Outline)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    // Colors

    private final Setting<Color> playersColor = sgColors.add(new ColorSetting.Builder()
            .name("players-color")
            .description("Players color.")
            .defaultValue(new Color(255, 255, 255))
            .build()
    );

    private final Setting<Color> animalsColor = sgColors.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(25, 255, 25, 255))
            .build()
    );

    private final Setting<Color> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
            .name("water-animals-color")
            .description("Water animals color.")
            .defaultValue(new Color(25, 25, 255, 255))
            .build()
    );

    private final Setting<Color> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("Monsters color.")
            .defaultValue(new Color(255, 25, 25, 255))
            .build()
    );

    private final Setting<Color> ambientColor = sgColors.add(new ColorSetting.Builder()
            .name("ambient-color")
            .description("Ambient color.")
            .defaultValue(new Color(25, 25, 25, 255))
            .build()
    );

    private final Setting<Color> miscColor = sgColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("Misc color.")
            .defaultValue(new Color(175, 175, 175, 255))
            .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("fade-distance")
            .description("At which distance the color will fade out.")
            .defaultValue(6)
            .min(0)
            .sliderMax(12)
            .build()
    );

    private final Color sideColor = new Color();
    private final Color outlineColor = new Color();
    private int count;

    public ESP() {
        super(Category.Render, "esp", "See entities through walls.");
    }

    private void setSideColor(Color lineColor) {
        sideColor.set(lineColor);
        sideColor.a = 25;
    }

    private void render(RenderEvent event, Entity entity, Color lineColor) {
        setSideColor(lineColor);

        double dist = mc.cameraEntity.squaredDistanceTo(entity.getX() + entity.getWidth() / 2, entity.getY() + entity.getHeight() / 2, entity.getZ() + entity.getWidth() / 2);
        double a = 1;
        if (dist <= fadeDistance.get() * fadeDistance.get()) a = dist / (fadeDistance.get() * fadeDistance.get());

        int prevLineA = lineColor.a;
        int prevSideA = sideColor.a;

        lineColor.a *= a;
        sideColor.a *= a;

        if (a >= 0.075) {
            double x = (entity.getX() - entity.prevX) * event.tickDelta;
            double y = (entity.getY() - entity.prevY) * event.tickDelta;
            double z = (entity.getZ() - entity.prevZ) * event.tickDelta;

            switch (mode.get()) {
                case Lines: {
                    Box box = entity.getBoundingBox();
                    ShapeBuilder.boxEdges(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, lineColor);
                    break;
                }
                case Sides: {
                    Box box = entity.getBoundingBox();
                    ShapeBuilder.boxSides(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor);
                    break;
                }
                case Both: {
                    Box box = entity.getBoundingBox();
                    ShapeBuilder.boxEdges(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, lineColor);
                    ShapeBuilder.boxSides(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor);
                    break;
                }
            }
        }

        lineColor.a = prevLineA;
        sideColor.a = prevSideA;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if ((!ModuleManager.INSTANCE.isActive(Freecam.class) && entity == mc.player) || !entities.get().contains(entity.getType())) continue;
            count++;

            if (mode.get() == Mode.Outline) {
                continue;
            }

            render(event, entity, getColor(entity));
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public Color getColor(Entity entity) {
        if (entity instanceof PlayerEntity) return FriendManager.INSTANCE.getColor((PlayerEntity) entity, playersColor.get());

        switch (entity.getType().getSpawnGroup()) {
            case CREATURE:       return animalsColor.get();
            case WATER_CREATURE: return waterAnimalsColor.get();
            case MONSTER:        return monstersColor.get();
            case AMBIENT:        return ambientColor.get();
            case MISC:           return miscColor.get();
        }

        return WHITE;
    }

    public Color getOutlineColor(Entity entity) {
        if (!entities.get().contains(entity.getType())) return null;
        Color color = getColor(entity);

        double dist = mc.cameraEntity.squaredDistanceTo(entity.getX() + entity.getWidth() / 2, entity.getY() + entity.getHeight() / 2, entity.getZ() + entity.getWidth() / 2);
        double a = 1;
        if (dist <= fadeDistance.get() * fadeDistance.get()) a = dist / (fadeDistance.get() * fadeDistance.get());

        if (a >= 0.075) {
            outlineColor.set(color);
            outlineColor.a *= a;
            return outlineColor;
        }

        return null;
    }

    public boolean isOutline() {
        return mode.get() == Mode.Outline;
    }
}
