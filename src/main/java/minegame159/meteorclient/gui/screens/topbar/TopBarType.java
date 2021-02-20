/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

public enum TopBarType {
    Modules(TopBarModules::new),
    Config(TopBarConfig::new),
    Gui(TopBarGui::new),
    Hud(TopBarHud::new, true),
    Friends(TopBarFriends::new),
    Macros(TopBarMacros::new),
    Baritone(TopBarBaritone::new),
    Waypoints(TopBarWaypoints::new);

    private interface TopBarScreenFactory {
        TopBarScreen create();
    }

    private final TopBarScreenFactory factory;
    public final boolean closeToParent;

    TopBarType(TopBarScreenFactory factory, boolean closeToParent) {
        this.factory = factory;
        this.closeToParent = closeToParent;
    }

    TopBarType(TopBarScreenFactory factory) {
        this(factory, false);
    }

    public TopBarScreen createScreen() {
        return factory.create();
    }
}
