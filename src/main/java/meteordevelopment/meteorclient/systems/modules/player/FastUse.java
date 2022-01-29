/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FastUse extends Module {
    public enum Mode {
        All,
        Some
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which items to fast use.")
        .defaultValue(Mode.All)
        .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Which items should fast place work on in \"Some\" mode.")
        .visible(() -> mode.get() == Mode.Some)
        .build()
    );

    private final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Fast-places blocks if the mode is \"Some\" mode.")
        .visible(() -> mode.get() == Mode.Some)
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Fast-use cooldown in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(4)
        .build()
    );

    public FastUse() {
        super(Categories.Player, "fast-use", "Allows you to use items at very high speeds.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        int cooldownTicks = Math.min(((MinecraftClientAccessor) mc).getItemUseCooldown(), cooldown.get());
        if (mode.get() == Mode.All || shouldWorkSome()) ((MinecraftClientAccessor) mc).setItemUseCooldown(cooldownTicks);
    }

    private boolean shouldWorkSome() {
        if (shouldWorkSome(mc.player.getMainHandStack())) return true;
        return shouldWorkSome(mc.player.getOffHandStack());
    }

    private boolean shouldWorkSome(ItemStack itemStack) {
        return (blocks.get() && itemStack.getItem() instanceof BlockItem) || items.get().contains(itemStack.getItem());
    }
}
