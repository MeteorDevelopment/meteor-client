/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

public class NoFall extends Module {
    public enum Mode {
        Packet,
        AirPlace,
        Bucket
    }

    private final Setting<PlaceMode> airplaceMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("Whether place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.BeforeDeath)
        .visible(() -> mode.get() == Mode.AirPlace)
        .build()
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.Packet)
        .build()
    );
    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Bucket)
        .build()
    );
    private final Setting<Boolean> elytra = sgGeneral.add(new BoolSetting.Builder()
        .name("elytra-compatibility")
        .description("Stops No Fall from working when using an elytra.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> minFallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("min-fall-height")
        .description("The minimum height to fall from for no fall to work.")
        .defaultValue(3)
        .min(1)
        .sliderMax(10)
        .build()
    );

    @Override
    public void onActivate() {
        fallHeightBaritone = BaritoneAPI.getSettings().maxFallHeightNoWater.get();
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = 255;
        placedWater = false;
        centeredPlayer = false;
    }

    private boolean placedWater;
    private boolean centeredPlayer;
    private int fallHeightBaritone;
    private double x, z;

    public NoFall() {
        super(Categories.Movement, "no-fall", "Prevents you from taking fall damage.");
    }

    @Override
    public void onDeactivate() {
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = fallHeightBaritone;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player.abilities.creativeMode) return;

        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (elytra.get() && mc.player.isFallFlying()) {
                for (int i = 0; i <= minFallHeight.get(); i++) {
                    BlockPos pos = mc.player.getBlockPos().down(i);

                    if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                        ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                        return;
                    }
                }
            } else if (mode.get() == Mode.Packet) {
                // Packet mode
                if (((IPlayerMoveC2SPacket) event.packet).getTag() != 1337) {
                    ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.abilities.creativeMode) return;

        if (mode.get() == Mode.AirPlace && ((airplaceMode.get() == PlaceMode.BeforeDamage && mc.player.fallDistance > 2) || (airplaceMode.get() == PlaceMode.BeforeDeath && ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < mc.player.fallDistance)))) {
            PlayerUtils.centerPlayer();
            BlockUtils.place(mc.player.getBlockPos().down(2), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), true, 50, true);
        } else if (mode.get() == Mode.Bucket) {
            // Bucket mode
            if (placedWater) {
                // Remove water
                FindItemResult bucket = InvUtils.findInHotbar(Items.BUCKET);

                if (bucket.isHotbar() && mc.player.getBlockState().getFluidState().getFluid() == Fluids.WATER) {
                    useBucket(bucket.getSlot(), false);
                }

                centeredPlayer = false;
            }
            else if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                // Place water
                FindItemResult bucket = InvUtils.findInHotbar(Items.WATER_BUCKET);

                if (anchor.get()) {
                    if (!centeredPlayer || x != mc.player.getX() || z != mc.player.getZ()) {
                        PlayerUtils.centerPlayer();
                        x = mc.player.getX();
                        z = mc.player.getZ();
                        centeredPlayer = true;
                    }
                }

                if (bucket.isHotbar()) {
                    BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                    if (result != null && result.getType() == HitResult.Type.BLOCK) {
                        useBucket(bucket.getSlot(), true);
                    }
                }
            }
        }

        if (mc.player.fallDistance == 0) placedWater = false;
    }

    public enum PlaceMode {
        BeforeDeath,
        BeforeDamage
    }

    private void useBucket(int slot, boolean setPlacedWater) {
        Rotations.rotate(mc.player.yaw, 90, 10, true, () -> {
            int preSlot = mc.player.inventory.selectedSlot;
            InvUtils.swap(slot);
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            InvUtils.swap(preSlot);

            placedWater = setPlacedWater;
        });
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }
}
