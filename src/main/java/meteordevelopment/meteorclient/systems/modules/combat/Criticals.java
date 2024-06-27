/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;


import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMace = settings.createGroup("Mace");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode on how Criticals will function.")
        .defaultValue(Mode.Packet)
        .build()
    );

    private final Setting<Boolean> ka = sgGeneral.add(new BoolSetting.Builder()
        .name("only-killaura")
        .description("Only performs crits when using killaura.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.None)
        .build()
    );

    private final Setting<Boolean> mace = sgMace.add(new BoolSetting.Builder()
        .name("smash-attack")
        .description("Will always perform smash attacks when using a mace.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> extraHeight = sgMace.add(new DoubleSetting.Builder()
    	.name("additional-height")
    	.description("The amount of additional height to spoof. More height means more damage.")
    	.defaultValue(0.0)
        .min(0)
        .sliderRange(0, 100)
        .visible(mace::get)
    	.build()
    );

    private PlayerInteractEntityC2SPacket attackPacket;
    private HandSwingC2SPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;

    public Criticals() {
        super(Categories.Combat, "criticals", "Performs critical attacks when you hit your target.");
    }

    @Override
    public void onActivate() {
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (mace.get() && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
                if (mc.player.isFallFlying()) return;

                sendPacket(0);
                sendPacket(1.501 + extraHeight.get());
                sendPacket(0);
            } else {
                if (skipCrit()) return;

                Entity entity = packet.getEntity();

                if (!(entity instanceof LivingEntity) || (entity != Modules.get().get(KillAura.class).getTarget() && ka.get()))
                    return;

                switch (mode.get()) {
                    case Packet -> {
                        sendPacket(0.0625);
                        sendPacket(0);
                    }
                    case Bypass -> {
                        sendPacket(0.11);
                        sendPacket(0.1100013579);
                        sendPacket(0.0000013579);
                    }
                    case Jump, MiniJump -> {
                        if (!sendPackets) {
                            sendPackets = true;
                            sendTimer = mode.get() == Mode.Jump ? 6 : 4;
                            attackPacket = (PlayerInteractEntityC2SPacket) event.packet;

                            if (mode.get() == Mode.Jump) mc.player.jump();
                            else ((IVec3d) mc.player.getVelocity()).setY(0.25);
                            event.cancel();
                        }
                    }
                }
            }
        }
        else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Mode.Packet) {
            if (skipCrit()) return;

            if (sendPackets && swingPacket == null) {
                swingPacket = (HandSwingC2SPacket) event.packet;

                event.cancel();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (sendPackets) {
            if (sendTimer <= 0) {
                sendPackets = false;

                if (attackPacket == null || swingPacket == null) return;
                mc.getNetworkHandler().sendPacket(attackPacket);
                mc.getNetworkHandler().sendPacket(swingPacket);

                attackPacket = null;
                swingPacket = null;
            } else {
                sendTimer--;
            }
        }
    }

    private void sendPacket(double height) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y + height, z, false);
        ((IPlayerMoveC2SPacket) packet).setTag(1337);

        mc.player.networkHandler.sendPacket(packet);
    }

    private boolean skipCrit() {
        return !mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing();
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    public enum Mode {
        None,
        Packet,
        Bypass,
        Jump,
        MiniJump
    }
}
