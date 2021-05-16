/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.TookDamageEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.ChunkOcclusionEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.client.options.Perspective;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Your speed while in freecam.")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    private final Setting<Boolean> autoDisableOnDamage = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-damage")
            .description("Disables freecam when you take damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoDisableOnDeath = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-death")
            .description("Disables freecam when you die.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoDisableOnLog = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-log")
            .description("Disables freecam when you disconnect from a server.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
            .name("reload-chunks")
            .description("Disables cave culling.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
            .name("show-hands")
            .description("Whether or not to render your hands in greecam.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates to the block or entity you are looking at.")
            .defaultValue(false)
            .build()
    );

    public final Vec3 pos = new Vec3();
    public final Vec3 prevPos = new Vec3();

    private Perspective perspective;

    public float yaw, pitch;
    public float prevYaw, prevPitch;

    private boolean forward, backward, right, left, up, down;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        yaw = mc.player.yaw;
        pitch = mc.player.pitch;

        perspective = mc.options.getPerspective();

        pos.set(mc.gameRenderer.getCamera().getPos());
        prevPos.set(mc.gameRenderer.getCamera().getPos());

        prevYaw = yaw;
        prevPitch = pitch;

        forward = false;
        backward = false;
        right = false;
        left = false;
        up = false;
        down = false;

        unpress();
        if (reloadChunks.get()) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (reloadChunks.get()) mc.worldRenderer.reload();
        mc.options.setPerspective(perspective);
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        unpress();

        prevPos.set(pos);
        prevYaw = yaw;
        prevPitch = pitch;
    }

    private void unpress() {
        mc.options.keyForward.setPressed(false);
        mc.options.keyBack.setPressed(false);
        mc.options.keyRight.setPressed(false);
        mc.options.keyLeft.setPressed(false);
        mc.options.keyJump.setPressed(false);
        mc.options.keySneak.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.cameraEntity.isInsideWall()) mc.getCameraEntity().noClip = true;
        if (!perspective.isFirstPerson()) mc.options.setPerspective(Perspective.FIRST_PERSON);

        if (mc.currentScreen != null) return;

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velY = 0;
        double velZ = 0;


        if (rotate.get()) {
            BlockPos crossHairPos;
            Vec3d crossHairPosition;

            if (mc.crosshairTarget instanceof EntityHitResult) {
                crossHairPos = ((EntityHitResult) mc.crosshairTarget).getEntity().getBlockPos();
                Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
            } else {
                crossHairPosition = mc.crosshairTarget.getPos();
                crossHairPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

                if (!mc.world.getBlockState(crossHairPos).isAir()) {
                    Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
                }
            }
        }
        
        double s = 0.5;
        if (mc.options.keySprint.isPressed()) s = 1;

        boolean a = false;
        if (this.forward) {
            velX += forward.x * s * speed.get();
            velZ += forward.z * s * speed.get();
            a = true;
        }
        if (this.backward) {
            velX -= forward.x * s * speed.get();
            velZ -= forward.z * s * speed.get();
            a = true;
        }

        boolean b = false;
        if (this.right) {
            velX += right.x * s * speed.get();
            velZ += right.z * s * speed.get();
            b = true;
        }
        if (this.left) {
            velX -= right.x * s * speed.get();
            velZ -= right.z * s * speed.get();
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (this.up) {
            velY += s * speed.get();
        }
        if (this.down) {
            velY -= s * speed.get();
        }

        prevPos.set(pos);
        pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        boolean cancel = true;

        if (mc.options.keyForward.matchesKey(event.key, 0)) {
            forward = event.action != KeyAction.Release;
        } else if (mc.options.keyBack.matchesKey(event.key, 0)) {
            backward = event.action != KeyAction.Release;
        } else if (mc.options.keyRight.matchesKey(event.key, 0)) {
            right = event.action != KeyAction.Release;
        } else if (mc.options.keyLeft.matchesKey(event.key, 0)) {
            left = event.action != KeyAction.Release;
        } else if (mc.options.keyJump.matchesKey(event.key, 0)) {
            up = event.action != KeyAction.Release;
        } else if (mc.options.keySneak.matchesKey(event.key, 0)) {
            down = event.action != KeyAction.Release;
        } else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onTookDamage(TookDamageEvent event) {
        if (event.entity.getUuid() == null) return;
        if (!event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (autoDisableOnDamage.get() || (autoDisableOnDeath.get() && event.entity.getHealth() <= 0)) {
            toggle();
            info("Auto toggled because you took damage or died.");
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (!autoDisableOnLog.get()) return;

        toggle();
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        prevYaw = yaw;
        prevPitch = pitch;

        yaw += deltaX;
        pitch += deltaY;

        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }

    public double getX(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.x, pos.x);
    }
    public double getY(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.y, pos.y);
    }
    public double getZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.z, pos.z);
    }

    public double getYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevYaw, yaw);
    }
    public double getPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPitch, pitch);
    }
}
