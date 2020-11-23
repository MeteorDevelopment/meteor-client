/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public interface IDisconnectedScreen {
    Screen getParent();

    Text getReason();

    int getReasonHeight();
}
