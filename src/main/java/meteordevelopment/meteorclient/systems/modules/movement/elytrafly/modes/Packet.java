/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
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
    public void onDeactivate() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().allowFlying = false;
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mc.player.getInventory().getArmorStack(2).getItem() != Items.ELYTRA || mc.player.fallDistance <= 0.2 || mc.options.sneakKey.isPressed()) return;

        if (mc.options.forwardKey.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY(-(float) Math.toRadians(mc.player.getYaw()));
        } else if (mc.options.backKey.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY((float) Math.toRadians(mc.player.getYaw()));
        }

        if (mc.options.jumpKey.isPressed()) {
            vec3d.add(0, elytraFly.verticalSpeed.get(), 0);
        } else if (!mc.options.jumpKey.isPressed()) {
            vec3d.add(0, -elytraFly.verticalSpeed.get(), 0);
        }

        mc.player.setVelocity(vec3d);
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
    }

    //Walalalalalalalalalalalala
    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    @Override
    public void onPlayerMove() {
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(elytraFly.horizontalSpeed.get().floatValue() / 20);
    }
}
