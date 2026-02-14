/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

public enum MessageKind {
    Passthrough(null),
    Info("info"),
    Warning("warning"),
    Error("error");

    public String key;

    MessageKind(String key) {
        this.key = key;
    }
}
