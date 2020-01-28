package minegame159.meteorclient.mixininterface;

public interface IMinecraftClient {
    public void leftClick();

    public void rightClick();

    public int getCurrentFps();

    public void setItemUseCooldown(int cooldown);
}
