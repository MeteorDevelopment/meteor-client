/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerMixin;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @see ClientPlayNetworkHandlerMixin#onPlayerPositionLookHead(PlayerPositionLookS2CPacket, CallbackInfo, LocalFloatRef, LocalFloatRef)
 */
public class NoRotate extends Module {
    public NoRotate() {
        super(Categories.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }
}
