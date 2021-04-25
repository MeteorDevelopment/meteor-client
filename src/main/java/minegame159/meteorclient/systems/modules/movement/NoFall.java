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
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class NoFall extends Module {
    public enum Mode {
        Packet,
        AirPlace,
        Bucket
    }

    public enum PlaceMode{
        BeforeDeath,
        BeforeDamage
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The way you are saved from fall damage.")
            .defaultValue(Mode.Packet)
            .build()
    );

    private final Setting<Boolean> elytra = sgGeneral.add(new BoolSetting.Builder()
            .name("elytra-compatibility")
            .description("Stops No Fall from working when using an elytra.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> baritone = sgGeneral.add(new BoolSetting.Builder()
            .name("baritone-compatibility")
            .description("Makes baritone assume you can fall 255 blocks without damage.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("How high you have to be off the ground for this to toggle on.")
            .defaultValue(0.5)
            .min(0.1)
            .sliderMax(1)
            .build()
    );

    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
            .name("place-mode")
            .description("Whether place mode places before you die or before you take damage.")
            .defaultValue(PlaceMode.BeforeDeath)
            .build()
    );

    private boolean placedWater;
    private int fallHeightBaritone;

    public NoFall() {
        super(Categories.Movement, "no-fall", "Prevents you from taking fall damage.");
    }

    @Override
    public void onActivate() {
        if (baritone.get()) {
            fallHeightBaritone = BaritoneAPI.getSettings().maxFallHeightNoWater.get();
            BaritoneAPI.getSettings().maxFallHeightNoWater.value = 255;
        }
        placedWater = false;
    }

    @Override
    public void onDeactivate() {
        if (baritone.get()) {
            BaritoneAPI.getSettings().maxFallHeightNoWater.value = fallHeightBaritone;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player != null && mc.player.abilities.creativeMode) return;

        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (elytra.get() && (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.options.keyJump.isPressed() || mc.player.isFallFlying())) {
                // Elytra block damage
                for (int i = 0; i <= Math.ceil(height.get()); i++) {
                    if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).getMaterial().isReplaceable()) {
                        if (mc.player.getBlockPos().add(0, -i, 0).getY() + 1 + height.get() >= mc.player.getPos().getY()) {
                            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                            return;
                        }
                    }
                }
            }
            else if (mode.get() == Mode.Packet) {
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

        if (mode.get() == Mode.AirPlace && ((placeMode.get() == PlaceMode.BeforeDamage && mc.player.fallDistance > 2) || (placeMode.get() == PlaceMode.BeforeDeath && ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < mc.player.fallDistance)))) {
            // Air Place mode
            int slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);

            if (slot != -1) {
                BlockUtils.place(mc.player.getBlockPos().down(), Hand.MAIN_HAND, slot, true, 10, true);
            }
        }
        else if (mode.get() == Mode.Bucket) {
            // Bucket mode
            if (placedWater) {
                // Remove water
                int slot = InvUtils.findItemInHotbar(Items.BUCKET);

                if (slot != -1 && mc.player.getBlockState().getFluidState().getFluid() == Fluids.WATER) {
                    useBucket(slot, false);
                }
            }
            else if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                // Place water
                int slot = InvUtils.findItemInHotbar(Items.WATER_BUCKET);

                if (slot != -1) {
                    BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos(), mc.player.getPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                    if (result != null && result.getType() == HitResult.Type.BLOCK) {
                        useBucket(slot, true);
                    }
                }
            }
        }

        if (mc.player.fallDistance == 0) placedWater = false;
    }

    private void useBucket(int slot, boolean setPlacedWater) {
        Rotations.rotate(mc.player.yaw, 90, 10, true, () -> {
            int preSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.inventory.selectedSlot = preSlot;

            placedWater = setPlacedWater;
        });
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }
}
