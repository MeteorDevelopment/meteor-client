/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 *
 *
 * Source used:
 *
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 *
 *
 */


package minegame159.meteorclient.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;



public class Fish extends Module {

    public Fish() {
        super(Categories.Movement, "fish", "Disables underwater gravity.");
    }


    @EventHandler
    private void PlayerMoveEvent(PlayerMoveEvent event){
        ClientPlayerEntity player = MC.player;
        if(!(mc.player.isTouchingWater() || mc.options.keySneak.isPressed())())
            return;

        Vec3d velocity = player.getVelocity();
        mc.player.setVelocity = (velocity.x, velocity.y + 0.005, velocity.z);
    }

}