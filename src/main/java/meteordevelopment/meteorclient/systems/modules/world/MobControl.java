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
import meteordevelopment.meteorclient.settings.Vector3dSetting;
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
import org.joml.Vector3d;

import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Controls what the mobs around you are allowed to do.
 *
 * <p>The AI of a mob runs on the server, so a client cannot switch it off. What a client can do is
 * change what the server knows and what the server sees:
 *
 * <ul>
 *     <li>Not attacking a mob, so it never has a reason to fight back.</li>
 *     <li>Reporting a position that is not the real one, so the mob AI looks for the player in
 *     the wrong place and loses the line of sight.</li>
 *     <li>Freezing the mobs on the screen, by not applying what the server sends.</li>
 * </ul>
 *
 * <p>Every switch says which of the two sides it affects.
 */
public class MobControl extends Module {
    /** The action of {@link ClientboundAnimatePacket} that swings the main or the off hand. */
    private static final int SWING_MAIN_HAND = 0;
    private static final int SWING_OFF_HAND = 1;

    private final SettingGroup sgMobs = settings.createGroup("Mobs");
    private final SettingGroup sgClient = settings.createGroup("Client Side");
    private final SettingGroup sgServer = settings.createGroup("Server Side");

    private final Setting<Set<EntityType<?>>> entities = sgMobs.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("The mob types that are controlled.")
        .defaultValue(monsters())
        .build()
    );

    private final Setting<Boolean> noAi = sgClient.add(new BoolSetting.Builder()
        .name("no-ai")
        .description("The mobs stop acting: their animation, their head movement and the interpolation of their movement stop. The server keeps controlling them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMove = sgClient.add(new BoolSetting.Builder()
        .name("no-move")
        .description("The mobs do not move on your screen. The server still moves them, this only keeps them in place.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSwing = sgClient.add(new BoolSetting.Builder()
        .name("no-swing")
        .description("The mobs do not show their attack animation.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noAttack = sgServer.add(new BoolSetting.Builder()
        .name("no-attack")
        .description("You do not attack the mobs, so they are not hurt and never fight back. The server never sees the attack.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fakePosition = sgServer.add(new BoolSetting.Builder()
        .name("fake-position")
        .description("Reports a position that is not the real one, so the mob AI looks for you in the wrong place. The server also believes you are there, so do not pick a spot inside a block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Vector3d> fakeOffset = sgServer.add(new Vector3dSetting.Builder()
        .name("fake-offset")
        .description("The offset the server sees you at.")
        .defaultValue(0, 0, 0)
        .range(-64, 64)
        .sliderRange(-64, 64)
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

    /** Whether the mobs should stop acting on the client. */
    public boolean noAi() {
        return isActive() && noAi.get();
    }

    /** Whether the position that is reported to the server should be faked. */
    public boolean fakePosition() {
        return isActive() && fakePosition.get();
    }

    /** The offset the server should see the player at. */
    public Vector3d fakeOffset() {
        return fakeOffset.get();
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
