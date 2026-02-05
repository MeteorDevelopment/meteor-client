/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.MessageFormatter;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MessageBuilderImpl implements MessageBuilder {
    private final GuiTheme theme = GuiThemes.get();
    private final MessageFormatter formatter = this.theme.messageFormatter();

    private int id = 0;
    private MessageKind kind;
    private @Nullable Class<?> topLevelPrefix;
    private @Nullable Text messagePrefix;
    private @Nullable MutableText messageBody;
    private Object[] args;

    private boolean hasStyledArgs = false;
    private boolean closed = false;

    /* Builder functions */

    @Override
    public MessageBuilder setId(int id) {
        assertOpen();
        this.id = id;
        return this;
    }

    @Override
    public MessageBuilder setKind(MessageKind kind) {
        assertOpen();
        this.kind = kind;
        return this;
    }

    @Override
    public MessageBuilder overrideClientPrefix(Class<?> holder) {
        assertOpen();
        this.topLevelPrefix = holder;
        return this;
    }

    @Override
    public MessageBuilder prefix(MutableText prefix) {
        assertOpen();
        this.messagePrefix = prefix;
        return this;
    }

    @Override
    public MessageBuilder prefix(String prefix) {
        assertOpen();
        this.messagePrefix = Text.literal(prefix);
        return this;
    }

    @Override
    public MessageBuilder prefix(String prefix, Formatting prefixColor) {
        assertOpen();
        this.messagePrefix = Text.literal(prefix).formatted(prefixColor);
        return this;
    }

    @Override
    public MessageBuilder body(MutableText body) {
        assertOpen();
        this.messageBody = body;
        return this;
    }

    @Override
    public MessageBuilder body(String body, Object... args) {
        assertOpen();
        processArgs(args);
        this.messageBody = Text.literal(String.format(body, args));
        return this;
    }

    @Override
    public MessageBuilder content(String translationKey, Object... args) {
        assertOpen();
        processArgs(args);
        this.messageBody = MutableText.of(new MeteorTranslatableTextContent(
            translationKey, null, args, this.hasStyledArgs
        ));
        return this;
    }

    /* Terminal Functions */

    @Override
    public Text build() {
        closed = true;
        if (this.messageBody == null) {
            throw new IllegalArgumentException("Message body cannot be empty!");
        } else if (this.kind == null) {
            throw new IllegalArgumentException("Message cannot have a kind!");
        }

        MutableText message = Text.empty()
            .append(this.formatter.formatPrefix(this.getPrefix()));

        if (this.messagePrefix != null) {
            message.append(this.formatter.formatPrefix(this.messagePrefix));
        }

        message.append(this.formatter.formatMessage(this.messageBody, this.kind));

        return message;
    }

    @Override
    public void send() {
        if (mc.world == null) return;

        Text message = this.build();
        int messageId = Config.get().deleteChatFeedback.get() ? this.id : 0;

        if (mc.isOnThread()) {
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, messageId);
        } else {
            mc.execute(() -> ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, messageId));
        }
    }

    /* Internal Functions */

    private void processArgs(Object[] args) {
        hasStyledArgs = false;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            args[i] = switch (arg) {
                // theme-dependent formatting
                case PlayerEntity player -> { hasStyledArgs = true; yield this.formatter.formatPlayerName(player); }
                case Entity entity -> { hasStyledArgs = true; yield this.formatter.formatEntityName(entity); }
                case Vec3i vec -> { hasStyledArgs = true; yield this.formatter.formatCoords(vec); }
                case Vec3d vec -> { hasStyledArgs = true; yield this.formatter.formatCoords(vec); }
                case Float f -> { hasStyledArgs = true; yield this.formatter.formatDecimal(f); }
                case Double d -> { hasStyledArgs = true; yield this.formatter.formatDecimal(d); }

                // accept common objects as parameters
                case StatusEffect statusEffect -> stripStyle(statusEffect.getName());
                case Item item -> stripStyle(item.getName());
                case Block block -> stripStyle(block.getName());
                case EntityType<?> type -> stripStyle(type.getName());

                case StringVisitable stringVisitable -> { hasStyledArgs = true; yield stringVisitable; }
                default -> String.valueOf(arg);
            };
        }
    }

    private Text stripStyle(Text text) {
        if (text instanceof MutableText mutable) {
            mutable.setStyle(Style.EMPTY);
            for (Text sibling : mutable.getSiblings()) {
                stripStyle(sibling);
            }
        }
        return text;
    }

    private Text getPrefix() {
        List<Pair<String, Supplier<Text>>> customPrefixes = ChatUtils.getCustomPrefixes();
        if (customPrefixes.isEmpty()) {
            return this.theme.getChatPrefix();
        }

        String className = null;
        if (topLevelPrefix != null) {
            className = topLevelPrefix.getName();
        } else {
            boolean foundClass = false;
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (foundClass) {
                    if (!element.getClassName().equals(MessageBuilderImpl.class.getName())) {
                        className = element.getClassName();
                        break;
                    }
                } else {
                    if (element.getClassName().equals(MessageBuilderImpl.class.getName())) {
                        foundClass = true;
                    }
                }
            }
        }

        if (className == null) {
            return this.theme.getChatPrefix();
        }

        for (Pair<String, Supplier<Text>> pair : customPrefixes) {
            if (className.startsWith(pair.getLeft())) {
                @Nullable Text prefix = pair.getRight().get();
                return prefix != null ? prefix : this.theme.getChatPrefix();
            }
        }

        return this.theme.getChatPrefix();
    }

    private void assertOpen() {
        if (this.closed) {
            throw new IllegalStateException("Cannot use MessageBuilder after building message.");
        }
    }
}
