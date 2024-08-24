/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.tabs.builtin.*;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PreInit;

import java.util.ArrayList;
import java.util.List;

public class Tabs {
    private static final List<Tab> tabs = new ArrayList<>();

    private Tabs() {
    }

    @PreInit(dependencies = PathManagers.class)
    public static void init() {
        add(new ModulesTab());
        add(new ConfigTab());
        add(new GuiTab());
        add(new HudTab());
        add(new FriendsTab());
        add(new MacrosTab());
        add(new ProfilesTab());

        if (PathManagers.get().getSettings().get().sizeGroups() > 0) {
            add(new PathManagerTab());
        }
    }

    public static void add(Tab tab) {
        tabs.add(tab);
    }

    public static List<Tab> get() {
        return tabs;
    }
}
