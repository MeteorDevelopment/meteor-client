package minegame159.meteorclient.altsfriends;

import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.SaveManager;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class FriendManager implements Listenable {
    public static FriendManager INSTANCE;

    private List<String> friends = new ArrayList<>();

    public void add(String friend) {
        friends.add(friend.trim());
        MeteorClient.eventBus.post(EventStore.friendListChangedEvent());
        SaveManager.save(getClass());
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
        SaveManager.save(getClass());
    }
    public void remove(PlayerEntity player) {
        remove(player.getGameProfile().getName());
    }
}
