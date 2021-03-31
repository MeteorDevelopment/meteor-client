/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.ParticleEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.ParticleTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.particle.ParticleType;

import java.util.ArrayList;
import java.util.List;

public class ParticleBlocker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<ParticleType<?>>> particles = sgGeneral.add(new ParticleTypeListSetting.Builder()
            .name("particles")
            .description("Particles to block.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    public ParticleBlocker() {
        super(Categories.Render, "particle-blocker", "Stops specified particles from rendering.");
    }

    @EventHandler
    private void onRenderParticle(ParticleEvent event) {
        if (event.particle != null && particles.get().contains(event.particle)) event.cancel();
    }
}
