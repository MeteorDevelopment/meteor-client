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

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.events.world.TickEvent;
import com.google.common.eventbus.Subscribe;
import net.minecraft.*;
import net.minecraft.entity.Entity;




public class Fish extends Module {

    // Decriptions for the ClickGUI
    public Fish() {
        super(Categories.Movement, "fish", "Disables underwater gravity.");
    }

    @Subscribe
    //On every single tick, check these things:
    private void onTick(TickEvent.Post event) {

        // If the sneak key is pressed, keep gravity because you want to go down.
        if (mc.options.keySneak.isPressed()) {
            return;
        }

        //If in water, set upwards velocity equal to downwards velocity, in order to stay stable.
        if (mc.player.isTouchingWater()) {
            entity.setVelocity(entity.getVelocity().x, 0.005, entity.getVelocity().z);
        }
    }


}