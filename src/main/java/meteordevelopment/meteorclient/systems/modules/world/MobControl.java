/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Controls what the mobs around you are allowed to do.
 *
 * <p>The server decides what a mob does, so most switches only change what the client does with
 * the information the server sends. The exception is <i>no attack</i>: the attack is never sent, so
 * the mob is not hurt and never fights back.
 */
public class MobControl extends Module {
    /** The action of {@link ClientboundAnimatePacket} that swings the main or the off hand. */
    private static final int SWING_MAIN_HAND = 0;
    private static final int SWING_OFF_HAND = 1;

    private final SettingGroup sgMobs = settings.createGroup("Mobs");
    private final SettingGroup sgActions = settings.createGroup("Actions");

    private final Setting<Set<EntityType<?>>> entities = sgMobs.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("The mob types that are controlled.")
        .defaultValue(monsters())
        .build()
    );

    private final Setting<Boolean> noMove = sgActions.add(new BoolSetting.Builder()
        .name("no-move")
        .description("The mobs do not move. The server still moves them, this only keeps them in place on your screen.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSwing = sgActions.add(new BoolSetting.Builder()
        .name("no-swing")
        .description("The mobs do not show their attack animation.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noAttack = sgActions.add(new BoolSetting.Builder()
        .name("no-attack")
        .description("You do not attack the mobs, so they never fight back. The server never sees the attack.")
        .defaultValue(false)
        .build()
    );

    public MobControl() {
        super(Categories.World, "mob-control", "Controls what the mobs around you are allowed to do.");
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundMoveEntityPacket packet) {
            if (noMove.get() && isControlled(packet.getEntity(mc.level))) event.cancel();
        }
        else if (event.packet instanceof ClientboundTeleportEntityPacket packet) {
            if (noMove.get() && isControlled(entity(packet.id()))) event.cancel();
        }
        else if (event.packet instanceof ClientboundAnimatePacket packet) {
            if (noSwing.get() && isSwing(packet.getAction()) && isControlled(entity(packet.getId()))) event.cancel();
        }
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundAttackPacket packet) {
            if (noAttack.get() && isControlled(entity(packet.entityId()))) event.cancel();
        }
    }

    /** Whether the entity is one of the mobs that are controlled. */
    public boolean isControlled(Entity entity) {
        if (!isActive() || entity == null || entity == mc.player) return false;

        return entities.get().contains(entity.getType());
    }

    private Entity entity(int id) {
        return mc.level == null ? null : mc.level.getEntity(id);
    }

    private static boolean isSwing(int action) {
        return action == SWING_MAIN_HAND || action == SWING_OFF_HAND;
    }

    private static EntityType<?>[] monsters() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(type -> type.getCategory() == MobCategory.MONSTER)
            .toArray(EntityType[]::new);
    }
}
