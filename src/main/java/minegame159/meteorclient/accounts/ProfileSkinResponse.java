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
