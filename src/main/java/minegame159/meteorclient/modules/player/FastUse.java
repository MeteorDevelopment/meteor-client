/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;

public class FastUse extends ToggleModule {

    public enum Mode {
        All,
        OnlySome
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which items to fastplace.")
            .defaultValue(Mode.All)
            .build()
    );

    private final Setting<Boolean> exp = sgGeneral.add(new BoolSetting.Builder()
            .name("xp")
            .description("Fast places XP bottles if Mode is \"OnlySome\".")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
            .name("blocks")
            .description("Fast places blocks if Mode is \"OnlySome\".")
            .defaultValue(false)
            .build()
    );

    public FastUse() {
        super(Category.Player, "fast-use", "Fast item use.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        switch (mode.get()) {
            case All:
                ((IMinecraftClient) mc).setItemUseCooldown(0);
                break;
            case OnlySome:
                if (exp.get() && (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE)) ((IMinecraftClient) mc).setItemUseCooldown(0);
                if (blocks.get() && mc.player.getMainHandStack().getItem() instanceof BlockItem || mc.player.getOffHandStack().getItem() instanceof BlockItem) ((IMinecraftClient) mc).setItemUseCooldown(0);
        }
    });
}
