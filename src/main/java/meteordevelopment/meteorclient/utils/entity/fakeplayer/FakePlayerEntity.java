/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.mixin.AbstractClientPlayerAccessor;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakePlayerEntity extends RemotePlayer {
    /**
     * Disables entity push with this fake player
     */
    public boolean doNotPush;
    /**
     * Stops rendering the fake player when you are inside it
     */
    public boolean hideWhenInsideCamera;
    /**
     * Prevents you from interacting with the fake player; will also prevent TargetUtils selecting it as a target
     */
    public boolean noHit;

    public FakePlayerEntity(RemotePlayer player, String name, float health, boolean copyInv) {
        super(mc.world, new GameProfile(UUID.randomUUID(), name));

        copyPositionAndRotation(player);

        lastYaw = getYaw();
        lastPitch = getPitch();
        headYaw = player.headYaw;
        lastHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        lastBodyYaw = bodyYaw;

        getAttributes().setFrom(player.getAttributes());
        setPose(player.getPose());

        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }

        if (copyInv) getInventory().clone(player.getInventory());
    }

    public void spawn() {
        unsetRemoved();
        mc.world.addEntity(this);
    }

    public void despawn() {
        mc.world.removeEntity(getId(), RemovalReason.DISCARDED);
        setRemoved(RemovalReason.DISCARDED);
    }

    @Nullable
    @Override
    protected PlayerInfo getPlayerListEntry() {
        PlayerInfo entry = super.getPlayerListEntry();

        if (entry == null) {
            ((AbstractClientPlayerAccessor) this).meteor$setPlayerInfo(mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()));
        }

        return entry;
    }
}
