/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 * Introduced by iDucky (https://github.com/i-duckyy)
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MaceKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> preventDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("No Fall")
        .description("Attempts to prevent fall damage even on packet hiccups.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Power")
        .description("Simulates a fall from this distance")
        .defaultValue(35)
        .sliderRange(1, 200)
        .min(1)
        .max(200)
        .build());

    private final Setting<Boolean> packetDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("Disable When Blocked")
        .description("Does not send movement packets if the attack was blocked. (prevents death)")
        .defaultValue(true)
        .build());

    public MaceKill() {
        super(Categories.Combat, "MaceKill", "Makes the Mace powerful when swung.");
    }

    private Vec3d previouspos;

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode()))
            return;

        previouspos = mc.player.getPos();
        int blocks = getMaxHeightAbovePlayer();
        int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) packetsRequired = 1;

        BlockPos isopenair1 = mc.player.getBlockPos().add(0, blocks, 0);
        BlockPos isopenair2 = mc.player.getBlockPos().add(0, blocks + 1, 0);
        if (!isSafeBlock(isopenair1) || !isSafeBlock(isopenair2)) return;

        if (blocks <= 22) {
            if (mc.player.hasVehicle()) {
                for (int i = 0; i < 4; i++) {
                    assert mc.player.getVehicle() != null;
                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                }
                double maxHeight = Math.min(mc.player.getVehicle().getY() + 22, mc.player.getVehicle().getY() + blocks);
                doVehicleTeleports(maxHeight, blocks);
            } else {
                for (int i = 0; i < 4; i++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }
                double heightY = Math.min(mc.player.getY() + 22, mc.player.getY() + blocks);
                doPlayerTeleports(heightY);
            }
        } else {
            if (mc.player.hasVehicle()) {
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    assert mc.player.getVehicle() != null;
                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                }
                double maxHeight = mc.player.getVehicle().getY() + blocks;
                doVehicleTeleports(maxHeight, blocks);
            } else {
                for (int i = 0; i < packetsRequired - 1; i++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }
                double heightY = mc.player.getY() + blocks;
                doPlayerTeleports(heightY);
            }
        }
    }

    private void doPlayerTeleports(double height) {
        assert mc.player != null;
        PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            mc.player.getX(), height, mc.player.getZ(), false, mc.player.horizontalCollision);
        PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            previouspos.getX(), previouspos.getY(), previouspos.getZ(),
            false, mc.player.horizontalCollision);
        if (preventDeath.get()) {
            homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                previouspos.getX(), previouspos.getY() + 0.25, previouspos.getZ(),
                false, mc.player.horizontalCollision);
        }
        ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
        mc.player.networkHandler.sendPacket(movepacket);
        mc.player.networkHandler.sendPacket(homepacket);
        if (preventDeath.get()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
            mc.player.fallDistance = 0;
        }
    }

    private void doVehicleTeleports(double height, int blocks) {
        assert mc.player != null;
        assert mc.player.getVehicle() != null;
        mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), height + blocks, mc.player.getVehicle().getZ());
        mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
        mc.player.getVehicle().setPosition(previouspos);
        mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
    }

    private int getMaxHeightAbovePlayer() {
        assert mc.player != null;
        BlockPos playerPos = mc.player.getBlockPos();
        int maxHeight = playerPos.getY() + (fallHeight.get());
        for (int i = maxHeight; i > playerPos.getY(); i--) {
            BlockPos up1 = new BlockPos(playerPos.getX(), i, playerPos.getZ());
            BlockPos up2 = up1.up(1);
            if (isSafeBlock(up1) && isSafeBlock(up2)) return i - playerPos.getY();
        }
        return 0;
    }

    private boolean isSafeBlock(BlockPos pos) {
        assert mc.world != null;
        return mc.world.getBlockState(pos).isReplaceable()
            && mc.world.getFluidState(pos).isEmpty()
            && !mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }
}
