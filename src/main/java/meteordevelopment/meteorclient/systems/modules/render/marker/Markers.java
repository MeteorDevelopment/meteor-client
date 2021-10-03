/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Markers {
    private interface Factory {
        BaseMarker create();
    }

    private final Map<String, Factory> factories;

    public Markers() {
        factories = new HashMap<>();
        factories.put(CuboidMarker.type, CuboidMarker::new);
        factories.put(Sphere2dMarker.type, Sphere2dMarker::new);
    }

    public List<String> getNames() {
        return factories.keySet().stream().toList();
    }

    public BaseMarker createMarker(String name) {
        if (factories.containsKey(name)) {
            return factories.get(name).create();
        }
        return null;
    }
}
