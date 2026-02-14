/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.MessageFormatter;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.MessageKind;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.text.DecimalFormat;
import java.util.Optional;

public class MeteorMessageFormatter implements MessageFormatter {
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0##");

    @Override
    public Text formatPlayerName(PlayerEntity player) {
        return player.getName();
    }

    @Override
    public Text formatEntityName(Entity entity) {
        return Text.literal(entity.getName().getString());
    }

    @Override
    public Text formatCoords(Vec3i blockPos) {
        return formatCoords(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public Text formatCoords(Vec3d pos) {
        return formatCoords((int) Math.round(pos.getX()), (int) Math.round(pos.getY()), (int) Math.round(pos.getZ()));
    }

    private Text formatCoords(int x, int y, int z) {
        Style style = Style.EMPTY.withFormatting(Formatting.WHITE).withUnderline(true);

        if (!(PathManagers.get() instanceof NopPathManager)) {
            style = style.withBold(true)
                .withHoverEvent(new HoverEvent.ShowText(
                    Text.literal("Set as pathing goal")
                ))
                .withClickEvent(new RunnableClickEvent(
                    () -> PathManagers.get().moveTo(new BlockPos(x, y, z))
                ));
        }

        return Text.literal(x + ", " + y + ", " + z).setStyle(style);
    }

    @Override
    public Text formatHighlight(Text text) {
        return Text.empty().formatted(Formatting.WHITE).append(text);
    }

    @Override
    public Text formatDecimal(double decimal) {
        return Text.literal(this.decimalFormat.format(decimal));
    }

    @Override
    public Text formatPrefix(Text prefix) {
        return Text.empty().formatted(Formatting.GRAY)
            .append("[")
            .append(prefix)
            .append("] ");
    }

    @Override
    public Text formatToggleFeedback(Text clientPrefix, Text featurePrefix, Module module, boolean enabled) {
        Text feedback = MeteorClient.translatable(
            "module.base.toggled",
            module.getTitleText(),
            enabled ? Text.literal("on").formatted(Formatting.GREEN) : Text.literal("off").formatted(Formatting.RED)
        );

        return this.formatMessage(clientPrefix, Optional.of(featurePrefix), feedback, MessageKind.Passthrough);
    }

    @Override
    public Text formatMessage(Text clientPrefix, Optional<Text> featurePrefix, Text message, MessageKind messageKind) {
        MutableText formattedMessage = Text.empty().append(clientPrefix);
        featurePrefix.ifPresent(formattedMessage::append);
        formattedMessage.append(message);

        return switch (messageKind) {
            case Passthrough -> formattedMessage;
            case Info -> formattedMessage.formatted(Formatting.GRAY);
            case Warning -> formattedMessage.formatted(Formatting.YELLOW);
            case Error -> formattedMessage.formatted(Formatting.RED);
        };
    }
}
