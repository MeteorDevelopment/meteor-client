/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart = settings.createGroup("Smart");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Determines when to hold a totem, strict will always hold.")
            .defaultValue(Mode.Smart)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The ticks between slot movements.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build()
    );

    // Smart settings

    private final Setting<Integer> health = sgSmart.add(new IntSetting.Builder()
            .name("health")
            .description("The health to hold a totem at.")
            .defaultValue(10)
            .min(0)
            .sliderMax(36)
            .max(36)
            .build()
    );

    private final Setting<Boolean> elytra = sgSmart.add(new BoolSetting.Builder()
            .name("elytra")
            .description("Will always hold a totem when flying with elytra.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fall = sgSmart.add(new BoolSetting.Builder()
            .name("fall")
            .description("Will hold a totem when fall damage could kill you.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> explosion = sgSmart.add(new BoolSetting.Builder()
            .name("explosion")
            .description("Will hold a totem when explosion damage could kill you.")
            .defaultValue(true)
            .build()
    );

    public boolean locked;
    private int totems, ticks;

    public AutoTotem() {
        super(Categories.Combat, "auto-totem", "Automatically equips a totem in your offhand.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);
        totems = result.count;

        if (totems <= 0) locked = false;
        else if (ticks >= delay.get()) {
            boolean low = mc.player.getHealth() + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions(explosion.get(), fall.get()) <= health.get();
            boolean ely = elytra.get() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.player.isFallFlying();

            locked = mode.get() == Mode.Strict || (mode.get() == Mode.Smart && (low || ely));

            if (locked && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(result.slot).toOffhand();
            }

            ticks = 0;
            return;
        }

        ticks++;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        if (p.getEntity(mc.world).equals(mc.player)) ticks = 0;
    }

    public boolean isLocked() {
        return isActive() && locked;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }

    public enum Mode {
        Smart,
        Strict
    }
}
