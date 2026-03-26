/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;


import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPathing = settings.createGroup("Pathing");

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

    private final Setting<Boolean> baritoneClick = sgPathing.add(new BoolSetting.Builder()
        .name("click-to-path")
        .description("Sets a pathfinding goal to any block/entity you click at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> requireDoubleClick = sgPathing.add(new BoolSetting.Builder()
        .name("double-click")
        .description("Require two clicks to start pathing.")
        .defaultValue(false)
        .build()
    );

    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();

    private CameraType perspective;
    private double speedValue;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double fovScale;
    private boolean bobView;

    private boolean forward, backward, right, left, up, down, isSneaking;

    private long clickTs = 0;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        fovScale = mc.options.fovEffectScale().get();
        bobView = mc.options.bobView().get();
        if (staticView.get()) {
            mc.options.fovEffectScale().set((double)0);
            mc.options.bobView().set(false);
        }
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();

        perspective = mc.options.getCameraType();
        speedValue = speed.get();

        Utils.set(pos, mc.gameRenderer.getMainCamera().position());
        Utils.set(prevPos, mc.gameRenderer.getMainCamera().position());

        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.keyShift.isDown();

        forward = Input.isPressed(mc.options.keyUp);
        backward = Input.isPressed(mc.options.keyDown);
        right = Input.isPressed(mc.options.keyRight);
        left = Input.isPressed(mc.options.keyLeft);
        up = Input.isPressed(mc.options.keyJump);
        down = Input.isPressed(mc.options.keyShift);

        unpress();
        if (reloadChunks.get()) mc.levelRenderer.allChanged();
    }

    @Override
    public void onDeactivate() {
        if (reloadChunks.get()) {
            mc.execute(mc.levelRenderer::allChanged);
        }

        mc.options.setCameraType(perspective);

        if (staticView.get()) {
            mc.options.fovEffectScale().set(fovScale);
            mc.options.bobView().set(bobView);
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
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getCameraEntity().isInWall()) mc.getCameraEntity().noPhysics = true;
        if (!perspective.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);

        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        double velX = 0;
        double velY = 0;
        double velZ = 0;

        if (rotate.get()) {
            BlockPos crossHairPos;
            Vec3 crossHairPosition;

            if (mc.hitResult instanceof EntityHitResult) {
                crossHairPos = ((EntityHitResult) mc.hitResult).getEntity().blockPosition();
                Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
            } else {
                crossHairPosition = mc.hitResult.getLocation();
                crossHairPos = ((BlockHitResult) mc.hitResult).getBlockPos();

                if (!mc.level.getBlockState(crossHairPos).isAir()) {
                    Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
                }
            }
        }

        double s = 0.5;
        if (Input.isPressed(mc.options.keySprint)) s = 1;

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onKey(KeyEvent event) {
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;
        if (checkGuiMove()) return;

        if (onInput(event.key(), event.action)) event.cancel();
    }

    @Nullable
    private BlockPos rayCastEntity(Vec3 posVec, Vec3 max, short maxDist) {
        EntityHitResult res = ProjectileUtil.getEntityHitResult(
            mc.player,
            posVec,
            max,
            AABB.encapsulatingFullBlocks(BlockPos.containing(posVec.x, posVec.y, posVec.z), BlockPos.containing(max.x, max.y, max.z)),
            (entity) -> true,
            maxDist
        );

        if (res == null) return null;

        Vec3 vec = res.getLocation();

        return BlockPos.containing(vec.x, vec.y, vec.z);
    }

    @Nullable
    private BlockPos rayCastBlock(Vec3 posVec, Vec3 max) {
        ClipContext ctx = new ClipContext(
            posVec,
            max,
            ClipContext.Block.VISUAL,
            ClipContext.Fluid.SOURCE_ONLY,
            CollisionContext.empty()
        );

        BlockHitResult res = mc.level.clip(ctx);
        if (res.getType() == HitResult.Type.MISS) return null;

        // Don't move inside block
        return res.getBlockPos().offset(res.getDirection().getUnitVec3i());
    }

    private void setGoal() {
        long prevClick = clickTs;
        clickTs = System.currentTimeMillis();

        if (requireDoubleClick.get() && clickTs - prevClick > 500) return;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 posVec = cam.position();
        Vec3 lookVec = Vec3.directionFromRotation(cam.xRot(), cam.yRot());
        short maxDist = 256;
        Vec3 max = posVec.add(lookVec.scale(maxDist));

        BlockPos pos = rayCastEntity(posVec, max, maxDist);
        if (pos == null) {
            pos = rayCastBlock(posVec, max);
        }

        if (pos == null) return;

        PathManagers.get().moveTo(pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        if (checkGuiMove()) return;

        if (baritoneClick.get() && event.action == KeyAction.Press && mc.options.keyAttack.matchesMouse(event.click)) {
            setGoal();
        }

        if (onInput(event.button(), event.action)) event.cancel();
    }

    private boolean onInput(int key, KeyAction action) {
        if (Input.getKey(mc.options.keyUp) == key) {
            forward = action != KeyAction.Release;
            mc.options.keyUp.setDown(false);
        }
        else if (Input.getKey(mc.options.keyDown) == key) {
            backward = action != KeyAction.Release;
            mc.options.keyDown.setDown(false);
        }
        else if (Input.getKey(mc.options.keyRight) == key) {
            right = action != KeyAction.Release;
            mc.options.keyRight.setDown(false);
        }
        else if (Input.getKey(mc.options.keyLeft) == key) {
            left = action != KeyAction.Release;
            mc.options.keyLeft.setDown(false);
        }
        else if (Input.getKey(mc.options.keyJump) == key) {
            up = action != KeyAction.Release;
            mc.options.keyJump.setDown(false);
        }
        else if (Input.getKey(mc.options.keyShift) == key) {
            down = action != KeyAction.Release;
            mc.options.keyShift.setDown(false);
        }
        else {
            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (speedScrollSensitivity.get() > 0 && mc.screen == null) {
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
        if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
            Entity entity = mc.level.getEntity(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        }
        else if (event.packet instanceof ClientboundSetHealthPacket packet) {
            if (mc.player.getHealth() - packet.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                info("Toggled off because you took damage.");
            }
        }
        else if (event.packet instanceof ClientboundRespawnPacket) {
            if (isActive()) {
                toggle();
                info("Toggled off because you changed dimensions.");
            }
        }
    }

    private boolean checkGuiMove() {
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if (mc.screen != null && !guiMove.isActive()) return true;
        return (mc.screen != null && guiMove.isActive() && guiMove.skip());
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = Mth.clamp(pitch, -90, 90);
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }

    public boolean staySneaking() {
        return isActive() && !mc.player.getAbilities().flying && staySneaking.get() && isSneaking;
    }

    public double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.x, pos.x);
    }
    public double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.y, pos.y);
    }
    public double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.z, pos.z);
    }

    public double getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }
    public double getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }
}
