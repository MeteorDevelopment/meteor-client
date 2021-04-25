/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.entity.ProjectileEntitySimulator;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to display trajectories for.")
            .defaultValue(getDefaultItems())
            .filter(this::itemFilter)
            .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
            .name("accurate")
            .description("Calculates accurate trajectories while moving.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 150, 0, 35))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 150, 0))
            .build()
    );

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();

    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Path> paths = new ArrayList<>();

    public Trajectories() {
        super(Categories.Render, "trajectories", "Predicts the trajectory of throwable items.");
    }

    private boolean itemFilter(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof FishingRodItem || item instanceof TridentItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
    }

    private List<Item> getDefaultItems() {
        List<Item> items = new ArrayList<>();

        for (Item item : Registry.ITEM) {
            if (itemFilter(item)) items.add(item);
        }

        return items;
    }

    private Path getEmptyPath() {
        for (Path path : paths) {
            if (path.points.isEmpty()) return path;
        }

        Path path = new Path();
        paths.add(path);
        return path;
    }

    private void calculatePath(RenderEvent event) {
        // Clear paths
        for (Path path : paths) path.clear();

        // Get item
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack == null) itemStack = mc.player.getOffHandStack();
        if (itemStack == null) return;
        if (!items.get().contains(itemStack.getItem())) return;

        // Calculate paths
        if (!simulator.set(mc.player, itemStack, 0, accurate.get(), event.tickDelta)) return;
        getEmptyPath().calculate();

        if (itemStack.getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(Enchantments.MULTISHOT, itemStack) > 0) {
            if (!simulator.set(mc.player, itemStack, -10, accurate.get(), event.tickDelta)) return;
            getEmptyPath().calculate();

            if (!simulator.set(mc.player, itemStack, 10, accurate.get(), event.tickDelta)) return;
            getEmptyPath().calculate();
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        calculatePath(event);

        for (Path path : paths) path.render(event);
    }

    private class Path {
        private final List<Vec3> points = new ArrayList<>();

        private boolean hitQuad, hitQuadHorizontal;
        private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

        private Entity entity;

        public void clear() {
            for (Vec3 point : points) vec3s.free(point);
            points.clear();

            hitQuad = false;
            entity = null;
        }

        public void calculate() {
            addPoint();

            while (true) {
                HitResult result = simulator.tick();

                if (result != null) {
                    processHitResult(result);
                    break;
                }

                addPoint();
            }
        }

        private void addPoint() {
            points.add(vec3s.get().set(simulator.pos));
        }

        private void processHitResult(HitResult result) {
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult r = (BlockHitResult) result;

                hitQuad = true;
                hitQuadX1 = r.getPos().x;
                hitQuadY1 = r.getPos().y;
                hitQuadZ1 = r.getPos().z;
                hitQuadX2 = r.getPos().x;
                hitQuadY2 = r.getPos().y;
                hitQuadZ2 = r.getPos().z;

                if (r.getSide() == Direction.UP || r.getSide() == Direction.DOWN) {
                    hitQuadHorizontal = true;
                    hitQuadX1 -= 0.25;
                    hitQuadZ1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadZ2 += 0.25;
                }
                else if (r.getSide() == Direction.NORTH || r.getSide() == Direction.SOUTH) {
                    hitQuadHorizontal = false;
                    hitQuadX1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadY2 += 0.25;
                }
                else {
                    hitQuadHorizontal = false;
                    hitQuadZ1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadZ2 += 0.25;
                    hitQuadY2 += 0.25;
                }

                points.add(vec3s.get().set(result.getPos()));
            }
            else if (result.getType() == HitResult.Type.ENTITY) {
                entity = ((EntityHitResult) result).getEntity();

                points.add(vec3s.get().set(result.getPos()).add(0, entity.getHeight() / 2, 0));
            }
        }

        public void render(RenderEvent event) {
            // Render path
            Vec3 lastPoint = null;

            for (Vec3 point : points) {
                if (lastPoint != null) Renderer.LINES.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, lineColor.get());
                lastPoint = point;
            }

            // Render hit quad
            if (hitQuad) {
                if (hitQuadHorizontal) Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, hitQuadX1, hitQuadY1, hitQuadZ1, 0.5, sideColor.get(), lineColor.get(), shapeMode.get());
                else Renderer.quadWithLinesVertical(Renderer.NORMAL, Renderer.LINES, hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, sideColor.get(), lineColor.get(), shapeMode.get());
            }

            // Render entity
            if (entity != null) {
                double x = (entity.getX() - entity.prevX) * event.tickDelta;
                double y = (entity.getY() - entity.prevY) * event.tickDelta;
                double z = (entity.getZ() - entity.prevZ) * event.tickDelta;

                Box box = entity.getBoundingBox();
                Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
