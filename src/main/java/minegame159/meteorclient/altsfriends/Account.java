package minegame159.meteorclient.altsfriends;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

import java.util.Map;
import java.util.Objects;

public class Account {
    public transient String email;
    public transient String username;

    Map<String, Object> credentials;

    public Account(String email, String password) {
        this.email = email.trim();

        try {
            if (AccountManager.userAuthentication.isLoggedIn()) AccountManager.userAuthentication.logOut();
            AccountManager.userAuthentication.setUsername(email);
            AccountManager.userAuthentication.setPassword(password.trim());
            AccountManager.userAuthentication.logIn();

            credentials = AccountManager.userAuthentication.saveForStorage();
            username = AccountManager.userAuthentication.getSelectedProfile().getName();
        } catch (AuthenticationException ignored) {
        }
    }

    public Account(Map<String, Object> credentials) {
        this.credentials = credentials;
        this.username = (String) credentials.get("displayName");
        this.email = (String) credentials.get("username");
    }

    public boolean logIn() {
        AccountManager.userAuthentication.loadFromStorage(credentials);

        try {
            AccountManager.userAuthentication.logIn();
            GameProfile profile = AccountManager.userAuthentication.getSelectedProfile();
            ((IMinecraftClient) MinecraftClient.getInstance()).setSession(new Session(profile.getName(), profile.getId().toString(), AccountManager.userAuthentication.getAuthenticatedToken(), AccountManager.userAuthentication.getUserType().getName()));

            username = AccountManager.userAuthentication.getSelectedProfile().getName();
            return true;
        } catch (AuthenticationException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(email, account.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
