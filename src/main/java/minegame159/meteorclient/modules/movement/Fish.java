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
import net.minecraft.*;




public class Fish extends Module {

    public Fish() {
        super(Categories.Movement, "fish", "Disables underwater gravity.");
    }



    if (mc.options.keySneak.isPressed()) {
        return;
    }

    mc.player.setVelocity(initialVelocity.add(0, 0.005, 0));


}