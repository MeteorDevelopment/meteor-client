/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class AntiBreak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
        .name("anti-break-percentage")
        .description("The durability percentage to stop using a tool.")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> preventMine = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-mine")
        .description("Prevents mining if the selected tool is low durability.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preventHit = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-hit")
        .description("Prevents hitting if the selected tool is low durability.")
        .defaultValue(true)
        .build()
    );

    public AntiBreak() {
        super(Categories.Player, "anti-break", "Prevents you from using items which are about to break.");
    }

    public boolean canUse(ItemStack itemStack) {
        return !itemStack.isDamageable() || itemStack.getMaxDamage() - itemStack.getDamage() >= itemStack.getMaxDamage() * breakDurability.get() / 100;
    }

    @EventHandler(priority = EventPriority.HIGH - 10) //after autotool
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!preventMine.get() || Modules.get().isActive(InfinityMiner.class)) return;

        BlockState state = mc.world.getBlockState(event.blockPos);
        if (state.getHardness(mc.world, event.blockPos) == 0) return; //breaks instantly
        if (!BlockUtils.canBreak(event.blockPos, state)) return; //can't break

        if (!canUse(mc.player.getMainHandStack())) {
            mc.options.attackKey.setPressed(false);
            event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (!preventHit.get() || !event.entity.isAttackable() || !(event.entity instanceof LivingEntity)) return;

        if (!canUse(mc.player.getMainHandStack())) {
            mc.options.attackKey.setPressed(false);
            event.cancel();
        }
    }
}
