/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Rotation extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noRotate = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-rotate")
            .description("Attempts to block server to client rotations.")
            .defaultValue(true)
            .build()
    );

    // Yaw
    private final SettingGroup sgYaw = settings.createGroup("Yaw");

    private final Setting<Boolean> yawEnabled = sgYaw.add(new BoolSetting.Builder()
            .name("yaw-enabled")
            .description("Locks your yaw.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> yawAngle = sgYaw.add(new DoubleSetting.Builder()
            .name("yaw-angle")
            .description("Yaw angle in degrees.")
            .defaultValue(0)
            .build()
    );

    private final Setting<Boolean> yawAutoAngle = sgYaw.add(new BoolSetting.Builder()
            .name("yaw-auto-angle")
            .description("Automatically uses the best angle.")
            .defaultValue(true)
            .build()
    );

    // Pitch
    private final SettingGroup sgPitch = settings.createGroup("Pitch");

    private final Setting<Boolean> pitchEnabled = sgPitch.add(new BoolSetting.Builder()
            .name("pitch-enabled")
            .description("Locks your pitch.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> pitchAngle = sgPitch.add(new DoubleSetting.Builder()
            .name("pitch-angle")
            .description("Pitch angle in degrees.")
            .defaultValue(0)
            .min(-90)
            .max(90)
            .build()
    );

    public Rotation() {
        super(Category.Player, "rotation", "Allows you to lock your yaw and pitch.");
    }

    @Override
    public void onActivate() {
        onTick.invoke(null);
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        // Yaw
        if (yawEnabled.get()) {
            if (yawAutoAngle.get()) mc.player.yaw = getYawDirection();
            else mc.player.yaw = yawAngle.get().floatValue();
        }

        // Pitch
        if (pitchEnabled.get()) {
            mc.player.pitch = pitchAngle.get().floatValue();
        }
    });

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (noRotate.get() && event.packet instanceof PlayerMoveC2SPacket && !ModuleManager.INSTANCE.get(XpBottleThrower.class).isActive()) {
            IPlayerMoveC2SPacket packet = (IPlayerMoveC2SPacket) event.packet;
            packet.setPitch(mc.player.getPitch(0));
            packet.setYaw(mc.player.getYaw(1));
        }
    });

    private float getYawDirection() {
        return Math.round((mc.player.yaw + 1f) / 45f) * 45f;
    }
}
