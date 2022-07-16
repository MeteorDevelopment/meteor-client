/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.tabs.builtin.*;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tabs {
    private static final List<Tab> tabs = new ArrayList<>();
    private static final Map<Class<? extends Tab>, Tab> tabInstances = new HashMap<>();

    @Init(stage = InitStage.Pre)
    public static void init() {
        add(new ModulesTab());
        add(new ConfigTab());
        add(new GuiTab());
        add(new HudTab());
        add(new FriendsTab());
        add(new MacrosTab());
        add(new ProfilesTab());
        add(new BaritoneTab());
        add(new AccountTab());
    }

    public static void add(Tab tab) {
        tabs.add(tab);
        tabInstances.put(tab.getClass(), tab);
    }

    public static List<Tab> get() {
        return tabs;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tab> T get(Class<T> klass) {
        return (T) tabInstances.get(klass);
    }
}
