/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.impl.addons;

import org.jetbrains.annotations.NotNull;
import org.meteordev.meteor.api.addons.Addon;
import org.meteordev.meteor.api.addons.AddonManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AddonManagerImpl implements AddonManager {
    public static final AddonManagerImpl INSTANCE = new AddonManagerImpl();

    private final Map<String, Addon> addons = new HashMap<>();

    private AddonManagerImpl() {}

    @Override
    public void add(Addon addon) {
        addons.put(addon.getId(), addon);
    }

    @Override
    public Addon get(String id) {
        return addons.get(id);
    }

    @NotNull
    @Override
    public Iterator<Addon> iterator() {
        return addons.values().iterator();
    }
}
