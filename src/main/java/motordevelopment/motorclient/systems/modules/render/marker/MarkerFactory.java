/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.render.marker;

import motordevelopment.motorclient.systems.modules.Modules;

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

        names = new String[factories.size()];
        int i = 0;
        for (String key : factories.keySet()) names[i++] = key;
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
