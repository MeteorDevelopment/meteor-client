/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement.elytrafly.modes;

import minegame159.meteorclient.modules.movement.elytrafly.ElytraFlightMode;
import minegame159.meteorclient.modules.movement.elytrafly.ElytraFlightModes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Packet extends ElytraFlightMode {

    private final Vec3d vec3d = new Vec3d(0,0,0);

    public Packet() {
        super(ElytraFlightModes.Packet);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mc.player.inventory.getArmorStack(2).getItem() != Items.ELYTRA || mc.player.fallDistance <= 0.2 || mc.options.keySneak.isPressed()) return;

        if (mc.options.keyForward.isPressed()) {
            vec3d.add(0, 0, settings.horizontalSpeed.get());
            vec3d.rotateY(-(float) Math.toRadians(mc.player.yaw));
        } else if (mc.options.keyBack.isPressed()) {
            vec3d.add(0, 0, settings.horizontalSpeed.get());
            vec3d.rotateY((float) Math.toRadians(mc.player.yaw));
        }

        if (mc.options.keyJump.isPressed()) {
            vec3d.add(0, settings.verticalSpeed.get(), 0);
        } else if (!mc.options.keyJump.isPressed()) {
            vec3d.add(0, -settings.verticalSpeed.get(), 0);
        }

        mc.player.setVelocity(vec3d);
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
    }
}
