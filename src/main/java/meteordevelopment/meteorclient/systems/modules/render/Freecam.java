/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Producer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgControl = settings.createGroup("Controls");

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

    private final Setting<Boolean> staySneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-sneaking")
        .description("If you are sneaking when you enter freecam, whether your player should remain sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preserveTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("preserve-crosshair-target")
        .description("Target the block the player is looking at instead of the block the camera is looking at")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-damage")
        .description("Disables freecam when you take damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-death")
        .description("Disables freecam when you die.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnLog = sgGeneral.add(new BoolSetting.Builder()
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

    private final Setting<Boolean> staticView = sgGeneral.add(new BoolSetting.Builder()
        .name("static")
        .description("Disables settings that move the view.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> relativePos = sgGeneral.add(new BoolSetting.Builder()
        .name("relative")
        .description("Camera moves along with the player")
        .defaultValue(false)
        .onChanged(this::onRelativeToggle)
        .build()
    );

    private final Setting<Keybind> relativeBind = sgControl.add(new KeybindSetting.Builder()
        .name("toggle-relative")
        .description("Press this bind to toggle the relative setting (bind will not trigger other actions)")
        .build()
    );

    private final Setting<Keybind> returnBind = sgControl.add(new KeybindSetting.Builder()
        .name("return")
        .description("Press this bind to return camera to player (bind will not trigger other actions)")
        .build()
    );

    private final Setting<Keybind> overrideBind = sgControl.add(new KeybindSetting.Builder()
        .name("switch-control")
        .description("Press this bind to switch control between player and camera (bind will not trigger other actions)")
        .build()
    );

    private final Setting<Boolean> overrideBindHold = sgControl.add(new BoolSetting.Builder()
        .name("switch-back-on-release")
        .description("Switch control back when bind is released")
        .defaultValue(true)
        .build()
    );

    // Will be relative to player if relativePos is set
    private final Vector3d pos = new Vector3d();
    private final Vector3d prevPos = new Vector3d();

    private Perspective perspective;
    private double speedValue;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double fovScale;
    private boolean bobView;

    private boolean forward, backward, right, left, up, down, isSneaking;

    private boolean override = false;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        fovScale = mc.options.getFovEffectScale().getValue();
        bobView = mc.options.getBobView().getValue();
        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue((double)0);
            mc.options.getBobView().setValue(false);
        }
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        perspective = mc.options.getPerspective();
        speedValue = speed.get();

        Utils.set(pos, mc.gameRenderer.getCamera().getPos());
        Utils.set(prevPos, mc.gameRenderer.getCamera().getPos());

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.sneakKey.isPressed();

        forward = Input.isPressed(mc.options.forwardKey);
        backward = Input.isPressed(mc.options.backKey);
        right = Input.isPressed(mc.options.rightKey);
        left = Input.isPressed(mc.options.leftKey);
        up = Input.isPressed(mc.options.jumpKey);
        down = Input.isPressed(mc.options.sneakKey);

        override = false;

        unpress();
        if (reloadChunks.get()) mc.worldRenderer.reload();
        resetCamera();
    }

    @Override
    public void onDeactivate() {
        if (reloadChunks.get()) {
            mc.execute(mc.worldRenderer::reload);
        }

        mc.options.setPerspective(perspective);

        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue(fovScale);
            mc.options.getBobView().setValue(bobView);
        }

        isSneaking = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        unpress();

        prevPos.set(pos);
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
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

        if (rotate.get() && shouldChangeCrosshairTarget()) {
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
        if (Input.isPressed(mc.options.sprintKey)) s = 1;

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
        if (relativePos.get()) {
            Vec3d delta = mc.player.getPos().subtract(mc.player.getLastRenderPos());
            prevPos.sub(delta.x, delta.y, delta.z);
        }
    }

    // Shadow other keybinds to not waste keyboard space when freecam is off
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKeyHigh(KeyEvent event) {

        if (mc.options.chatKey.matchesKey(event.key, 0)) return;
        if (KeyBinding.byId("key.meteor-client.open-gui").matchesKey(event.key, 0)) return;

        if (checkGuiMove()) return;
        if (handleOverrideBind()) event.cancel();
        if (override) return;
        if (handleReturnBind()) event.cancel();
        if (handleRelativeBind()) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMouseButtonHigh(MouseButtonEvent event) {

        if (mc.options.chatKey.matchesMouse(event.button)) return;
        if (KeyBinding.byId("key.meteor-client.open-gui").matchesMouse(event.button)) return;

        if (checkGuiMove()) return;
        if (handleOverrideBind()) event.cancel();
        if (override) return;
        if (handleReturnBind()) event.cancel();
        if (handleRelativeBind()) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKey(KeyEvent event) {
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;
        if (checkGuiMove()) return;
        if (override) return;

        boolean cancel = true;

        if (mc.options.forwardKey.matchesKey(event.key, 0)) {
            forward = event.action != KeyAction.Release;
            mc.options.forwardKey.setPressed(false);
        }
        else if (mc.options.backKey.matchesKey(event.key, 0)) {
            backward = event.action != KeyAction.Release;
            mc.options.backKey.setPressed(false);
        }
        else if (mc.options.rightKey.matchesKey(event.key, 0)) {
            right = event.action != KeyAction.Release;
            mc.options.rightKey.setPressed(false);
        }
        else if (mc.options.leftKey.matchesKey(event.key, 0)) {
            left = event.action != KeyAction.Release;
            mc.options.leftKey.setPressed(false);
        }
        else if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            up = event.action != KeyAction.Release;
            mc.options.jumpKey.setPressed(false);
        }
        else if (mc.options.sneakKey.matchesKey(event.key, 0)) {
            down = event.action != KeyAction.Release;
            mc.options.sneakKey.setPressed(false);
        }
        else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (checkGuiMove()) return;
        if (override) return;

        boolean cancel = true;

        if (mc.options.forwardKey.matchesMouse(event.button)) {
            forward = event.action != KeyAction.Release;
            mc.options.forwardKey.setPressed(false);
        }
        else if (mc.options.backKey.matchesMouse(event.button)) {
            backward = event.action != KeyAction.Release;
            mc.options.backKey.setPressed(false);
        }
        else if (mc.options.rightKey.matchesMouse(event.button)) {
            right = event.action != KeyAction.Release;
            mc.options.rightKey.setPressed(false);
        }
        else if (mc.options.leftKey.matchesMouse(event.button)) {
            left = event.action != KeyAction.Release;
            mc.options.leftKey.setPressed(false);
        }
        else if (mc.options.jumpKey.matchesMouse(event.button)) {
            up = event.action != KeyAction.Release;
            mc.options.jumpKey.setPressed(false);
        }
        else if (mc.options.sneakKey.matchesMouse(event.button)) {
            down = event.action != KeyAction.Release;
            mc.options.sneakKey.setPressed(false);
        }
        else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (override) return;

        if (speedScrollSensitivity.get() > 0 && mc.currentScreen == null) {
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
    private void onGameLeft(GameLeftEvent event) {
        if (!toggleOnLog.get()) return;

        toggle();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        }
        else if (event.packet instanceof HealthUpdateS2CPacket packet) {
            if (mc.player.getHealth() - packet.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                info("Toggled off because you took damage.");
            }
        }
    }

    private void onRelativeToggle(boolean relative) {
        if (mc.cameraEntity == null) return;
        if (relative) {
            pos.sub(mc.cameraEntity.getX(), mc.cameraEntity.getY(), mc.cameraEntity.getZ());
            prevPos.sub(mc.cameraEntity.getX(), mc.cameraEntity.getY(), mc.cameraEntity.getZ());
        } else {
            pos.add(mc.cameraEntity.getX(), mc.cameraEntity.getY(), mc.cameraEntity.getZ());
            prevPos.add(mc.cameraEntity.getX(), mc.cameraEntity.getY(), mc.cameraEntity.getZ());
        }
    }

    private boolean checkGuiMove() {
        // TODO: This is very bad but you all can cope :cope:
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if (mc.currentScreen != null && !guiMove.isActive()) return true;
        return (mc.currentScreen != null && guiMove.isActive() && guiMove.skip());
    }

    private void resetCamera() {
        if (mc.cameraEntity == null) return;
        if (relativePos.get()) pos.set(0, mc.cameraEntity.getEyeHeight(mc.cameraEntity.getPose()), 0);
        else pos.set(mc.cameraEntity.getX(), mc.cameraEntity.getEyeY(), mc.cameraEntity.getZ());
        pitch = mc.cameraEntity.getPitch();
        yaw = mc.cameraEntity.getYaw();

        prevPos.set(pos);
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private boolean overrideBindIsHeld = false;
    private boolean handleOverrideBind() {
        if (overrideBindIsHeld == overrideBind.get().isPressed()) return false;
        overrideBindIsHeld = overrideBind.get().isPressed();
        if (overrideBindIsHeld || (overrideBindHold.get() && !overrideBindIsHeld)) {
            up = down = left = right = forward = backward = false;
            override ^= true;
        }
        return true;
    }

    private boolean returnBindIsHeld = false;
    private boolean handleReturnBind() {
        boolean changed = returnBindIsHeld != returnBind.get().isPressed();
        returnBindIsHeld = returnBind.get().isPressed();
        if (changed && returnBindIsHeld) resetCamera();
        return changed;
    }

    private boolean relativeBindIsHeld = false;
    private boolean handleRelativeBind() {
        boolean changed = relativeBindIsHeld != relativeBind.get().isPressed();
        relativeBindIsHeld = relativeBind.get().isPressed();
        if (changed && relativeBindIsHeld) relativePos.set(!relativePos.get());
        return changed;
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }

    public boolean staySneaking() {
        return isActive() && !mc.player.getAbilities().flying && staySneaking.get() && isSneaking;
    }

    public double getX(float tickDelta) {
        double x =  MathHelper.lerp(tickDelta, prevPos.x, pos.x);
        if (relativePos.get()) x += mc.cameraEntity.getX();
        return x;
    }
    public double getY(float tickDelta) {
        double y =  MathHelper.lerp(tickDelta, prevPos.y, pos.y);
        if (relativePos.get()) y += mc.cameraEntity.getY();
        return y;
    }
    public double getZ(float tickDelta) {
        double z = MathHelper.lerp(tickDelta, prevPos.z, pos.z);
        if (relativePos.get()) z += mc.cameraEntity.getZ();
        return z;
    }
    public Vec3d getPos(float tickDelta) {
        return new Vec3d(getX(tickDelta), getY(tickDelta), getZ(tickDelta));
    }
    public Vec3d getPos() {
        return new Vec3d(getX(1), getY(1), getZ(1));
    }

    static public <R> R withPos(Producer<R> c) {
        Freecam f = Modules.get().get(Freecam.class);

        Entity cameraE = MeteorClient.mc.getCameraEntity();

        if (!f.shouldChangeCrosshairTarget()) return c.create();

        double x = cameraE.getX();
        double y = cameraE.getY();
        double z = cameraE.getZ();
        double lastX = cameraE.lastX;
        double lastY = cameraE.lastY;
        double lastZ = cameraE.lastZ;
        float yaw = cameraE.getYaw();
        float pitch = cameraE.getPitch();
        float lastYaw = cameraE.lastYaw;
        float lastPitch = cameraE.lastPitch;

        cameraE.lastX = f.getX(1);
        cameraE.lastY = f.getY(1) - cameraE.getEyeHeight(cameraE.getPose());
        cameraE.lastZ = f.getZ(1);
        cameraE.setYaw(yaw);
        cameraE.setPitch(pitch);
        cameraE.lastYaw = yaw;
        cameraE.lastPitch = pitch;

        R r = c.create();

        ((IVec3d) cameraE.getPos()).meteor$set(x, y, z);
        cameraE.lastX = lastX;
        cameraE.lastY = lastY;
        cameraE.lastZ = lastZ;
        cameraE.setYaw(yaw);
        cameraE.setPitch(pitch);
        cameraE.lastYaw = lastYaw;
        cameraE.lastPitch = lastPitch;

        return r;
    }

    static public void withPos(Runnable c) {
        Freecam f = Modules.get().get(Freecam.class);

        Entity cameraE = MeteorClient.mc.getCameraEntity();

        if (!f.shouldChangeCrosshairTarget()) {
            c.run();
            return;
        }

        double x = cameraE.getX();
        double y = cameraE.getY();
        double z = cameraE.getZ();
        double lastX = cameraE.lastX;
        double lastY = cameraE.lastY;
        double lastZ = cameraE.lastZ;
        float yaw = cameraE.getYaw();
        float pitch = cameraE.getPitch();
        float lastYaw = cameraE.lastYaw;
        float lastPitch = cameraE.lastPitch;

        cameraE.lastX = f.getX(1);
        cameraE.lastY = f.getY(1) - cameraE.getEyeHeight(cameraE.getPose());
        cameraE.lastZ = f.getZ(1);
        cameraE.setYaw(yaw);
        cameraE.setPitch(pitch);
        cameraE.lastYaw = yaw;
        cameraE.lastPitch = pitch;

        c.run();

        ((IVec3d) cameraE.getPos()).meteor$set(x, y, z);
        cameraE.lastX = lastX;
        cameraE.lastY = lastY;
        cameraE.lastZ = lastZ;
        cameraE.setYaw(yaw);
        cameraE.setPitch(pitch);
        cameraE.lastYaw = lastYaw;
        cameraE.lastPitch = lastPitch;
    }


    public double getYaw(float tickDelta) {
        if (override || !mc.isWindowFocused()) return yaw;
        return MathHelper.lerp(tickDelta, lastYaw, yaw);
    }
    public double getPitch(float tickDelta) {
        if (override || !mc.isWindowFocused()) return pitch;
        return MathHelper.lerp(tickDelta, lastPitch, pitch);
    }

    public boolean getOverride() {
        return override;
    }

    public boolean shouldChangeCrosshairTarget() {
        return isActive() && !override && !preserveTarget.get();
    }
}
