/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.text.Text;

public interface IChatHud {
    void add(Text message, int messageId, int timestamp, boolean refresh);
}
