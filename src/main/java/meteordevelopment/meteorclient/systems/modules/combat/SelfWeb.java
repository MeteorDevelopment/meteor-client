/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class SelfWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode to use for selfweb.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("How far away the player has to be from you to place webs. Requires Mode to Smart.")
            .defaultValue(3)
            .min(1)
            .sliderRange(1, 7)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("double-place")
            .description("Places webs in your upper hitbox as well.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Toggles off after placing the webs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate downwards when placing webs.")
            .defaultValue(true)
            .build()
    );

    public SelfWeb() {
        super(Categories.Combat, "self-web", "Automatically places webs on you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switch (mode.get()) {
            case Normal:
                placeWeb();
                break;
            case Smart:
                if (TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance) != null) placeWeb();
                break;
        }
    }

    private void placeWeb() {
        FindItemResult web = InvUtils.findInHotbar(Items.COBWEB);

        BlockUtils.place(mc.player.getBlockPos(), web, rotate.get(), 0, false);

        if (doubles.get()) {
            BlockUtils.place(mc.player.getBlockPos().add(0, 1, 0), web, rotate.get(), 0, false);
        }

        if (turnOff.get()) toggle();
    }

    public enum Mode {
        Normal,
        Smart
    }
}
