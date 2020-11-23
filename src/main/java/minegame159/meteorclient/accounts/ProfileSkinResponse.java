/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts;

public class ProfileSkinResponse {
    public Textures textures;

    public static class Textures {
        public Texture SKIN;
        public Texture CAPE;
    }

    public static class Texture {
        public String url;
    }
}
