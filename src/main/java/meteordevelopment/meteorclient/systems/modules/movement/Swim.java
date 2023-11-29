/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityPose;

public class Swim extends Module {
    public Swim(){
         super(Categories.Movement, "swim", "Automatically sets player to the new swimming pose");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.isTouchingWater() && !mc.player.isInSwimmingPose()) mc.player.setPose(EntityPose.SWIMMING);
    }
}
