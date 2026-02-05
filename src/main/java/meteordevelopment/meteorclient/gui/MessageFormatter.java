/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.utils.misc.text.MessageKind;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public interface MessageFormatter {
    Text formatPlayerName(PlayerEntity player);
    Text formatEntityName(Entity entity);

    Text formatCoords(Vec3i blockPos);
    Text formatCoords(Vec3d pos);

    Text formatHighlight(MutableText text);
    Text formatDecimal(double decimal);

    Text formatPrefix(Text prefix);
    Text formatMessage(MutableText message, MessageKind messageKind);
}
