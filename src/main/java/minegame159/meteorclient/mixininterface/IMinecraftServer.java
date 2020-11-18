package minegame159.meteorclient.mixininterface;

import net.minecraft.world.level.storage.LevelStorage;

public interface IMinecraftServer {
    LevelStorage.Session getSession();
}
