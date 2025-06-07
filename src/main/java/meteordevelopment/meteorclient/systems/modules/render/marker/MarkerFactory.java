/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.HashMap;
import java.util.Map;

public class MarkerFactory {
    private interface Factory {
        BaseMarker create();
    }

    private final Map<String, Factory> factories;
    private final String[] names;

    public MarkerFactory() {
        factories = new HashMap<>();
        factories.put(CuboidMarker.type, CuboidMarker::new);
        factories.put(Sphere2dMarker.type, Sphere2dMarker::new);
        factories.put(Sphere3dMarker.type, Sphere3dMarker::new);
        factories.put(CylinderMarker.type, CylinderMarker::new);

        names = factories.keySet().toArray(new String[0]);
    }

    public String[] getNames() {
        return names;
    }

    public BaseMarker createMarker(String name) {
        if (factories.containsKey(name)) {
            BaseMarker marker = factories.get(name).create();
            marker.settings.registerColorSettings(Modules.get().get(Marker.class));

            return marker;
        }

        return null;
    }
}
