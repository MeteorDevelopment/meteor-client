/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.tabs;

import motordevelopment.motorclient.gui.tabs.builtin.*;
import motordevelopment.motorclient.pathing.PathManagers;
import motordevelopment.motorclient.utils.PreInit;

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
