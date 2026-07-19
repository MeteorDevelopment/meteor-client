/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

/** Posted the moment the client learns why it's about to disconnect, before {@link GameLeftEvent} fires. */
public class DisconnectReasonEvent {
    public final String reason;

    public DisconnectReasonEvent(String reason) {
        this.reason = reason == null ? "" : reason;
    }
}
