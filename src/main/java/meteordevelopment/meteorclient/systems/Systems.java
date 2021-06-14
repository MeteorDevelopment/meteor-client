/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Systems {
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends System>, System<?>> systems = new HashMap<>();

    private static final List<Runnable> preLoadTasks = new ArrayList<>(1);
    private static System<?> config;

    public static void init() {
        config = add(new Config());
        config.load();
        config.init();

        add(new Modules());
        add(new Commands());
        add(new Friends());
        add(new Macros());
        add(new Accounts());
        add(new Waypoints());
        add(new Profiles());
        add(new Proxies());

        for (System<?> system : systems.values()) {
            if (system != config) system.init();
        }
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);

        return system;
    }

    public static void save(File folder) {
        MeteorClient.LOG.info("Saving");
        long start = java.lang.System.currentTimeMillis();

        for (System<?> system : systems.values()) system.save(folder);

        MeteorClient.LOG.info("Saved in {} milliseconds.", java.lang.System.currentTimeMillis() - start);
    }

    public static void save() {
        save(null);
    }

    public static void addPreLoadTask(Runnable task) {
        preLoadTasks.add(task);
    }

    public static void load(File folder) {
        MeteorClient.LOG.info("Loading");
        long start = java.lang.System.currentTimeMillis();

        for (Runnable task : preLoadTasks) task.run();

        for (System<?> system : systems.values()) {
            if (system != config) system.load(folder);
        }

        MeteorClient.LOG.info("Loaded in {} milliseconds", java.lang.System.currentTimeMillis() - start);
    }

    public static void load() {
        load(null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends System<?>> T get(Class<T> klass) {
        return (T) systems.get(klass);
    }
}
