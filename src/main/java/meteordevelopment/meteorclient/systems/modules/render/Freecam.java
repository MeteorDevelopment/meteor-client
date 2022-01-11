/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Your speed while in freecam.")
            .onChanged(aDouble -> speedValue = aDouble)
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    private final Setting<Double> speedScrollSensitivity = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed-scroll-sensitivity")
            .description("Allows you to change speed value using scroll wheel. 0 to disable.")
            .defaultValue(0)
            .min(0)
            .sliderMax(2)
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
            .description("Whether or not to render your hands in freecam.")
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
    private double speedValue;

    public float yaw, pitch;
    public float prevYaw, prevPitch;

    private boolean forward, backward, right, left, up, down;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        perspective = mc.options.getPerspective();
        speedValue = speed.get();

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
            velX += forward.x * s * speedValue;
            velZ += forward.z * s * speedValue;
            a = true;
        }
        if (this.backward) {
            velX -= forward.x * s * speedValue;
            velZ -= forward.z * s * speedValue;
            a = true;
        }

        boolean b = false;
        if (this.right) {
            velX += right.x * s * speedValue;
            velZ += right.z * s * speedValue;
            b = true;
        }
        if (this.left) {
            velX -= right.x * s * speedValue;
            velZ -= right.z * s * speedValue;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (this.up) {
            velY += s * speedValue;
        }
        if (this.down) {
            velY -= s * speedValue;
        }

        prevPos.set(pos);
        pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;

        // TODO: This is very bad but you all can cope :cope:
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if (mc.currentScreen != null && !guiMove.isActive()) return;
        if (mc.currentScreen != null && guiMove.isActive() && guiMove.skip()) return;

        boolean cancel = true;

        if (mc.options.keyForward.matchesKey(event.key, 0) || mc.options.keyForward.matchesMouse(event.key)) {
            forward = event.action != KeyAction.Release;
            mc.options.keyForward.setPressed(false);
        }
        else if (mc.options.keyBack.matchesKey(event.key, 0) || mc.options.keyBack.matchesMouse(event.key)) {
            backward = event.action != KeyAction.Release;
            mc.options.keyBack.setPressed(false);
        }
        else if (mc.options.keyRight.matchesKey(event.key, 0) || mc.options.keyRight.matchesMouse(event.key)) {
            right = event.action != KeyAction.Release;
            mc.options.keyRight.setPressed(false);
        }
        else if (mc.options.keyLeft.matchesKey(event.key, 0) || mc.options.keyLeft.matchesMouse(event.key)) {
            left = event.action != KeyAction.Release;
            mc.options.keyLeft.setPressed(false);
        }
        else if (mc.options.keyJump.matchesKey(event.key, 0) || mc.options.keyJump.matchesMouse(event.key)) {
            up = event.action != KeyAction.Release;
            mc.options.keyJump.setPressed(false);
        }
        else if (mc.options.keySneak.matchesKey(event.key, 0) || mc.options.keySneak.matchesMouse(event.key)) {
            down = event.action != KeyAction.Release;
            mc.options.keySneak.setPressed(false);
        }
        else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (speedScrollSensitivity.get() > 0) {
            speedValue += event.value * 0.25 * (speedScrollSensitivity.get() * speedValue);
            if (speedValue < 0.1) speedValue = 0.1;

            event.cancel();
        }
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onDamage(DamageEvent event) {
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
