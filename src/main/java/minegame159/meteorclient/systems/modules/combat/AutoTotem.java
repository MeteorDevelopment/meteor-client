/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

public class AutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart = settings.createGroup("Smart");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Determines when to hold a totem, strict will always hold.")
            .defaultValue(Mode.Smart)
            .build()
    );

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
    private int totems;

    public AutoTotem() {
        super(Categories.Combat, "auto-totem", "Automatically equips a totem in your offhand.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.currentScreen != null && !(mc.currentScreen instanceof AbstractInventoryScreen)) return;

        InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);

        totems = result.count;

        if (totems <= 0) {
            locked = false;
            return;
        }

        boolean low = mc.player.getHealth() + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions(explosion.get(), fall.get()) <= health.get();
        boolean ely = elytra.get() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.player.isFallFlying();

        locked = mode.get() == Mode.Strict || (mode.get() == Mode.Smart && (low || ely));

        if (locked && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(result.slot).toOffhand();
        }
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
