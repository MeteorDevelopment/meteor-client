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
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public class AutoWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The maximum distance to target players.")
        .defaultValue(4)
        .range(0, 5)
        .sliderMax(5)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("doubles")
        .description("Places webs in the target's upper hitbox as well as the lower hitbox.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the webs when placing.")
        .defaultValue(true)
        .build()
    );

    private PlayerEntity target = null;

    public AutoWeb() {
        super(Categories.Combat, "auto-web", "Automatically places webs on other players.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, range.get())) {
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        }
        if (TargetUtils.isBadTarget(target, range.get())) return;

        BlockUtils.place(target.getBlockPos(), InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 0, false);

        if (doubles.get()) {
            BlockUtils.place(target.getBlockPos().add(0, 1, 0), InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 0, false);
        }
    }
}
