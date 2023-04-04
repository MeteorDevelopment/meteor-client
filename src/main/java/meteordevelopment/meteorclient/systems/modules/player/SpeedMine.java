/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.StatusEffectInstanceAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.List;

import static net.minecraft.entity.effect.StatusEffects.HASTE;

public class SpeedMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Selected blocks.")
            .filter(block -> block.getHardness() > 0)
            .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the blocks setting.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Normal)
            .build()
    );
    public final Setting<Double> modifier = sgGeneral.add(new DoubleSetting.Builder()
            .name("modifier")
            .description("Mining speed modifier. An additional value of 0.2 is equivalent to one haste level (1.2 = haste 1).")
            .defaultValue(1.4)
            .min(0)
            .build()
    );

    public SpeedMine() {
        super(Categories.Player, "speed-mine", "Allows you to quickly mine blocks.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mode.get() == Mode.Normal) return;

        int amplifier = mode.get() == Mode.Haste2 ? 1 : 0;

        if (!mc.player.hasStatusEffect(HASTE)) {
            mc.player.addStatusEffect(new StatusEffectInstance(HASTE, 255, amplifier, false, false, false));
        }

        StatusEffectInstance effect = mc.player.getStatusEffect(HASTE);
        ((StatusEffectInstanceAccessor) effect).setAmplifier(amplifier);
        if (effect.getDuration() < 20) ((StatusEffectInstanceAccessor) effect).setDuration(20);
    }

    public boolean filter(Block block) {
        if (blocksFilter.get() == ListMode.Blacklist && !blocks.get().contains(block)) return true;
        else if (blocksFilter.get() == ListMode.Whitelist && blocks.get().contains(block)) return true;

        return false;
    }

    public enum Mode {
        Normal,
        Haste1,
        Haste2
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
