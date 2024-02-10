package me.jellysquid.mods.lithium.common.compat.worldedit;

import net.fabricmc.loader.api.FabricLoader;

public class WorldEditCompat {

    public static final boolean WORLD_EDIT_PRESENT = FabricLoader.getInstance().isModLoaded("worldedit");

}
