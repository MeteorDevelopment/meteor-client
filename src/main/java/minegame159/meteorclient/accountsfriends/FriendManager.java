package minegame159.meteorclient.accountsfriends;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FriendManager extends Savable<FriendManager> {
    public static final FriendManager INSTANCE = new FriendManager();

    private List<String> friends = new ArrayList<>();

    private FriendManager() {
        super(new File(MeteorClient.FOLDER, "friends.nbt"));
    }

    public void add(String friend) {
        friends.add(friend.trim());
        MeteorClient.EVENT_BUS.post(EventStore.friendListChangedEvent());
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
        MeteorClient.EVENT_BUS.post(EventStore.friendListChangedEvent());
        save();
    }
    public void remove(PlayerEntity player) {
        remove(player.getGameProfile().getName());
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag friendsTag = new ListTag();
        for (String friend : friends) friendsTag.add(new StringTag(friend));
        tag.put("friends", friendsTag);

        return tag;
    }

    @Override
    public FriendManager fromTag(CompoundTag tag) {
        friends = NbtUtils.listFromTag(tag.getList("friends", 8), Tag::asString);

        return this;
    }
}
