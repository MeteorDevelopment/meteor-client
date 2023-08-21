/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

/*
 * Credit to Luna (https://github.com/InLieuOfLuna) for making the original Elytra Recast mod (https://github.com/InLieuOfLuna/elytra-recast)!
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class Recast extends ElytraFlightMode {

    public Recast() {
        super(ElytraFlightModes.Recast);
    }

    public static boolean rubberbanded = false;

    int tickDelay = elytraFly.restartDelay.get();

    @Override
    public void onTick() {
        super.onTick();

        // Make sure all the conditions are met (player has an elytra, isn't in water, etc)
        if (ElytraFly.checkConditions(mc.player)) {

            if (elytraFly.flightMode.get() == ElytraFlightModes.Recast) {
                mc.player.setSprinting(true);
                setPressed(mc.options.forwardKey, true);
                if (elytraFly.autoJump.get()) setPressed(mc.options.jumpKey, true);
                mc.player.setYaw(getSmartYawDirection());
                mc.player.setPitch(elytraFly.pitch.get());
            }

            // Rubberbanding
            if (rubberbanded && elytraFly.restart.get()) {
                if (tickDelay > 0) {
                    tickDelay--;
                } else {
                    rubberbanded = false;
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    tickDelay = elytraFly.restartDelay.get();
                }
            }
        }
    }

    private void unpress() {
        setPressed(mc.options.forwardKey, false);
        if (elytraFly.autoJump.get()) setPressed(mc.options.jumpKey, false);
    }

    @Override
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            rubberbanded = true;
            mc.player.stopFallFlying();
        }
    }


    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    private float getSmartYawDirection() {
        return Math.round((mc.player.getYaw() + 1f) / 45f) * 45f;
    }

    @Override
    public void onDeactivate() {
        unpress();
    }
}
