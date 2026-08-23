/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

public enum CursorStyle {
    Default(CursorType.DEFAULT),
    Click(CursorTypes.POINTING_HAND),
    Type(CursorTypes.IBEAM);

    private final CursorType cursor;

    CursorStyle(CursorType cursor) {
        this.cursor = cursor;
    }

    public CursorType getCursor() {
        return cursor;
    }
}
