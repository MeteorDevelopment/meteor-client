/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.MessageFormatter;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MessageBuilderImpl implements MessageBuilder {
    private final GuiTheme theme = GuiThemes.get();
    private final MessageFormatter formatter = this.theme.messageFormatter();

    private int id = 0;
    private @Nullable MessageKind kind;
    private @Nullable Object source;
    private @Nullable Text messagePrefix;
    private @Nullable Text messageBodyText;
    private @Nullable String messageBody;
    private Object[] args;

    private @Nullable String translationContext;

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
    public MessageBuilder setTranslationContext(String translationContext) {
        assertOpen();
        this.translationContext = translationContext;
        return this;
    }

    @Override
    public MessageBuilder setSource(Object source) {
        assertOpen();
        this.source = source;
        return this;
    }

    @Override
    public MessageBuilder prefix(Text prefix) {
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
    public MessageBuilder body(Text body) {
        assertOpen();
        this.messageBodyText = body;
        return this;
    }

    @Override
    public MessageBuilder body(String body, Object... args) {
        assertOpen();
        processArgs(args);
        this.messageBody = body;
        this.args = args;
        return this;
    }

    /* Terminal Functions */

    @Override
    public Text build() {
        assertOpen();
        closed = true;

        if (this.messageBody == null && this.messageBodyText == null) {
            throw new IllegalArgumentException("Message body cannot be empty!");
        } else if (this.kind == null) {
            throw new IllegalArgumentException("Message cannot have a kind!");
        }

        Text bodyText = this.messageBodyText != null ? this.messageBodyText : this.createMessageBody(this.messageBody, this.kind);

        return this.formatter.formatMessage(
            this.formatter.formatPrefix(ChatUtils.getPrefix(this.source, this.theme)),
            Optional.ofNullable(this.messagePrefix).map(this.formatter::formatPrefix),
            bodyText,
            this.kind
        );
    }

    @Override
    public void send() {
        Text message = this.build();
        int messageId = Config.get().deleteChatFeedback.get() ? this.id : 0;

        ChatUtils.sendMsg(messageId, message);
    }

    /* Internal Functions */

    public static Text highlight(Object arg) {
        MessageFormatter formatter = GuiThemes.get().messageFormatter();
        Text processed = processArg(formatter, arg);
        return formatter.formatHighlight(processed);
    }

    private void processArgs(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i] = processArg(this.formatter, args[i]);
        }
    }

    private static Text processArg(MessageFormatter formatter, Object arg) {
        return switch (arg) {
            // theme-dependent formatting
            case PlayerEntity player -> formatter.formatPlayerName(player);
            case Entity entity -> formatter.formatEntityName(entity);
            case Vec3i vec -> formatter.formatCoords(vec);
            case Vec3d vec -> formatter.formatCoords(vec);
            case Float f -> formatter.formatDecimal(f);
            case Double d -> formatter.formatDecimal(d);

            // accept common objects as parameters
            case StatusEffect statusEffect -> stripStyle(statusEffect.getName());
            case Item item -> stripStyle(item.getName());
            case Block block -> stripStyle(block.getName());
            case EntityType<?> type -> stripStyle(type.getName());

            case Text text -> text;
            default -> Text.literal(String.valueOf(arg));
        };
    }

    private static Text stripStyle(Text text) {
        if (text instanceof MutableText mutable) {
            mutable.setStyle(Style.EMPTY);
            for (Text sibling : mutable.getSiblings()) {
                stripStyle(sibling);
            }
        }
        return text;
    }

    private MutableText createMessageBody(String messageBody, MessageKind kind) {
        MeteorTranslations.MeteorLanguage language = MeteorTranslations.getCurrentLanguage();

        if (language.hasTranslation(messageBody)) {
            return MutableText.of(new MeteorTranslatableTextContent(
                messageBody, null, this.args
            ));
        } else if (this.translationContext != null && kind != MessageKind.Passthrough) {
            String computedTranslationKey = this.translationContext + "." + kind.key + "." + messageBody;
            if (language.hasTranslation(computedTranslationKey)) {
                return MutableText.of(new MeteorTranslatableTextContent(
                    computedTranslationKey, null, this.args
                ));
            }
        }

        if (this.args.length == 0) {
            return Text.literal(messageBody);
        } else {
            return MutableText.of(new RichPlainTextContent(messageBody, this.args));
        }
    }

    private void assertOpen() {
        if (this.closed) {
            throw new IllegalStateException("Cannot use MessageBuilder after building message.");
        }
    }
}
