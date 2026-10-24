/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.ICancellable;
import org.jspecify.annotations.Nullable;

/**
 * Disables and reports a module that throws from an event handler instead of crashing the game.
 * Exceptions that don't come from a module are rethrown as usual.
 */
public class MeteorEventBus extends EventBus {
    @Override
    public <T> T post(T event) {
        try {
            return super.post(event);
        } catch (RuntimeException e) {
            if (!disableCrashedModule(e)) throw e;
            return event;
        }
    }

    @Override
    public <T extends ICancellable> T post(T event) {
        try {
            return super.post(event);
        } catch (RuntimeException e) {
            if (!disableCrashedModule(e)) throw e;
            return event;
        }
    }

    private static boolean disableCrashedModule(RuntimeException e) {
        Module module = findModule(e);
        if (module == null) return false;

        MeteorClient.LOG.error("Module {} threw an exception and was disabled", module.name, e);

        if (module.isActive()) {
            try {
                module.toggle();
            } catch (RuntimeException deactivateException) {
                MeteorClient.LOG.error("Module {} also threw while being disabled", module.name, deactivateException);
            }
        }

        ChatUtils.errorPrefix("Meteor", "(highlight)%s(default) crashed and was disabled, check the log for details.", module.title);
        return true;
    }

    private static @Nullable Module findModule(Throwable e) {
        Modules modules = Modules.get();
        if (modules == null) return null;

        for (StackTraceElement frame : e.getStackTrace()) {
            String className = frame.getClassName();

            for (Module module : modules.getAll()) {
                String moduleClass = module.getClass().getName();
                if (className.equals(moduleClass) || className.startsWith(moduleClass + "$")) return module;
            }
        }

        return null;
    }
}
