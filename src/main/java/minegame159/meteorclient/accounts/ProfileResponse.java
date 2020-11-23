/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts;

import java.util.List;
import java.util.Map;

public class ProfileResponse {
    public List<Map<String, String>> properties;

    public String getTextures() {
        for (Map<String, String> map : properties) {
            if (map.get("name").equals("textures")) return map.get("value");
        }

        return null;
    }
}
