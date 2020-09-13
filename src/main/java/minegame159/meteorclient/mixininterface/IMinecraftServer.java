package minegame159.meteorclient.mixininterface;

import net.minecraft.world.level.storage.LevelStorage;

public interface IMinecraftServer {
    public LevelStorage.Session getSession();
}
