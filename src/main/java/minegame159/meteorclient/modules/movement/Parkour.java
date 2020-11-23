/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.stream.Stream;

public class Parkour extends ToggleModule {
    public Parkour() {
        super(Category.Movement, "parkour", "Automatically jumps at the edges of blocks.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if(!mc.player.isOnGround() || mc.options.keyJump.isPressed()) return;

        if(mc.player.isSneaking() || mc.options.keySneak.isPressed()) return;

        Box box = mc.player.getBoundingBox();
        Box adjustedBox = box.offset(0, -0.5, 0).expand(-0.001, 0, -0.001);

        Stream<VoxelShape> blockCollisions = mc.world.getBlockCollisions(mc.player, adjustedBox);

        if(blockCollisions.findAny().isPresent()) return;

        mc.player.jump();
    });
}
