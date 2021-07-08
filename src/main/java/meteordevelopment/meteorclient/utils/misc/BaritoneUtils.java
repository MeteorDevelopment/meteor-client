/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.lang.reflect.Field;

public class BaritoneUtils {
    private static Field targetField;

    // TODO: Baritone
    /*public static Rotation getTarget() {
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
    }*/
}
