package minegame159.meteorclient.mixininterface;

import net.minecraft.client.util.Session;

import java.net.Proxy;

public interface IMinecraftClient {
    void leftClick();

    void rightClick();

    void setItemUseCooldown(int cooldown);

    Proxy getProxy();

    void setSession(Session session);

    int getFps();
}
