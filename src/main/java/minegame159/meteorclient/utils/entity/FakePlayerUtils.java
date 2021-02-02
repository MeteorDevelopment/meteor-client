package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.player.FakePlayer;

import java.util.HashMap;
import java.util.Map;

public class FakePlayerUtils {
    private static final Map<FakePlayerEntity, Integer> players = new HashMap<>();
    public static int ID;

    public static void spawnFakePlayer() {
        FakePlayer module = Modules.get().get(FakePlayer.class);

        if (module.isActive()) {
            FakePlayerEntity fakePlayer = new FakePlayerEntity(module.name.get(), module.copyInv.get(), module.glowing.get(), module.health.get());
            players.put(fakePlayer, ID);
            ID++;
        }
    }
    
    public static void removeFakePlayer(int id) {
        if (Modules.get().isActive(FakePlayer.class)) {
            if (players.isEmpty()) return;

            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getValue() == id) {
                    player.getKey().despawn();
                }
            }
        }
    }

    public static void clearFakePlayers() {
        for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
            player.getKey().despawn();
        }
        players.clear();
    }

    public static Map<FakePlayerEntity, Integer> getPlayers() {
        return players;
    }

    public static int getID(FakePlayerEntity entity) {
        int id = 0;

        if (!players.isEmpty()) {
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getKey() == entity) id = player.getValue();
            }
        }

        return id;
    }
}
