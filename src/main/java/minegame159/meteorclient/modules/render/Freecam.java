/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.KeyAction;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Freecam extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
            .name("reload-chunks")
            .description("Disables cave culling.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
            .name("render-hands")
            .description("Render hands when in freecam.")
            .defaultValue(true)
            .build()
    );

    public final Vec3d pos = new Vec3d(0, 0, 0);
    public final Vec3d prevPos = new Vec3d(0, 0, 0);

    public float yaw, pitch;
    public float prevYaw, prevPitch;

    private boolean forward, backward, right, left, up, down;

    public Freecam() {
        super(Category.Render, "freecam", "You know what it does.");
    }

    @Override
    public void onActivate() {
        ((IVec3d) pos).set(mc.gameRenderer.getCamera().getPos());
        ((IVec3d) prevPos).set(mc.gameRenderer.getCamera().getPos());

        yaw = mc.player.yaw;
        pitch = mc.player.pitch;
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
    }

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> unpress());

    private void unpress() {
        ((IKeyBinding) mc.options.keyForward).setPressed(false);
        ((IKeyBinding) mc.options.keyBack).setPressed(false);
        ((IKeyBinding) mc.options.keyRight).setPressed(false);
        ((IKeyBinding) mc.options.keyLeft).setPressed(false);
        ((IKeyBinding) mc.options.keyJump).setPressed(false);
        ((IKeyBinding) mc.options.keySneak).setPressed(false);
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        Vec3d forward = Vec3d.fromPolar(0, getYaw(1 / 20f));
        Vec3d right = Vec3d.fromPolar(0, getYaw(1 / 20f) + 90);
        double velX = 0;
        double velY = 0;
        double velZ = 0;

        if (mc.options.keyForward.isPressed()) System.out.println("OMG");

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

        ((IVec3d) prevPos).set(pos);
        ((IVec3d) pos).set(pos.x + velX, pos.y + velY, pos.z + velZ);
    });

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        boolean cancel = true;

        if (KeyBindingHelper.getBoundKeyOf(mc.options.keyForward).getCode() == event.key) {
            forward = event.action != KeyAction.Release;
        } else if (KeyBindingHelper.getBoundKeyOf(mc.options.keyBack).getCode() == event.key) {
            backward = event.action != KeyAction.Release;
        } else if (KeyBindingHelper.getBoundKeyOf(mc.options.keyRight).getCode() == event.key) {
            right = event.action != KeyAction.Release;
        } else if (KeyBindingHelper.getBoundKeyOf(mc.options.keyLeft).getCode() == event.key) {
            left = event.action != KeyAction.Release;
        } else if (KeyBindingHelper.getBoundKeyOf(mc.options.keyJump).getCode() == event.key) {
            up = event.action != KeyAction.Release;
        } else if (KeyBindingHelper.getBoundKeyOf(mc.options.keySneak).getCode() == event.key) {
            down = event.action != KeyAction.Release;
        } else {
            cancel = false;
        }

        if (cancel) event.cancel();
    });

    public void changeLookDirection(double deltaX, double deltaY) {
        prevYaw = yaw;
        prevPitch = pitch;

        yaw += deltaX;
        pitch += deltaY;

        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    public double getX(float delta) {
        return MathHelper.lerp(delta, prevPos.x, pos.x);
    }

    public double getY(float delta) {
        return MathHelper.lerp(delta, prevPos.y, pos.y);
    }

    public double getZ(float delta) {
        return MathHelper.lerp(delta, prevPos.z, pos.z);
    }

    public float getYaw(float delta) {
        return MathHelper.lerp(delta, prevYaw, yaw);
    }

    public float getPitch(float delta) {
        return MathHelper.lerp(delta, prevPitch, pitch);
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }
}
