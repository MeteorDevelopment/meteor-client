package minegame159.meteorclient.mixininterface;

import net.minecraft.client.util.Session;

import java.net.Proxy;

public interface IMinecraftClient {
    public void leftClick();

    public void rightClick();

    public void setItemUseCooldown(int cooldown);

    public Proxy getProxy();

    public void setSession(Session session);
}
