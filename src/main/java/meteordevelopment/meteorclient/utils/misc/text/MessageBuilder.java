/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface MessageBuilder {
    MessageBuilder setId(int id);
    MessageBuilder setKind(MessageKind kind);
    MessageBuilder overrideClientPrefix(Class<?> holder);

    MessageBuilder prefix(MutableText prefix);
    MessageBuilder prefix(String prefix);
    MessageBuilder prefix(String prefix, Formatting prefixColor);

    MessageBuilder body(MutableText body);
    MessageBuilder body(String body, Object... args);
    MessageBuilder content(String translationKey, Object... args);

    Text build();
    void send();
}
