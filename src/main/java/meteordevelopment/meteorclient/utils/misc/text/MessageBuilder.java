/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Supplier;

public interface MessageBuilder {
    static MessageBuilder create() {
        return new MessageBuilderImpl();
    }

    static Text highlight(Object argument) {
        return MessageBuilderImpl.highlight(argument);
    }

    /**
     * Sets the id associated with this message. There can only be a single message with a given id in chat.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder setId(int id);

    /**
     * Sets the kind of message you want to send, for use in styling. Use {@link MessageKind#Passthrough} to avoid
     * the formatter to send the message body as-is, with no additional styling.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder setKind(MessageKind kind);

    /**
     * Sets the context to be used for translation keys within this builder. If this is set, it will automatically
     * attempt to resolve translations using the key {@code "${context}.${kind}.${body}"}.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder setTranslationContext(String translationContext);

    /**
     * Sets the source of this builder. If set, the Meteor Client prefix will be replaced based on
     * {@link meteordevelopment.meteorclient.utils.player.ChatUtils#registerCustomPrefix(String, Supplier)}.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder setSource(Object source);

    /**
     * Sets the prefix to show in front of the message. This can optionally be styled, but should not contain any
     * decorations such as brackets as those will be added by the formatter.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder prefix(Text prefix);

    /**
     * Sets the prefix to show in front of the message. This should not contain any decorations such as brackets as
     * those will be added by the formatter.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder prefix(String prefix);

    /**
     * Sets the prefix to show in front of the message. This should not contain any decorations such as brackets as
     * those will be added by the formatter. The prefix color is a suggestion, the formatter is allowed to override it.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder prefix(String prefix, Formatting prefixColor);

    /**
     * Sets the message body.
     *
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder body(Text body);

    /**
     * Sets the message body.
     *
     * @param body the message body. If this is a valid Meteor Client translation key, the message body will instead be
     *             the translated text. If {@link MessageBuilder#setTranslationContext(String)} is set and
     *             {@link MessageBuilder#setKind(MessageKind)} is not set to {@link MessageKind#Passthrough}, the
     *             message body will also attempt to resolve the key made using the format
     *             {@code "${context}.${kind}.${body}"}. For example,
     *             {@code builder.setKind(MessageKind.Info).setTranslationContext("example").body("test")} would result
     *             in the key {@code "example.info.test"}. Otherwise, the message body will simply be passed in plain
     *             text.
     * @param args the arguments that will be used to replace the format specifiers in the body. This builder supports
     *             many special argument types: <ul>
     *             <li>{@link Text} — Keeps the styling.</li>
     *             <li>{@link net.minecraft.entity.player.PlayerEntity} — Uses the player's name.</li>
     *             <li>{@link net.minecraft.entity.Entity} — Uses the entity's display name.</li>
     *             <li>{@link net.minecraft.util.math.BlockPos} — Formatted coordinate display.</li>
     *             <li>{@link net.minecraft.util.math.Vec3d} — Formatted coordinate display.</li>
     *             <li>{@link Float} & {@link Double} — Truncates some decimals.</li>
     *             <li>{@link net.minecraft.entity.effect.StatusEffect} — Uses the status effect's display name.</li>
     *             <li>{@link net.minecraft.item.Item} — Uses the item's display name.</li>
     *             <li>{@link net.minecraft.block.Block} — Uses the block's display name.</li>
     *             <li>{@link net.minecraft.entity.EntityType} — Uses the entity type's display name.</li>
     *             </ul> Otherwise the argument will be passed through {@link String#valueOf(Object)}.
     * @return this builder
     * @throws IllegalStateException if this builder is already closed.
     */
    MessageBuilder body(String body, Object... args);

    /**
     * Builds the message and closes this builder.
     *
     * @return the built message
     * @throws IllegalArgumentException if the message has no body or no kind.
     * @throws IllegalStateException if this builder is already closed.
     */
    Text build();

    /**
     * Sends the message in chat and closes this builder.
     *
     * @throws IllegalArgumentException if the message has no body or no kind.
     * @throws IllegalStateException if this builder is already closed.
     */
    void send();
}
