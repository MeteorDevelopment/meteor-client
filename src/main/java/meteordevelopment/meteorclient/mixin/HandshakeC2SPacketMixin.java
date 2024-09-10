/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.google.common.base.Charsets;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BungeeCordSpoof;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HandshakeC2SPacket.class)
public class HandshakeC2SPacketMixin {
    @Mutable
    @Shadow
    @Final
    private String address;

    @Inject(method = "<init>(ILjava/lang/String;ILnet/minecraft/network/packet/c2s/handshake/ConnectionIntent;)V", at = @At("TAIL"))
    public void init(int protocolVersion, String address, int port, ConnectionIntent intendedState, CallbackInfo ci) {
        if (Modules.get().isActive(BungeeCordSpoof.class) && intendedState == ConnectionIntent.LOGIN) {
            BungeeCordSpoof bungeeCordSpoofModule = Modules.get().get(BungeeCordSpoof.class);

            // Obtain UUID to send
            String uuid;
            if (!bungeeCordSpoofModule.spoofedUuid.get().isEmpty()) {
                uuid = bungeeCordSpoofModule.spoofedUuid.get();
            } else {
                UUID currentUuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
                if (currentUuid == null) {
                    // Cracked account
                    currentUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + MinecraftClient.getInstance().getSession().getUsername()).getBytes(Charsets.UTF_8));
                }

                // UUID must be without dashes
                uuid = currentUuid.toString().replace("-", "");
            }

            // hostName \00 spoofed ip \00 spoofed uuid (\00 optional skin)
            this.address = address + "\00" + bungeeCordSpoofModule.spoofedIp.get() + "\00" + uuid;
        }
    }
}
