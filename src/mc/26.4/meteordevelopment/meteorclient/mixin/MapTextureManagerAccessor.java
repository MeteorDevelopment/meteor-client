/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.resources.MapTextureManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.4: {@code MapTextureManager.MapInstance} became a private nested class, so it cannot be
 * named in an accessor. The 26.4 SaveMapCommand rebuilds the map image from the saved data
 * instead of reading it back from the map texture, so it does not need the invoker either.
 */
@Mixin(MapTextureManager.class)
public interface MapTextureManagerAccessor {
}
