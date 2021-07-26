/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Reach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
            .name("reach")
            .description("Your reach modifier.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    
    private final Setting<Boolean> reachBlocks = sgGeneral.add(new BoolSetting.Builder()
    		.name("reach-blocks")
    		.description("Apply reach to block reach distance.")
    		.defaultValue(true)
    		.build()
    );

    public Reach() {
        super(Categories.Player, "reach", "Gives you super long arms.");
    }

    public float getReach() {
        if (isActive() && reachBlocks.get().booleanValue()) {
        	return reach.get().floatValue();
        }
        return mc.interactionManager.getCurrentGameMode().isCreative() ? 5.0F : 4.5F;
    }
}
