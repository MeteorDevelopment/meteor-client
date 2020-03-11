package minegame159.meteorclient.altsfriends;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.entity.player.PlayerEntity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FriendManager {
    public static FriendManager INSTANCE;
    private static final File file = new File(MeteorClient.directory, "friends.json");

    private List<String> friends = new ArrayList<>();

    public void add(String friend) {
        friends.add(friend.trim());
        MeteorClient.eventBus.post(EventStore.friendListChangedEvent());
        save();
    }
    public void add(PlayerEntity player) {
        add(player.getGameProfile().getName());
    }

    public List<String> getAll() {
        return friends;
    }

    public boolean contains(String friend) {
        return friends.contains(friend.trim());
    }
    public boolean contains(PlayerEntity player) {
        return contains(player.getGameProfile().getName());
    }

    public void addOrRemove(String friend) {
        if (friends.contains(friend)) remove(friend);
        else add(friend);
    }
    public void addOrRemove(PlayerEntity player) {
        addOrRemove(player.getGameProfile().getName());
    }

    public void remove(String friend) {
        friends.remove(friend.trim());
        MeteorClient.eventBus.post(EventStore.friendListChangedEvent());
        save();
    }
    public void remove(PlayerEntity player) {
        remove(player.getGameProfile().getName());
    }

    public static void save() {
        try {
            Writer writer = new FileWriter(file);
            MeteorClient.gson.toJson(INSTANCE, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!file.exists()) {
            if (INSTANCE == null) INSTANCE = new FriendManager();
            return;
        }

        try {
            FileReader reader = new FileReader(file);
            INSTANCE = MeteorClient.gson.fromJson(reader, FriendManager.class);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
