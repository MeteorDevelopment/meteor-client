/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

/**
 * Tells how "cheaty" a module is, which decides whether it exists on a server.
 *
 * <p>{@link #LEGIT} and {@link #BORDERLINE} modules are always available, in singleplayer and on servers.
 * {@link #CHEAT} modules are hidden and disabled on servers until every player on the server has agreed to them
 * by typing the unlock keyword in chat, see {@link CheatGate}.
 */
public enum ModuleTier {
    /**
     * Purely client side. Only changes what you see or how the client behaves, gives no gameplay advantage.
     */
    LEGIT("Legit", "Client side only, no advantage."),
    /**
     * Gives an advantage, but only through information the server already sends or through automating things you
     * could do by hand. Invisible enough to keep on servers.
     */
    BORDERLINE("Borderline", "Advantage through information or automation you could do by hand."),
    /**
     * Changes what you do in the world beyond vanilla limits. Only available in singleplayer, or on servers that
     * unlocked cheats through chat.
     */
    CHEAT("Cheat", "Changes gameplay beyond vanilla limits.");

    public final String title;
    public final String description;

    ModuleTier(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
