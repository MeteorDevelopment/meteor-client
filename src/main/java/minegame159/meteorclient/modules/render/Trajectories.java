/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Color> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(255, 150, 0))
            .build()
    );

    private final Pool<Vec3d> vec3ds = new Pool<>(() -> new Vec3d(0, 0, 0));
    private final List<Vec3d> path = new ArrayList<>();

    private boolean hitQuad, hitQuadHorizontal;
    private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;
    private final Color hitQuadColor = new Color();

    public Trajectories() {
        super(Category.Render, "trajectories", "Displays trajectory of held items.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (!Utils.isThrowable(mc.player.getMainHandStack().getItem())) return;

        calculatePath(event.tickDelta);

        Vec3d lastPoint = null;
        for (Vec3d point : path) {
            if (lastPoint != null) ShapeBuilder.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, color.get());
            lastPoint = point;
        }

        if (hitQuad) {
            hitQuadColor.set(color.get());
            hitQuadColor.a = 35;

            if (hitQuadHorizontal) ShapeBuilder.quadWithLines(hitQuadX1, hitQuadY1, hitQuadZ1, 0.5, 0.5, hitQuadColor, color.get());
            else ShapeBuilder.quadWithLinesVertical(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, hitQuadColor, color.get());
        }
    });

    private void calculatePath(float tickDelta) {
        // Clear path and target
        for (Vec3d point : path) vec3ds.free(point);
        path.clear();

        Item item = mc.player.getMainHandStack().getItem();

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
            Vec3d pos = addToPath(x, y, z);

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
            RaycastContext context = new RaycastContext(eyesPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
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

    private Vec3d addToPath(double x, double y, double z) {
        Vec3d point = vec3ds.get();
        ((IVec3d) point).set(x, y, z);
        path.add(point);

        return point;
    }
}
