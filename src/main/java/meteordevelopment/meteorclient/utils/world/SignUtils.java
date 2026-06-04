/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.misc.TranslationUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignUtils {
    private static final Map<Key, String[]> originalText = new HashMap<>();

    private SignUtils() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(SignUtils.class);
    }

    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        originalText.clear();
    }

    public static void cacheOriginalText(SignBlockEntity sign, boolean front) {
        Component[] messages = sign.getText(front).getMessages(false);
        String[] lines = new String[messages.length];
        boolean found = false;

        for (int i = 0; i < messages.length; i++) {
            lines[i] = TranslationUtils.getNamespaceTranslationFallback(MeteorClient.MOD_ID, messages[i]);
            if (lines[i] != null) found = true;
        }

        Key key = new Key(sign.getBlockPos(), front);
        if (found) originalText.put(key, lines);
        else originalText.remove(key);
    }

    public static ServerboundSignUpdatePacket replaceMeteorTranslations(ServerboundSignUpdatePacket packet) {
        String[] originals = originalText.remove(new Key(packet.getPos(), packet.isFrontText()));
        if (originals == null) return packet;

        String[] lines = Arrays.copyOf(packet.getLines(), packet.getLines().length);
        boolean changed = false;

        for (int i = 0; i < lines.length; i++) {
            if (originals[i] == null || !TranslationUtils.isNamespaceTranslation(MeteorClient.MOD_ID, lines[i])) continue;

            lines[i] = originals[i];
            changed = true;
        }

        return changed
            ? new ServerboundSignUpdatePacket(packet.getPos(), packet.isFrontText(), lines[0], lines[1], lines[2], lines[3])
            : packet;
    }

    private record Key(BlockPos pos, boolean front) {
    }
}
