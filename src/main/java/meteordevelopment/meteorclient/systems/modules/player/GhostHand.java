/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class GhostHand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
        .name("active-when")
        .description("Ghost-Hand is active when you meet these requirements.")
        .defaultValue(ActiveWhen.NotSneaking)
        .build()
    );

    private final Setting<Boolean> staySneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-sneaking")
        .description("If you are sneaking when you open a container, whether your player should remain sneaking.")
        .defaultValue(false)
        .visible(() -> activeWhen.get() != ActiveWhen.NotSneaking)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swings your hand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> prioritizeItemUse = sgGeneral.add(new BoolSetting.Builder()
        .name("Prioritize-Item-Use")
        .description("Will not open containers if you are using an item. ")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> posList = new ObjectOpenHashSet<>();
    private boolean isSneaking = false;

    public GhostHand() {
        super(Categories.Player, "ghost-hand", "Opens containers through walls.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.options.useKey.isPressed()) return;
        if (prioritizeItemUse.get() && (mc.player.isUsingItem() || mc.interactionManager.isBreakingBlock() || mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (activeWhen.get() == ActiveWhen.Sneaking && !mc.player.isSneaking()) return;
        if (activeWhen.get() == ActiveWhen.NotSneaking && mc.player.isSneaking()) return;

        // why tf is this always false
        if (mc.world.getBlockState(BlockPos.ofFloored(mc.player.raycast(mc.player.getBlockInteractionRange(), mc.getRenderTickCounter().getTickProgress(true), false).getPos())).hasBlockEntity()) return;

//        info("1");
//        info(String.valueOf(mc.player.getPitch()));

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d direction = null;

        if (Modules.get().isActive(Freecam.class)) {
            direction = new Vec3d(0, 0, 0.1)
                    .rotateX(-(float) Math.toRadians(Modules.get().get(Freecam.class).getPitch(tickDelta)))
                    .rotateY(-(float) Math.toRadians(Modules.get().get(Freecam.class).getYaw(tickDelta)));
        } else {
            direction = new Vec3d(0, 0, 0.1)
                    .rotateX(-(float) Math.toRadians(mc.player.getPitch()))
                    .rotateY(-(float) Math.toRadians(mc.player.getYaw()));
        }

//        info("2");
//        info(String.valueOf(direction));

        posList.clear();

        for (int i = 1; i < mc.player.getBlockInteractionRange() * 10; i++) {
            BlockPos pos = null;
            if (Modules.get().isActive(Freecam.class)) {
                pos = BlockPos.ofFloored(new Vec3d(
                        Modules.get().get(Freecam.class).getX(tickDelta),
                        Modules.get().get(Freecam.class).getY(tickDelta),
                        Modules.get().get(Freecam.class).getZ(tickDelta)
                ).add(direction.multiply(i)));
            } else {
                 pos = BlockPos.ofFloored(mc.player.getCameraPosVec(tickDelta).add(direction.multiply(i)));
            }

//            info(String.valueOf(Modules.get().isActive(Freecam.class)));
//            info(String.valueOf(pos));

            if (posList.contains(pos)) continue;
            posList.add(pos);

            if (mc.world.getBlockState(pos).hasBlockEntity()) {
                warning("HIT: " + pos);
                if (!prioritizeItemUse.get()) mc.interactionManager.stopUsingItem(mc.player);
                if (mc.player.isSneaking() && staySneaking.get()) isSneaking = true;
                if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
                return;
            }
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        isSneaking = false;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof CloseScreenS2CPacket) isSneaking = false;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof CloseHandledScreenC2SPacket) isSneaking = false;
    }

    public boolean staySneaking() {
        return isActive() && !mc.player.getAbilities().flying && isSneaking;
    }

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }
}
