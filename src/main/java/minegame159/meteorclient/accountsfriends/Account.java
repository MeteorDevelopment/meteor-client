package minegame159.meteorclient.accountsfriends;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.utils.ISerializable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import net.minecraft.nbt.CompoundTag;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

public class Account implements ISerializable<Account> {
    private String email;
    private String username;
    private String password;

    Account() {}

    public Account(String email, String password) {
        this.email = email;
        this.password = AESEncryption.encrypt(password, "Lps98faSD6ASD8fe");
    }

    public boolean logIn() {
        try {
            if (AccountManager.userAuthentication.isLoggedIn()) AccountManager.userAuthentication.logOut();

            AccountManager.userAuthentication.setUsername(email);
            AccountManager.userAuthentication.setPassword(AESEncryption.decrypt(this.password, "Lps98faSD6ASD8fe"));

            AccountManager.userAuthentication.logIn();
            GameProfile profile = AccountManager.userAuthentication.getSelectedProfile();
            ((IMinecraftClient) MinecraftClient.getInstance()).setSession(new Session(profile.getName(), profile.getId().toString(), AccountManager.userAuthentication.getAuthenticatedToken(), AccountManager.userAuthentication.getUserType().getName()));

            username = AccountManager.userAuthentication.getSelectedProfile().getName();
            AccountManager.INSTANCE.save();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return (username == null || username.isEmpty()) ? email : username;
    }

    public boolean isValid() {
        return email != null && password != null;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("email", email);
        if (username != null) tag.putString("username", username);
        tag.putString("password", password);

        return tag;
    }

    @Override
    public Account fromTag(CompoundTag tag) {
        email = tag.getString("email");
        if (tag.contains("username")) username = tag.getString("username");
        password = tag.getString("password");

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(email, account.email) &&
                Objects.equals(password, account.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }
}
