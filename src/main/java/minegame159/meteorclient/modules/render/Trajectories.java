/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 150, 0, 35))
            .build()
    );
    
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 150, 0))
            .build()
    );

    private final Vec3d vec3d = new Vec3d(0, 0, 0);

    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Vec3> path = new ArrayList<>();

    private boolean hitQuad, hitQuadHorizontal;
    private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

    public Trajectories() {
        super(Categories.Render, "trajectories", "Predicts the trajectory of throwable items.");
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        Item item = mc.player.getMainHandStack().getItem();
        if (!Utils.isThrowable(item)) {
            item = mc.player.getOffHandStack().getItem();
            if (!Utils.isThrowable(item)) return;
        }

        calculatePath(event.tickDelta, item);

        Vec3 lastPoint = null;
        for (Vec3 point : path) {
            if (lastPoint != null) Renderer.LINES.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, lineColor.get());
            lastPoint = point;
        }

        if (hitQuad) {
            if (hitQuadHorizontal) Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, hitQuadX1, hitQuadY1, hitQuadZ1, 0.5, sideColor.get(), lineColor.get(), shapeMode.get());
            else Renderer.quadWithLinesVertical(Renderer.NORMAL, Renderer.LINES, hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, sideColor.get(), lineColor.get(), shapeMode.get());
        }
    }

    private void calculatePath(float tickDelta, Item item) {
        // Clear path and target
        for (Vec3 point : path) vec3s.free(point);
        path.clear();

        // Calculate starting position
        double x = mc.player.lastRenderX + (mc.player.getX() - mc.player.lastRenderX) * tickDelta - Math.cos(Math.toRadians(mc.player.yaw)) * 0.16;
        double y = mc.player.lastRenderY + (mc.player.getY() - mc.player.lastRenderY) * tickDelta + mc.player.getStandingEyeHeight() - 0.1;
        double z = mc.player.lastRenderZ + (mc.player.getZ() - mc.player.lastRenderZ) * tickDelta - Math.sin(Math.toRadians(mc.player.yaw)) * 0.16;

        // Motion factor. Arrows go faster than snowballs and all that
        double velocityFactor = item instanceof RangedWeaponItem ? 1.0 : 0.4;

        double yaw = Math.toRadians(mc.player.yaw);
        double pitch = Math.toRadians(mc.player.pitch);

        // Calculate starting motion
        double velocityX = -Math.sin(yaw) * Math.cos(pitch) * velocityFactor;
        double velocityY = -Math.sin(pitch) * velocityFactor;
        double velocityZ = Math.cos(yaw) * Math.cos(pitch) * velocityFactor;

        // 3D Pythagorean theorem. Returns the length of the arrowMotion vector.
        double velocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);

        velocityX /= velocity;
        velocityY /= velocity;
        velocityZ /= velocity;

        // Apply bow charge
        if(item instanceof RangedWeaponItem) {
            float bowPower = (72000 - mc.player.getItemUseTimeLeft()) / 20.0f;
            bowPower = (bowPower * bowPower + bowPower * 2.0f) / 3.0f;

            if(bowPower > 1 || bowPower <= 0.1F) bowPower = 1;

            bowPower *= 3F;
            velocityX *= bowPower;
            velocityY *= bowPower;
            velocityZ *= bowPower;

        } else {
            velocityX *= 1.5;
            velocityY *= 1.5;
            velocityZ *= 1.5;
        }

        double gravity = getProjectileGravity(item);
        Vec3d eyesPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

        HitResult lastHitResult = null;

        while (true) {
            // Add to path
            Vec3 pos = addToPath(x, y, z);

            // Apply motion
            x += velocityX * 0.1;
            y += velocityY * 0.1;
            z += velocityZ * 0.1;

            if (y < 0) break;

            // Apply air friction
            velocityX *= 0.999;
            velocityY *= 0.999;
            velocityZ *= 0.999;

            // Apply gravity
            velocityY -= gravity * 0.1;

            // Check if chunk is loaded
            int chunkX = (int) (x / 16);
            int chunkZ = (int) (z / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) break;

            // Check for collision
            ((IVec3d) vec3d).set(pos);
            RaycastContext context = new RaycastContext(eyesPos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            lastHitResult = mc.world.raycast(context);
            if (lastHitResult.getType() != HitResult.Type.MISS) break;
        }

        if (lastHitResult != null && lastHitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult r = (BlockHitResult) lastHitResult;

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
            } else if (r.getSide() == Direction.NORTH || r.getSide() == Direction.SOUTH) {
                hitQuadHorizontal = false;
                hitQuadX1 -= 0.25;
                hitQuadY1 -= 0.25;
                hitQuadX2 += 0.25;
                hitQuadY2 += 0.25;
            } else {
                hitQuadHorizontal = false;
                hitQuadZ1 -= 0.25;
                hitQuadY1 -= 0.25;
                hitQuadZ2 += 0.25;
                hitQuadY2 += 0.25;
            }
        } else hitQuad = false;
    }

    private double getProjectileGravity(Item item) {
        if(item instanceof BowItem || item instanceof CrossbowItem) return 0.05;
        if(item instanceof PotionItem) return 0.4;
        if(item instanceof FishingRodItem) return 0.15;
        if(item instanceof TridentItem) return 0.015;

        return 0.03;
    }

    private Vec3 addToPath(double x, double y, double z) {
        Vec3 point = vec3s.get();
        point.set(x, y, z);
        path.add(point);

        return point;
    }
}
