/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixin.HeldItemRendererAccessor;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class HandView extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> mainhandY = sgGeneral.add(new DoubleSetting.Builder()
            .name("mainhand-y")
            .description("The hand progress, or Y level of your main hand.")
            .defaultValue(0.8)
            .min(0.3)
            .max(3.4)
            .build()
    );

    private final Setting<Double> offhandY = sgGeneral.add(new DoubleSetting.Builder()
            .name("offhand-y")
            .description("The hand progress, or Y level of your offhand.")
            .defaultValue(0.8)
            .min(0.3)
            .max(3.4)
            .build()
    );

    public HandView() {
        super(Category.Render, "hand-view", "Changes the way items are rendered in your hands.");
    }

    HeldItemRendererAccessor itemRenderer = (HeldItemRendererAccessor) mc.gameRenderer.firstPersonRenderer;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        itemRenderer.setItemStackMainHand(mc.player.getMainHandStack());
        itemRenderer.setItemStackOffHand(mc.player.getOffHandStack());
        itemRenderer.setEquippedProgressMainHand(mainhandY.get().floatValue());
        itemRenderer.setEquippedProgressOffHand(offhandY.get().floatValue());
    });
}