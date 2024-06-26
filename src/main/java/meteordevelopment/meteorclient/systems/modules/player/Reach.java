/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.attribute.EntityAttributes;

public class Reach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> blockReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("block-reach")
        .description("The reach modifier for blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> entityReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("entity-reach")
        .description("The reach modifier for entities.")
        .defaultValue(3)
        .min(0)
        .sliderMax(6)
        .build()
    );

    public Reach() {
        super(Categories.Player, "reach", "Gives you super long arms.");
    }

    public double blockReach() {
        if (!isActive()) return mc.player.getAttributeValue(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE);
        return blockReach.get().floatValue();
    }

    public double entityReach() {
        if (!isActive()) return mc.player.getAttributeValue(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
        return entityReach.get().floatValue();
    }
}
