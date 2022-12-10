/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick"); //Pog
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode for Flight.")
        .defaultValue(Mode.Abilities)
        .onChanged(mode -> {
            if (!isActive()) return;
            abilitiesOff();
        })
        .build()
    );
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .build()
    );
    private final Setting<Boolean> verticalSpeedMatch = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .build()
    );
    private final Setting<AntiKickMode> antiKickMode = sgAntiKick.add(new EnumSetting.Builder<AntiKickMode>()
        .name("mode")
        .description("The mode for anti kick.")
        .defaultValue(AntiKickMode.Packet)
        .build()
    );
    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .build()
    );

    // Anti Kick
    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in milliseconds, to fly down a bit to reset floating ticks.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );
    
    private int delayLeft = delay.get();
    private int offLeft = offTime.get();
    private boolean flip;
    private float lastYaw;
    private double lastPacketY = Double.MAX_VALUE;

    public Flight() {
        super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = true;
            if (mc.player.getAbilities().creativeMode) return;
            mc.player.getAbilities().allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            abilitiesOff();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        float currentYaw = mc.player.getYaw();
        if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getVelocity().length() < 0.003d) {
            mc.player.setYaw(currentYaw + (flip ? 1 : -1));
            flip = !flip;
        }
        lastYaw = currentYaw;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (delayLeft > 0) delayLeft--;

        if (offLeft <= 0 && delayLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();

            if (antiKickMode.get() == AntiKickMode.Packet) {
                // Resend movement packets
                ((ClientPlayerEntityAccessor) mc.player).setTicksSinceLastPositionPacketSent(20);
            }
        } else if (delayLeft <= 0) {
            boolean shouldReturn = false;

            if (antiKickMode.get() == AntiKickMode.Normal) {
                if (mode.get() == Mode.Abilities) {
                    abilitiesOff();
                    shouldReturn = true;
                }
            } else if (antiKickMode.get() == AntiKickMode.Packet && offLeft == offTime.get()) {
                // Resend movement packets
                ((ClientPlayerEntityAccessor) mc.player).setTicksSinceLastPositionPacketSent(20);
            }

            offLeft--;

            if (shouldReturn) return;
        }

        if (mc.player.getYaw() != lastYaw) mc.player.setYaw(lastYaw);

        switch (mode.get()) {
            case Velocity -> {

                 /*TODO: deal with underwater movement, find a way to "spoof" not being in water
                also, all of the multiplication below is to get the speed to roughly match the speed
                you get when using vanilla fly*/

                mc.player.getAbilities().flying = false;
                mc.player.airStrafingSpeed = speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);
                mc.player.setVelocity(0, 0, 0);
                Vec3d initialVelocity = mc.player.getVelocity();
                if (mc.options.jumpKey.isPressed())
                    mc.player.setVelocity(initialVelocity.add(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
                if (mc.options.sneakKey.isPressed())
                    mc.player.setVelocity(initialVelocity.subtract(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
            }
            case Abilities -> {
                if (mc.player.isSpectator()) return;
                mc.player.getAbilities().setFlySpeed(speed.get().floatValue());
                mc.player.getAbilities().flying = true;
                if (mc.player.getAbilities().creativeMode) return;
                mc.player.getAbilities().allowFlying = true;
            }
        }
    }

    private void antiKickPacket(PlayerMoveC2SPacket packet, double currentY) {
        // maximum time we can be "floating" is 80 ticks, so 4 seconds max
        if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
            shouldFlyDown(currentY, this.lastPacketY) && isEntityOnAir(mc.player)) {
            // actual check is for >= -0.03125D, but we have to do a bit more than that
            // due to the fact that it's a bigger or *equal* to, and not just a bigger than
            ((PlayerMoveC2SPacketAccessor) packet).setY(lastPacketY - 0.03130D);
        } else {
            lastPacketY = currentY;
        }
    }

    /**
     * @see ServerPlayNetworkHandler#onPlayerMove(PlayerMoveC2SPacket)
     */
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket packet) || antiKickMode.get() != AntiKickMode.Packet) return;

        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            antiKickPacket(packet, currentY);
        } else {
            // if the packet is a LookAndOnGround packet or an OnGroundOnly packet then we need to
            // make it a Full packet or a PositionAndOnGround packet respectively, so it has a Y value
            PlayerMoveC2SPacket fullPacket;
            if (packet.changesLook()) {
                fullPacket = new PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.getYaw(0),
                    packet.getPitch(0),
                    packet.isOnGround()
                );
            } else {
                fullPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.isOnGround()
                );
            }
            event.cancel();
            antiKickPacket(fullPacket, mc.player.getY());
            mc.getNetworkHandler().sendPacket(fullPacket);
        }
    }

    private boolean shouldFlyDown(double currentY, double lastY) {
        if (currentY >= lastY) {
            return true;
        } else return lastY - currentY < 0.03130D;
    }

    private void abilitiesOff() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05f);
        if (mc.player.getAbilities().creativeMode) return;
        mc.player.getAbilities().allowFlying = false;
    }

    // Copied from ServerPlayNetworkHandler#isEntityOnAir
    private boolean isEntityOnAir(Entity entity) {
        return entity.world.getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlock.AbstractBlockState::isAir);
    }

    public enum Mode {
        Abilities,
        Velocity
    }

    public enum AntiKickMode {
        Normal,
        Packet,
        None
    }
}
