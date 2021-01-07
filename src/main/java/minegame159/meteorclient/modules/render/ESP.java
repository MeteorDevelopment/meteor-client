/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class ESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> outline = sgGeneral.add(new BoolSetting.Builder()
            .name("outline")
            .description("Renders an outline around the entities.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> box = sgGeneral.add(new BoolSetting.Builder()
            .name("box")
            .description("Renders a box around the entities.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("box-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    public final Setting<Boolean> showInvis = sgGeneral.add(new BoolSetting.Builder()
            .name("show-invisible")
            .description("Shows invisibile entities.")
            .defaultValue(true)
            .build()
    );

    // Colors

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
            .name("players-color")
            .description("The other player's color.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("The animal's color.")
            .defaultValue(new SettingColor(25, 255, 25, 255))
            .build()
    );

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
            .name("water-animals-color")
            .description("The water animal's color.")
            .defaultValue(new SettingColor(25, 25, 255, 255))
            .build()
    );

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("The monster's color.")
            .defaultValue(new SettingColor(255, 25, 25, 255))
            .build()
    );

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
            .name("ambient-color")
            .description("The ambient's color.")
            .defaultValue(new SettingColor(25, 25, 25, 255))
            .build()
    );

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("The misc color.")
            .defaultValue(new SettingColor(175, 175, 175, 255))
            .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("fade-distance")
            .description("The distance where the color fades.")
            .defaultValue(6)
            .min(0)
            .sliderMax(12)
            .build()
    );

    private final Color sideColor = new Color();
    private final Color outlineColor = new Color();
    private int count;

    public ESP() {
        super(Category.Render, "ESP", "Renders entities through walls.");
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

            Box box = entity.getBoundingBox();
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor, lineColor, shapeMode.get(), 0);
        }

        lineColor.a = prevLineA;
        sideColor.a = prevSideA;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (!box.get()) return;

        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if ((!ModuleManager.INSTANCE.isActive(Freecam.class) && entity == mc.player) || !entities.get().contains(entity.getType())) continue;
            count++;
            render(event, entity, getColor(entity));
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
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

    public Color getColor(Entity entity) {
        if (entity instanceof PlayerEntity) return FriendManager.INSTANCE.getColor((PlayerEntity) entity, playersColor.get(), false);

        switch (entity.getType().getSpawnGroup()) {
            case CREATURE:       return animalsColor.get();
            case WATER_CREATURE: return waterAnimalsColor.get();
            case MONSTER:        return monstersColor.get();
            case AMBIENT:        return ambientColor.get();
            case MISC:           return miscColor.get();
        }

        return Utils.WHITE;
    }

    public boolean isOutline() {
        return outline.get();
    }
}
