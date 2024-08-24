/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Capes {
    private static final String CAPE_OWNERS_URL = "https://meteorclient.com/api/capeowners";
    private static final String CAPES_URL = "https://meteorclient.com/api/capes";

    private static final Map<UUID, String> OWNERS = new HashMap<>();
    private static final Map<String, String> URLS = new HashMap<>();
    private static final Map<String, Cape> TEXTURES = new HashMap<>();

    private static final List<Cape> TO_REGISTER = new ArrayList<>();
    private static final List<Cape> TO_RETRY = new ArrayList<>();
    private static final List<Cape> TO_REMOVE = new ArrayList<>();

    private Capes() {
    }

    @PreInit(dependencies = MeteorExecutor.class)
    public static void init() {
        OWNERS.clear();
        URLS.clear();
        TEXTURES.clear();
        TO_REGISTER.clear();
        TO_RETRY.clear();
        TO_REMOVE.clear();

        MeteorExecutor.execute(() -> {
            // Cape owners
            Stream<String> lines = Http.get(CAPE_OWNERS_URL)
                .exceptionHandler(e -> MeteorClient.LOG.error("Could not load capes: " + e.getMessage()))
                .sendLines();
            if (lines != null) {
                lines.forEach(s -> {
                    String[] split = s.split(" ");

                    if (split.length >= 2) {
                        OWNERS.put(UUID.fromString(split[0]), split[1]);
                        if (!TEXTURES.containsKey(split[1])) TEXTURES.put(split[1], new Cape(split[1]));
                    }
                });
            } else return;

            // Capes
            lines = Http.get(CAPES_URL).sendLines();
            if (lines != null) lines.forEach(s -> {
                String[] split = s.split(" ");

                if (split.length >= 2) {
                    if (!URLS.containsKey(split[0])) URLS.put(split[0], split[1]);
                }
            });
        });

        MeteorClient.EVENT_BUS.subscribe(Capes.class);
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        synchronized (TO_REGISTER) {
            for (Cape cape : TO_REGISTER) cape.register();
            TO_REGISTER.clear();
        }

        synchronized (TO_RETRY) {
            TO_RETRY.removeIf(Cape::tick);
        }

        synchronized (TO_REMOVE) {
            for (Cape cape : TO_REMOVE) {
                URLS.remove(cape.name);
                TEXTURES.remove(cape.name);
                TO_REGISTER.remove(cape);
                TO_RETRY.remove(cape);
            }

            TO_REMOVE.clear();
        }
    }

    public static Identifier get(PlayerEntity player) {
        String capeName = OWNERS.get(player.getUuid());
        if (capeName != null) {
            Cape cape = TEXTURES.get(capeName);
            if (cape == null) return null;

            if (cape.isDownloaded()) return cape.getIdentifier();

            cape.download();
            return null;
        }

        return null;
    }

    private static class Cape {
        private static int COUNT = 0;

        private final String name;
        private final Identifier identifier;

        private boolean downloaded;
        private boolean downloading;

        private NativeImage img;

        private int retryTimer;

        public Cape(String name) {
            this.identifier = MeteorClient.identifier("capes/" + COUNT++);
            this.name = name;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public void download() {
            if (downloaded || downloading || retryTimer > 0) return;
            downloading = true;

            MeteorExecutor.execute(() -> {
                try {
                    String url = URLS.get(name);
                    if (url == null) {
                        synchronized (TO_REMOVE) {
                            TO_REMOVE.add(this);
                            downloading = false;
                            return;
                        }
                    }

                    InputStream in = Http.get(url).sendInputStream();
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
        }

        public void register() {
            mc.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(img));
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
