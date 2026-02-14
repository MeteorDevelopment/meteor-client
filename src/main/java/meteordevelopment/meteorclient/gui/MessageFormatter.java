/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.MessageKind;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

public interface MessageFormatter {
    Text formatPlayerName(PlayerEntity player);
    Text formatEntityName(Entity entity);

    Text formatCoords(Vec3i blockPos);
    Text formatCoords(Vec3d pos);

    Text formatHighlight(Text text);
    Text formatDecimal(double decimal);

    Text formatPrefix(Text prefix);
    Text formatToggleFeedback(Text clientPrefix, Text featurePrefix, Module module, boolean enabled);
    Text formatMessage(Text clientPrefix, Optional<Text> featurePrefix, Text message, MessageKind messageKind);
}
