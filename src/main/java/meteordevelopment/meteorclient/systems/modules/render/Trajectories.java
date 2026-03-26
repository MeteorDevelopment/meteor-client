/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.simulator.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.entity.simulator.SimulationStep;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3d;

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

    private final Setting<Boolean> otherPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("other-players")
        .description("Calculates trajectories for other players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> firedProjectiles = sgGeneral.add(new BoolSetting.Builder()
        .name("fired-projectiles")
        .description("Calculates trajectories for already fired projectiles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreWitherSkulls = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-wither-skulls")
        .description("Whether to ignore fired wither skulls.")
        .defaultValue(false)
        .visible(firedProjectiles::get)
        .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Whether or not to calculate more accurate.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> simulationSteps = sgGeneral.add(new IntSetting.Builder()
        .name("simulation-steps")
        .description("How many steps to simulate projectiles. Zero for no limit")
        .defaultValue(500)
        .sliderMax(5000)
        .build()
    );

    // Render

    private final Setting<Integer> ignoreFirstTicks = sgRender.add(new IntSetting.Builder()
        .name("ignore-rendering-first-ticks")
        .description("Ignores rendering the first given ticks, to make the rest of the path more visible.")
        .defaultValue(3)
        .min(0)
        .build()
    );

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

    private final Setting<Boolean> renderPositionBox = sgRender.add(new BoolSetting.Builder()
        .name("render-position-boxes")
        .description("Renders the actual position the projectile will be at each tick along it's trajectory.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> positionBoxSize = sgRender.add(new DoubleSetting.Builder()
    	.name("position-box-size")
    	.description("The size of the box drawn at the simulated positions.")
    	.defaultValue(0.02)
        .sliderRange(0.01, 0.1)
        .visible(renderPositionBox::get)
    	.build()
    );

    private final Setting<SettingColor> positionSideColor = sgRender.add(new ColorSetting.Builder()
        .name("position-side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 150, 0, 35))
        .visible(renderPositionBox::get)
        .build()
    );

    private final Setting<SettingColor> positionLineColor = sgRender.add(new ColorSetting.Builder()
        .name("position-line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 150, 0))
        .visible(renderPositionBox::get)
        .build()
    );

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();

    private final Pool<Vector3d> vec3s = new Pool<>(Vector3d::new);
    private final List<Path> paths = new ArrayList<>();

    private static final double MULTISHOT_OFFSET = Math.toRadians(10); // accurate-ish offset of crossbow multishot in radians (10° degrees)

    public Trajectories() {
        super(Categories.Render, "trajectories", "Predicts the trajectory of throwable items.");
    }

    private boolean itemFilter(Item item) {
        return item instanceof ProjectileWeaponItem || item instanceof FishingRodItem || item instanceof TridentItem ||
            item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem ||
            item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem || item instanceof WindChargeItem;
    }

    private List<Item> getDefaultItems() {
        List<Item> items = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
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

    private void calculatePath(Player player, float tickDelta) {
        // Clear paths
        for (Path path : paths) path.clear();

        // Get item
        ItemStack itemStack = player.getMainHandItem();
        if (!items.get().contains(itemStack.getItem())) {
            itemStack = player.getOffhandItem();
            if (!items.get().contains(itemStack.getItem())) return;
        }

        // Calculate paths
        if (!simulator.set(player, itemStack, 0, accurate.get(), tickDelta)) return;
        Path p = getEmptyPath().calculate();
        if (player == mc.player) p.ignoreFirstTicks();

        if (itemStack.getItem() instanceof CrossbowItem && Utils.hasEnchantment(itemStack, Enchantments.MULTISHOT)) {
            if (!simulator.set(player, itemStack, MULTISHOT_OFFSET, accurate.get(), tickDelta)) return; // left multishot arrow
            p = getEmptyPath().calculate();
            if (player == mc.player) p.ignoreFirstTicks();

            if (!simulator.set(player, itemStack, -MULTISHOT_OFFSET, accurate.get(), tickDelta)) return; // right multishot arrow
            p = getEmptyPath().calculate();
            if (player == mc.player) p.ignoreFirstTicks();
        }
    }

    private void calculateFiredPath(Entity entity, double tickDelta) {
        for (Path path : paths) path.clear();

        // Calculate paths
        if (!simulator.set(entity)) return;
        getEmptyPath().setStart(entity, tickDelta).calculate();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        float tickDelta = mc.level.tickRateManager().isFrozen() ? 1 : event.tickDelta;

        for (Player player : mc.level.players()) {
            if (!otherPlayers.get() && player != mc.player) continue;

            calculatePath(player, tickDelta);
            for (Path path : paths) path.render(event);
        }

        if (firedProjectiles.get()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Projectile) {
                    if (ignoreWitherSkulls.get() && entity instanceof WitherSkull) continue;
                    if (entity instanceof ThrownTrident trident && trident.noPhysics) continue; // when it's returning via loyalty

                    calculateFiredPath(entity, tickDelta);
                    for (Path path : paths) path.render(event);
                }
            }
        }
    }

    private class Path {
        private final List<Vector3d> points = new ArrayList<>();

        private boolean hitQuad, hitQuadHorizontal;
        private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

        private final List<Entity> collidingEntities = new ArrayList<>();
        public Vector3d lastPoint;
        private int start;

        public void clear() {
            vec3s.freeAll(points);
            points.clear();

            hitQuad = false;
            collidingEntities.clear();
            lastPoint = null;
            start = 0;
        }

        public Path calculate() {
            addPoint();

            for (int i = 0; i < (simulationSteps.get() > 0 ? simulationSteps.get() : Integer.MAX_VALUE); i++) {
                SimulationStep result = simulator.tick();

                processHitResults(result);
                if (result.shouldStop) break;

                addPoint();
            }

            return this;
        }

        public Path setStart(Entity entity, double tickDelta) {
            lastPoint = new Vector3d(
                Mth.lerp(tickDelta, entity.xOld, entity.getX()),
                Mth.lerp(tickDelta, entity.yOld, entity.getY()),
                Mth.lerp(tickDelta, entity.zOld, entity.getZ())
            );

            return this;
        }

        private void addPoint() {
            points.add(vec3s.get().set(simulator.pos));
        }

        private void processHitResults(SimulationStep step) {
            for (int i = 0; i < step.hitResults.length; i++) {
                HitResult result = step.hitResults[i];
                if (result.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult r = (BlockHitResult) result;

                    hitQuad = true;
                    hitQuadX1 = r.getLocation().x;
                    hitQuadY1 = r.getLocation().y;
                    hitQuadZ1 = r.getLocation().z;
                    hitQuadX2 = r.getLocation().x;
                    hitQuadY2 = r.getLocation().y;
                    hitQuadZ2 = r.getLocation().z;

                    if (r.getDirection() == Direction.UP || r.getDirection() == Direction.DOWN) {
                        hitQuadHorizontal = true;
                        hitQuadX1 -= 0.25;
                        hitQuadZ1 -= 0.25;
                        hitQuadX2 += 0.25;
                        hitQuadZ2 += 0.25;
                    }
                    else if (r.getDirection() == Direction.NORTH || r.getDirection() == Direction.SOUTH) {
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

                    points.add(Utils.set(vec3s.get(), result.getLocation()));
                }
                else if (result.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) result).getEntity();
                    collidingEntities.add(entity);

                    if (step.shouldStop && i == step.hitResults.length - 1) {
                        points.add(Utils.set(vec3s.get(), result.getLocation()));
                    }
                }
            }
        }

        public void ignoreFirstTicks() {
            start = points.size() <= ignoreFirstTicks.get() ? 0 : ignoreFirstTicks.get();
        }

        public void render(Render3DEvent event) {
            // Render path
            for (int i = start; i < points.size(); i++) {
                Vector3d point = points.get(i);

                if (lastPoint != null) {
                    event.renderer.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, lineColor.get());
                    if (renderPositionBox.get()) {
                        event.renderer.box(
                            point.x - positionBoxSize.get(), point.y - positionBoxSize.get(), point.z - positionBoxSize.get(),
                            point.x + positionBoxSize.get(), point.y + positionBoxSize.get(), point.z + positionBoxSize.get(),
                            positionSideColor.get(), positionLineColor.get(), shapeMode.get(), 0
                        );
                    }
                }

                lastPoint = point;
            }

            // Render hit quad
            if (hitQuad) {
                if (hitQuadHorizontal) event.renderer.sideHorizontal(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX1 + 0.5, hitQuadZ1 + 0.5, sideColor.get(), lineColor.get(), shapeMode.get());
                else event.renderer.sideVertical(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, sideColor.get(), lineColor.get(), shapeMode.get());
            }

            // Render entity
            for (Entity collidingEntity : collidingEntities) {
                double x = (collidingEntity.getX() - collidingEntity.xo) * event.tickDelta;
                double y = (collidingEntity.getY() - collidingEntity.yo) * event.tickDelta;
                double z = (collidingEntity.getZ() - collidingEntity.zo) * event.tickDelta;

                AABB box = collidingEntity.getBoundingBox();
                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
