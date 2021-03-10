package minegame159.meteorclient.utils.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptifineCapes {
    private static final String CAPE_URL = "http://s.optifine.net/capes/%s.png";

    private static final Map<String, OptifineCape> CAPES = new HashMap<>();
    private static final List<String> NO_CAPES = new ArrayList<>();

    private static final List<OptifineCape> TO_REGISTER = new ArrayList<>();
    private static final List<OptifineCape> TO_RETRY = new ArrayList<>();
    private static final List<OptifineCape> TO_REMOVE = new ArrayList<>();

    public static Identifier get(PlayerEntity player) {
        String username;
        if ((username = player.getGameProfile().getName()) == null
                || !username.matches("(?i)[a-z0-9_]{3,16}") // get sure username is a vanilla one
                || NO_CAPES.contains(username)
                || player.isFallFlying()
                || player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return null;

        if (!CAPES.containsKey(username)) {
            CAPES.put(username, new OptifineCape(username));
        }

        OptifineCape cape = CAPES.get(username);

        if (cape.isDownloaded()) {
            return cape;
        }

        cape.download();
        return null;
    }

    public static void tick() {
        synchronized (TO_REGISTER) {
            for (OptifineCape cape : TO_REGISTER) cape.register();
            TO_REGISTER.clear();
        }

        synchronized (TO_RETRY) {
            TO_RETRY.removeIf(OptifineCape::tick);
        }

        synchronized (TO_REMOVE) {
            for (OptifineCape cape : TO_REMOVE) {
                CAPES.remove(cape.username);
                TO_REGISTER.remove(cape);
                TO_RETRY.remove(cape);
            }

            TO_REMOVE.clear();
        }
    }

    private static class OptifineCape extends Identifier {
        private final String username;
        private final String url;

        private boolean downloaded;
        private boolean downloading;

        private NativeImage img;

        private int retryTimer;

        public OptifineCape(String username) {
            super("meteor-client", "optifine_capes/" + username.toLowerCase());

            this.username = username;
            this.url = String.format(CAPE_URL, username);
        }

        public void download() {
            if (downloaded || downloading || retryTimer > 0) {
                return;
            }
            downloading = true;

            MeteorExecutor.execute(() -> {
                try {
                    if (url == null) {
                        synchronized (TO_RETRY) {
                            TO_REMOVE.add(this);
                            downloading = false;
                            return;
                        }
                    }

                    HttpURLConnection conn = HttpUtils.connect("GET", url, null);

                    int res = -1;
                    if (conn == null || (res = conn.getResponseCode()) != 200) {
                        // some error occurred
                        if (res == 404) {
                            // player does not have a optifine cape
                            synchronized (TO_RETRY) {
                                TO_REMOVE.add(this);
                                downloading = false;
                            }
                            synchronized (NO_CAPES) {
                                NO_CAPES.add(username);
                            }
                        } else {
                            // a other error occurred
                            synchronized (TO_RETRY) {
                                TO_RETRY.add(this);
                                retryTimer = 10 * 20;
                                downloading = false;
                            }
                        }
                        return;
                    }
                    InputStream in = conn.getInputStream();

                    img = parseCape(NativeImage.read(in));

                    synchronized (TO_REGISTER) {
                        TO_REGISTER.add(this);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private NativeImage parseCape(NativeImage img) {
            int i = 64;
            int j = 32;
            int k = img.getWidth();

            for (int l = img.getHeight(); i < k || j < l; j *= 2) {
                i *= 2;
            }

            NativeImage nativeimage = new NativeImage(i, j, true);
            nativeimage.copyFrom(img);
            img.close();
            return nativeimage;
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
