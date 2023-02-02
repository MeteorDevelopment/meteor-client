/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.effect.StatusEffect;

import java.util.List;

import static net.minecraft.entity.effect.StatusEffects.*;

public class AntiPotion extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<StatusEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("effects")
        .description("The effects to block.")
        .defaultValue(
            LEVITATION,
            JUMP_BOOST,
            SLOW_FALLING,
            DOLPHINS_GRACE
        )
        .build()
    );

    public final Setting<Boolean> applyGravity = sgGeneral.add(new BoolSetting.Builder()
        .name("gravity")
        .description("Applies gravity when levitating.")
        .defaultValue(false)
        .build()
    );

    public AntiPotion() {
        super(Categories.Player, "anti-potion", "Block some potion effects, but won't work for some effects like speed.");
    }

    public boolean shouldBlock(StatusEffect effect) {
        return isActive() && effects.get().contains(effect);
    }
}
