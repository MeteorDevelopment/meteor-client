/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import baritone.api.BaritoneAPI;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import meteordevelopment.meteorclient.utils.PreInit;

import java.lang.reflect.Field;

public class BaritoneUtils {
    private static Field targetField;
    private static boolean active = false;

    @PreInit
    public static void preInit() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().registerProcess(new IBaritoneProcess() {
            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public PathingCommand onTick(boolean b, boolean b1) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }

            @Override
            public boolean isTemporary() {
                return true;
            }

            @Override
            public void onLostControl() {
            }

            @Override
            public double priority() {
                return 0d;
            }

            @Override
            public String displayName0() {
                return "Meteor Client";
            }
        });
    }

    public static void pause() {
        active = true;
    }

    public static void resume() {
        active = false;
    }

    public static boolean paused() {
        return active;
    }

    public static void stop() {
        active = false;
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static Rotation getTarget() {
        findField();
        if (targetField == null) return null;

        targetField.setAccessible(true);

        try {
            return (Rotation) targetField.get(BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void findField() {
        if (targetField != null) return;

        Class<?> klass = BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior().getClass();

        for (Field field : klass.getDeclaredFields()) {
            if (field.getType() == Rotation.class) {
                targetField = field;
                break;
            }
        }
    }
}
