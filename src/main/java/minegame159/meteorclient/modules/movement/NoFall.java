/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

public class NoFall extends Module {
    public enum Mode{
        Packet,
        AirPlace
    }

    public enum PlaceMode{
        BeforeDeath,
        BeforeDamage
    }

    public NoFall() {
        super(Category.Movement, "no-fall", "Prevents you from taking fall damage.");
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

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player != null && mc.player.abilities.creativeMode) return;

        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (elytra.get() && (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.options.keyJump.isPressed() || mc.player.isFallFlying())) {
                for (int i = 0; i <= Math.ceil(height.get()); i++) {
                    if (!mc.world.getBlockState(mc.player.getBlockPos().add(0, -i, 0)).getMaterial().isReplaceable()) {
                        if (mc.player.getBlockPos().add(0, -i, 0).getY() + 1 + height.get() >= mc.player.getPos().getY()) {
                            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                            return;
                        }
                    }
                }
            } else if (mode.get() == Mode.Packet){
                if(((IPlayerMoveC2SPacket) event.packet).getTag() != 1337) {
                    ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
                }
            } else if ((placeMode.get() == PlaceMode.BeforeDamage && mc.player.fallDistance > 2)
                    || (placeMode.get() == PlaceMode.BeforeDeath && ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < mc.player.fallDistance))){
                int slot = -1;
                int preSlot;
                for (int i = 0; i < 9; i++){
                    if (mc.player.inventory.getStack(i).getItem() instanceof BlockItem){
                        slot = i;
                        break;
                    }
                }
                if (slot != -1){
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = slot;
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos().add(0, -1, 0), Direction.UP, mc.player.getBlockPos().down(), false));
                    mc.player.inventory.selectedSlot = preSlot;
                }
            }
        }
    }
}
