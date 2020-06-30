package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Capes {
    private static final String CAPE_OWNERS_URL = "https://raw.githubusercontent.com/MeteorClient/meteorclient.github.io/master/capes/owners.txt";
    private static final String CAPE_FOLDER_URL = "https://raw.githubusercontent.com/MeteorClient/meteorclient.github.io/master/";

    private static final Identifier EMPTY_CAPE = new Identifier("meteor-client", "empty_cape.png");

    private static final Map<UUID, String> OWNERS = new HashMap<>();
    private static final Map<String, Cape> TEXTURES = new HashMap<>();

    private static final List<Cape> TO_REGISTER = new ArrayList<>();
    private static final List<Cape> TO_RETRY = new ArrayList<>();

    public static void init() {
        MeteorTaskExecutor.start();
        MeteorTaskExecutor.execute(() -> HttpUtils.getLines(CAPE_OWNERS_URL, s -> {
            String[] split = s.split(" ");
            if (split.length >= 2) {
                OWNERS.put(UUID.fromString(split[0]), split[1]);
                if (!TEXTURES.containsKey(split[1])) TEXTURES.put(split[1], new Cape(split[1]));
            }
        }));
        MeteorTaskExecutor.stop();
    }

    public static Identifier getCape(PlayerEntity player) {
        String capeName = OWNERS.get(player.getUuid());
        if (capeName != null) {
            Cape cape = TEXTURES.get(capeName);
            if (cape.isDownloaded()) return cape;

            cape.download();
            return EMPTY_CAPE;
        }

        return null;
    }

    public static void tick() {
        synchronized (TO_REGISTER) {
            for (Cape cape : TO_REGISTER) cape.register();
            TO_REGISTER.clear();
        }

        synchronized (TO_RETRY) {
            TO_RETRY.removeIf(Cape::tick);
        }
    }

    private static class Cape extends Identifier {
        private boolean downloaded;
        private boolean downloading;

        private NativeImage img;

        private int retryTimer;

        public Cape(String name) {
            super("meteor-client", "capes/" + name + ".png");
        }

        public void download() {
            if (downloaded || downloading || retryTimer > 0) return;
            downloading = true;

            MeteorTaskExecutor.start();
            MeteorTaskExecutor.execute(() -> {
                try {
                    InputStream in = HttpUtils.get(CAPE_FOLDER_URL + path);
                    if (in == null) {
                        synchronized (TO_RETRY) {
                            TO_RETRY.add(this);
                            retryTimer = 10 * 20;
                            downloading = false;
                            return;
                        }
                    }

                    img = NativeImage.read(in);

                    synchronized (TO_REGISTER) {
                        TO_REGISTER.add(this);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            MeteorTaskExecutor.stop();
        }

        public void register() {
            MinecraftClient.getInstance().getTextureManager().registerTexture(this, new NativeImageBackedTexture(img));
            img = null;

            downloading = false;
            downloaded = true;
        }

        public boolean tick() {
            if (retryTimer > 0) {
                retryTimer--;
            } else {
                download();
                return true;
            }

            return false;
        }

        public boolean isDownloaded() {
            return downloaded;
        }
    }
}
