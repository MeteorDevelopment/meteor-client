/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor;

import org.meteordev.meteor.api.addons.FabricAddon;

public class TestAddon extends FabricAddon {
    public static final TestAddon INSTANCE = new TestAddon();

    private TestAddon() {
        super("meteor");
    }
}
