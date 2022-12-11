/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor;

import net.fabricmc.api.ClientModInitializer;
import org.meteordev.meteor.api.MeteorAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Meteor implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger(Meteor.class);

    @Override
    public void onInitializeClient() {
        LOG.info("Meteor API: v{}", MeteorAPI.getInstance().getVersion());
    }
}
