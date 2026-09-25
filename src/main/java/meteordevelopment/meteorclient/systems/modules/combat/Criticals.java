/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;


import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;

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

    private ServerboundAttackPacket attackPacket;
    private ServerboundSwingPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;
    private double lastY;
    private boolean waitingForPeak;

    public Criticals() {
        super(Categories.Combat, "criticals", "Performs critical attacks when you hit your target.");
    }

    @Override
    public void onActivate() {
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
        lastY = 0;
        waitingForPeak = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundAttackPacket(int entityId)) {
            if (mace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) {
                if (mc.player.isFallFlying()) return;

                sendPacket(0);
                sendPacket(1.501 + extraHeight.get());
                sendPacket(0);
            } else {
                if (skipCrit()) return;

                Entity entity = mc.level.getEntity(entityId);

                if (!(entity instanceof LivingEntity) || (entity != Modules.get().get(KillAura.class).getTarget() && ka.get()))
                    return;

                switch (mode.get()) {
                    case Packet -> {
                        sendPacket(0.0625);
                        sendPacket(0);
                    }
                    case UpdatedNCP -> {
                        sendPacket(0.0000008);
                        sendPacket(0);
                    }
                    case OldNCP -> {
                        sendPacket(0.11);
                        sendPacket(0.1100013579);
                        sendPacket(0.0000013579);
                    }
                    case Jump, MiniJump -> {
                        if (!sendPackets) {
                            sendPackets = true;
                            attackPacket = (ServerboundAttackPacket) event.packet;

                            if (mode.get() == Mode.Jump) {
                                mc.player.jumpFromGround();
                                waitingForPeak = true;
                                lastY = mc.player.getY();
                            } else {
                                ((IVec3) mc.player.getDeltaMovement()).meteor$setY(0.25);
                                sendTimer = 4;
                            }
                            event.cancel();
                        }
                    }
                }
            }
        } else if (event.packet instanceof ServerboundSwingPacket serverboundSwingPacket && mode.get() != Mode.Packet) {
            if (skipCrit()) return;

            if (sendPackets && swingPacket == null) {
                swingPacket = serverboundSwingPacket;
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (sendPackets) {
            if (mode.get() == Mode.Jump && waitingForPeak) {
                double currentY = mc.player.getY();
                if (currentY <= lastY) {
                    waitingForPeak = false;
                    sendTimer = 0; // Attack on next tick after reaching peak
                }
                lastY = currentY;
                return;
            }

            if (sendTimer <= 0) {
                if (attackPacket == null || swingPacket == null) {
                    sendPackets = false;
                    return;
                }
                mc.getConnection().send(attackPacket);
                mc.getConnection().send(swingPacket);

                attackPacket = null;
                swingPacket = null;

                sendPackets = false;
            } else {
                sendTimer--;
            }
        }
    }

    private void sendPacket(double height) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(x, y + height, z, false, false);
        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.player.connection.send(packet);
    }

    private boolean skipCrit() {
        if (EntityUtils.isInCobweb(mc.player) && (mode.get() == Mode.Jump || mode.get() == Mode.MiniJump))
            return true;

        return !mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable();
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    public enum Mode {
        None,
        Packet,
        UpdatedNCP,
        OldNCP,
        Jump,
        MiniJump
    }
}
