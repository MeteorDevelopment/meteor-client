/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    public boolean doNotPush, hideWhenInsideCamera, Pop, allowDamage, swapToTotem;
    public float chosenHealth;

    public int ticks;

    public FakePlayerEntity(PlayerEntity player, String name, float health, boolean copyInv, boolean allowDamage) {
        super(mc.world, new GameProfile(UUID.randomUUID(), name));

        copyPositionAndRotation(player);

        prevYaw = getYaw();
        prevPitch = getPitch();
        headYaw = player.headYaw;
        prevHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        prevBodyYaw = bodyYaw;
        this.allowDamage = allowDamage;
        chosenHealth = health;
        ticks = 0;

        Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);

        getAttributes().setFrom(player.getAttributes());
        setPose(player.getPose());

        capeX = getX();
        capeY = getY();
        capeZ = getZ();

        this.setHealth(health);

        if (copyInv) getInventory().clone(player.getInventory());
    }

    // Removes health from FakePlayer and checks if Fake Player pops a totem.

    private void removeHealth(float Damage) {
        float health = this.getHealth() - Damage;

        if (health < .5) {
            health = chosenHealth;
            Pop = true;
        }

        ticks = 0;
        this.setHealth(health);
        this.animateDamage(this.getYaw());
    }

    // Gets Explosion Damage
    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        Packet<?> eventPacket = event.packet;
        if (eventPacket instanceof ExplosionS2CPacket packet) {
            if (!allowDamage) return;
            if (ticks < 10) return;
            float crystalDamage = (float) DamageUtils.crystalDamage(this, new Vec3d(packet.getX(), packet.getY(), packet.getZ()));
            removeHealth(crystalDamage);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ticks++;

        // Gets Sword Damage
        if ((mc.options.attackKey.isPressed() && mc.targetedEntity == this && mc.player.handSwinging)) {
            if (ticks < 10) return;
            if (!allowDamage) return;

            if (canCrit()) {
                mc.particleManager.addEmitter(this, ParticleTypes.CRIT);
                mc.world.playSound(mc.player, this.getBlockPos(), SoundEvent.of(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT.getId()), SoundCategory.PLAYERS, 1.0F, 1.0F);
            } else {
                mc.world.playSound(mc.player, this.getBlockPos(), SoundEvent.of(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG.getId()), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }

            removeHealth((float) DamageUtils.getSwordDamage(this, canCrit()));
            ticks = 0;
        }
        this.tick();

        // Sets FakePlayer Offhand Item to a Totem if Damage Option is enabled
        if (allowDamage) {
            if (swapToTotem && ticks < 3) return;
            swapToTotem = false;
            this.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
        } else {
            this.setStackInHand(Hand.OFF_HAND, Items.AIR.getDefaultStack());
            return;
        }

        if (!Pop) return;

        afterPop(this);

        // Plays a Totem Pop sound and adds Totem Pop effects
        mc.world.playSound(mc.player, this.getBlockPos(), SoundEvent.of(SoundEvents.ITEM_TOTEM_USE.getId()), SoundCategory.PLAYERS, 1.0F, 1.0F);
        mc.particleManager.addEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
        Pop = false;
    }

    // What happens after Fake Player pops a totem.
    private void afterPop(PlayerEntity player) {
        if (player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            player.setStackInHand(Hand.MAIN_HAND, Items.AIR.getDefaultStack());
            return;
        }

        if (player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING && player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING)
            return;
        player.setStackInHand(Hand.OFF_HAND, Items.AIR.getDefaultStack());
        swapToTotem = true;
    }

    // Checks if Player can Crit
    private boolean canCrit() {
        return mc.player.getAttackCooldownProgress(0.5f) > 0.9f && mc.player.fallDistance > 0.0F && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isSubmergedInWater() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.isSprinting();
    }

    public void spawn() {
        unsetRemoved();
        mc.world.addEntity(this);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void despawn() {
        mc.world.removeEntity(getId(), RemovalReason.DISCARDED);
        setRemoved(RemovalReason.DISCARDED);
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    @Nullable
    @Override
    protected PlayerListEntry getPlayerListEntry() {
        if (playerListEntry == null) {
            playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        }

        return playerListEntry;
    }
}
