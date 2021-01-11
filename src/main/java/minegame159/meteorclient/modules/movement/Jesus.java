/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.FluidCollisionShapeEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.Material;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Jesus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> walkOnWater = sgGeneral.add(new BoolSetting.Builder()
            .name("walk-on-water")
            .description("Lets you walk on water.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> walkOnLava = sgGeneral.add(new BoolSetting.Builder()
            .name("walk-on-lava")
            .description("Lets you walk on lava.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnSneak = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-sneak")
            .description("Lets you go under the liquid when your sneak key is held.")
            .defaultValue(true)
            .build()
    );

    private int tickTimer = 10;
    private int packetTimer = 0;

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private boolean preBaritoneAssumeWalkOnWater;
    private boolean preBaritoneAssumeWalkOnLava;

    public Jesus() {
        super(Category.Movement, "jesus", "Walk on water, be like Jesus (also works on lava).");
    }

    @Override
    public void onActivate() {
        preBaritoneAssumeWalkOnWater = BaritoneAPI.getSettings().assumeWalkOnWater.value;
        preBaritoneAssumeWalkOnLava = BaritoneAPI.getSettings().assumeWalkOnLava.value;

        BaritoneAPI.getSettings().assumeWalkOnWater.value = walkOnWater.get();
        BaritoneAPI.getSettings().assumeWalkOnLava.value = walkOnLava.get();
    }

    @Override
    public void onDeactivate() {
        BaritoneAPI.getSettings().assumeWalkOnWater.value = preBaritoneAssumeWalkOnWater;
        BaritoneAPI.getSettings().assumeWalkOnLava.value = preBaritoneAssumeWalkOnLava;
    }

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        if (mc.options.keySneak.isPressed() && disableOnSneak.get()) return;
        if (mc.player.isTouchingWater() && !walkOnWater.get()) return;
        if (mc.player.isInLava() && !walkOnLava.get()) return;

        // Move up in water
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            Vec3d velocity = mc.player.getVelocity();
            ((IVec3d) velocity).set(velocity.x, 0.11, velocity.z);
            tickTimer = 0;
            return;
        }

        // Simulate jumping out of water
        Vec3d velocity = mc.player.getVelocity();
        if (tickTimer == 0) ((IVec3d) velocity).set(velocity.x, 0.30, velocity.z);
        else if (tickTimer == 1) ((IVec3d) velocity).set(velocity.x, 0, velocity.z);

        tickTimer++;
    });

    @EventHandler
    private final Listener<CanWalkOnFluidEvent> onCanWalkOnFluid = new Listener<>(event -> {
        if (event.entity != mc.player) return;
        if (mc.options.keySneak.isPressed() && disableOnSneak.get()) return;

        if ((event.fluid == Fluids.WATER || event.fluid == Fluids.FLOWING_WATER) && walkOnWater.get()) event.walkOnFluid = true;
        else if ((event.fluid == Fluids.LAVA || event.fluid == Fluids.FLOWING_LAVA) && walkOnLava.get()) event.walkOnFluid = true;
    });

    @EventHandler
    private final Listener<FluidCollisionShapeEvent> onFluidCollisionShape = new Listener<>(event -> {
        if (!(mc.options.keySneak.isPressed() && disableOnSneak.get()) && mc.player.fallDistance <= 3) {
            if (event.state.getMaterial() == Material.WATER && !mc.player.isTouchingWater() && walkOnWater.get()) event.shape = VoxelShapes.fullCube();
            else if (event.state.getMaterial() == Material.LAVA && !mc.player.isInLava() && walkOnLava.get()) event.shape = VoxelShapes.fullCube();
        }
    });

    @EventHandler
    private final Listener<PacketEvent.Send> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof PlayerMoveC2SPacket)) return;
        if (mc.options.keySneak.isPressed() && disableOnSneak.get()) return;
        if (mc.player.isTouchingWater() && !walkOnWater.get()) return;
        if (mc.player.isInLava() && !walkOnLava.get()) return;

        PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;

        // Check if packet contains a position
        if (!(packet instanceof PlayerMoveC2SPacket.PositionOnly || packet instanceof PlayerMoveC2SPacket.Both)) return;

        // Check inWater, fallDistance and if over liquid
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.fallDistance > 3f || !isOverLiquid()) return;

        // If not actually moving, cancel packet
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) {
            event.cancel();
            return;
        }

        // Wait for timer
        packetTimer++;
        if (packetTimer < 4) return;

        // Cancel old packet
        event.cancel();

        // Get position
        double x = packet.getX(0);
        double y = packet.getY(0) + 0.05;
        double z = packet.getZ(0);

        // Create new packet
        Packet<?> newPacket;
        if (packet instanceof PlayerMoveC2SPacket.PositionOnly) newPacket = new PlayerMoveC2SPacket.PositionOnly(x, y, z, true);
        else newPacket = new PlayerMoveC2SPacket.Both(x, y, z, packet.getYaw(0), packet.getPitch(0), true);

        // Send new packet
        mc.getNetworkHandler().getConnection().send(newPacket);
    });

    public boolean isOverLiquid() {
        boolean foundLiquid = false;
        boolean foundSolid = false;

        List<Box> blockCollisions = mc.world
                .getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.5, 0))
                .map(VoxelShape::getBoundingBox)
                .collect(Collectors.toCollection(ArrayList::new));

        for (Box bb : blockCollisions) {
            blockPos.set(MathHelper.lerp(0.5D, bb.minX, bb.maxX), MathHelper.lerp(0.5D, bb.minY, bb.maxY), MathHelper.lerp(0.5D, bb.minZ, bb.maxZ));
            Material material = mc.world.getBlockState(blockPos).getMaterial();

            if(material == Material.WATER || material == Material.LAVA) foundLiquid = true;
            else if(material != Material.AIR) foundSolid = true;
        }

        return foundLiquid && !foundSolid;
    }
}
