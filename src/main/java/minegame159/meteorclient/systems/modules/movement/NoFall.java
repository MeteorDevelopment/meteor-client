/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.player.FindItemResult;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

import java.util.function.Predicate;

public class NoFall extends Module {
    public enum Mode {
        Packet,
        AirPlace,
        Bucket
    }

    public enum PlaceMode {
        BeforeDamage(height -> height > 2),
        BeforeDeath(height -> height > Math.max(PlayerUtils.getTotalHealth(), 2));

        private final Predicate<Float> fallHeight;

        PlaceMode(Predicate<Float> fallHeight) {
            this.fallHeight = fallHeight;
        }

        public boolean test(float fallheight) {
            return fallHeight.test(fallheight);
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.Packet)
        .build()
    );

    private final Setting<PlaceMode> airPlaceMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("Whether place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.BeforeDeath)
        .visible(() -> mode.get() == Mode.AirPlace)
        .build()
    );

    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Packet)
        .build()
    );

    private final Setting<Double> elytraStopHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("elytra-stop-height")
        .description("The height at which you will stop elytra flying.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private boolean placedWater;
    private int preBaritoneFallHeight;

    public NoFall() {
        super(Categories.Movement, "no-fall", "Attempts to prevent you from taking fall damage.");
    }

    @Override
    public void onActivate() {
        // TODO: Baritone
        // preBaritoneFallHeight = BaritoneAPI.getSettings().maxFallHeightNoWater.value;
        // if (mode.get() == Mode.Packet) BaritoneAPI.getSettings().maxFallHeightNoWater.value = 255;
        placedWater = false;
    }

    @Override
    public void onDeactivate() {
        // TODO: Baritone
        //BaritoneAPI.getSettings().maxFallHeightNoWater.value = fallHeightBaritone;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player.getAbilities().creativeMode) return;

        if (event.packet instanceof PlayerMoveC2SPacket) {
            // Elytra compat
            if (mc.player.isFallFlying()) {
                BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, elytraStopHeight.get(), 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                if (result != null && result.getType() == HitResult.Type.BLOCK) {
                    ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                    return;
                }
            }

            // Packet mode
            else if (mode.get() == Mode.Packet) {
                if (((IPlayerMoveC2SPacket) event.packet).getTag() != 1337) {
                    ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.getAbilities().creativeMode) return;

        // Airplace mode
        if (mode.get() == Mode.AirPlace) {
            // Test if fall damage setting is valid
            if (!airPlaceMode.get().test(mc.player.fallDistance)) return;

            // Center and place block
            if (anchor.get()) PlayerUtils.centerPlayer();

            Rotations.rotate(mc.player.getYaw(), 90, Integer.MAX_VALUE, () -> {
                double preY = mc.player.getVelocity().y;
                ((IVec3d) mc.player.getVelocity()).setY(0);

                BlockUtils.place(mc.player.getBlockPos().down(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true);

                ((IVec3d) mc.player.getVelocity()).setY(preY);
            });
        }

        // Bucket mode
        if (mode.get() == Mode.Bucket) {
            if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                // Place water
                FindItemResult waterBucket = InvUtils.findInHotbar(Items.WATER_BUCKET);

                if (!waterBucket.found()) return;

                // Center player
                if (anchor.get()) PlayerUtils.centerPlayer();

                // Check if there is a block within 5 blocks
                BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                // Place water
                if (result != null && result.getType() == HitResult.Type.BLOCK) {
                    useBucket(waterBucket, true);
                }
            }

            // Remove water
            if (placedWater && mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.WATER) {
                useBucket(InvUtils.findInHotbar(Items.BUCKET), false);
            }
        }
    }

    private void useBucket(FindItemResult bucket, boolean placedWater) {
        if (!bucket.found()) return;

        Rotations.rotate(mc.player.getYaw(), 90, 10, true, () -> {
            if (bucket.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
            }
            else {
                int preSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(bucket.getSlot());
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                InvUtils.swap(preSlot);
            }

            this.placedWater = placedWater;
        });
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }
}
