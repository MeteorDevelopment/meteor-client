/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class BoatFly extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoSteer = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-steer")
            .description("Automatically steer in the direction you are facing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> upwardsSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("upwards-speed")
            .description("How fast you fly upwards.")
            .defaultValue(0.3)
            .min(0)
            .build()
    );

    private final Setting<Boolean> slowFalling = sgGeneral.add(new BoolSetting.Builder()
            .name("slow-falling")
            .description("Makes you fall slower.")
            .defaultValue(true)
            .build()
    );

    public BoatFly() {
        super(Category.Movement, "boat-fly", "Transforms your boat into a plane.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!(mc.player.getVehicle() instanceof BoatEntity)) return;

        if (autoSteer.get()) {
            mc.player.getVehicle().yaw = mc.player.yaw;
        }

        Vec3d velocity = mc.player.getVehicle().getVelocity();
        if (mc.options.keyJump.isPressed()) {
            ((IVec3d) velocity).set(velocity.x, upwardsSpeed.get(), velocity.z);
        } else {
            if (slowFalling.get()) {
                ((IVec3d) velocity).set(velocity.x, 0, velocity.z);
            }
        }
    });
}
