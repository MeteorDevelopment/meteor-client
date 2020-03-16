package minegame159.meteorclient.altsfriends;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.SaveManager;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class AccountManager implements Listenable {
    public static AccountManager INSTANCE;

    static YggdrasilUserAuthentication userAuthentication;

    private List<Account> accounts = new ArrayList<>();

    public void add(Account account) {
        if (account.credentials == null) return;

        if (!accounts.contains(account)) {
            accounts.add(account);
            SaveManager.save(getClass());
            MeteorClient.eventBus.post(EventStore.accountListChangedEvent());
        }
    }

    public void remove(Account account) {
        if (accounts.remove(account)) {
            SaveManager.save(getClass());
            MeteorClient.eventBus.post(EventStore.accountListChangedEvent());
        }
    }

    public List<Account> getAll() {
        return accounts;
    }

    private void onLoad() {
        for (Account account : accounts) {
            account.username = (String) account.credentials.get("displayName");
            account.email = (String) account.credentials.get("username");
        }
    }

    public static void init() {
        userAuthentication = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(((IMinecraftClient) MinecraftClient.getInstance()).getProxy(), "48c1eb24-b218-4e50-844e-0a34975441da").createUserAuthentication(Agent.MINECRAFT);
    }
}
