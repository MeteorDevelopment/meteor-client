/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

//Updated by squidoodly 18/07/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    public enum Mode {
        Packet,
        Jump,
        MiniJump
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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
            .build()
    );

    private final Setting<Boolean> crystals = sgGeneral.add(new BoolSetting.Builder()
            .name("crystals")
            .description("Wether to crit crystals or not.")
            .defaultValue(false)
            .build()
    );

    public Criticals() {
        super(Categories.Combat, "criticals", "Performs critical attacks when you hit your target.");
    }

    private PlayerInteractEntityC2SPacket attackPacket;
    private HandSwingC2SPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;

    @Override
    public void onActivate() {
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket && ((PlayerInteractEntityC2SPacket) event.packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.ATTACK) {
            if(((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world) != Modules.get().get(KillAura.class).getTarget() && ka.get()) return;
            if((((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world) instanceof EndCrystalEntity) && !crystals.get()) return;
            if (skipCrit()) return;
            if (mode.get() == Mode.Packet) doPacketMode();
            else doJumpMode(event);
        } else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Mode.Packet) {
            if (skipCrit()) return;
            doJumpModeSwing(event);
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

    private void doPacketMode() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        PlayerMoveC2SPacket p1 = new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false);
        PlayerMoveC2SPacket p2 = new PlayerMoveC2SPacket.PositionOnly(x, y, z, false);

        //Ignore IntelliJ, it's fucking stupid. This is valid.
        ((IPlayerMoveC2SPacket) p1).setTag(1337);
        ((IPlayerMoveC2SPacket) p2).setTag(1337);

        mc.player.networkHandler.sendPacket(p1);
        mc.player.networkHandler.sendPacket(p2);
    }

    private void doJumpMode(PacketEvent.Send event) {
        if (!sendPackets) {
            sendPackets = true;
            sendTimer = mode.get() == Mode.Jump ? 6 : 4;
            attackPacket = (PlayerInteractEntityC2SPacket) event.packet;

            if (mode.get() == Mode.Jump) mc.player.jump();
            else ((IVec3d) mc.player.getVelocity()).setY(0.25);
            event.cancel();
        }
    }

    private void doJumpModeSwing(PacketEvent.Send event) {
        if (sendPackets && swingPacket == null) {
            swingPacket = (HandSwingC2SPacket) event.packet;

            event.cancel();
        }
    }

    private boolean skipCrit() {
        boolean a = !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isClimbing();
        if (!mc.player.isOnGround()) return true;
        return !a;
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }
}
