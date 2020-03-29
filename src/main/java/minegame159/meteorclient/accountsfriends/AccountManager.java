package minegame159.meteorclient.accountsfriends;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AccountManager extends Savable<AccountManager> {
    public static final AccountManager INSTANCE = new AccountManager();

    static YggdrasilUserAuthentication userAuthentication;

    private List<Account> accounts = new ArrayList<>();

    private AccountManager() {
        super(new File(MeteorClient.FOLDER, "accounts.nbt"));
    }

    public void add(Account account) {
        if (!accounts.contains(account)) {
            accounts.add(account);
            save();
            MeteorClient.EVENT_BUS.post(EventStore.accountListChangedEvent());
        }
    }

    public void remove(Account account) {
        if (accounts.remove(account)) {
            save();
            MeteorClient.EVENT_BUS.post(EventStore.accountListChangedEvent());
        }
    }

    public List<Account> getAll() {
        return accounts;
    }

    public static void init() {
        userAuthentication = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(((IMinecraftClient) MinecraftClient.getInstance()).getProxy(), "48c1eb24-b218-4e50-844e-0a34975441da").createUserAuthentication(Agent.MINECRAFT);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("accounts", NbtUtils.listToTag(accounts));
        return tag;
    }

    @Override
    public AccountManager fromTag(CompoundTag tag) {
        accounts = NbtUtils.listFromTag(tag.getList("accounts", 10), tag1 -> new Account().fromTag((CompoundTag) tag1));

        for (int i = 0; i < accounts.size(); i++) {
            if (!accounts.get(i).isValid()) {
                accounts.remove(i);
                i--;
            }
        }
        return this;
    }
}
