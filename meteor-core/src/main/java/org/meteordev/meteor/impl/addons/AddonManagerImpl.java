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

    private final Map<String, Addon> addonsId = new HashMap<>();
    private final Map<Class<? extends Addon>, Addon> addonsClass = new HashMap<>();

    private AddonManagerImpl() {}

    @Override
    public void add(Addon addon) {
        addonsId.put(addon.getId(), addon);
        addonsClass.put(addon.getClass(), addon);
    }

    @Override
    public Addon get(String id) {
        return addonsId.get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Addon> T get(Class<T> klass) {
        return (T) addonsClass.get(klass);
    }

    @NotNull
    @Override
    public Iterator<Addon> iterator() {
        return addonsId.values().iterator();
    }
}
