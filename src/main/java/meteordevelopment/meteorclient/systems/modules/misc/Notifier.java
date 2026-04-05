/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;

public class Notifier extends Module {
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgJoinsLeaves = settings.createGroup("Joins/Leaves");

    // Totem Pops

    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder()
        .name("totem-pops")
        .description("Notifies you when a player pops a totem.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> totemsDistanceCheck = sgTotemPops.add(new BoolSetting.Builder()
        .name("distance-check")
        .description("Limits the distance in which the pops are recognized.")
        .defaultValue(false)
        .visible(totemPops::get)
        .build()
    );

    private final Setting<Integer> totemsDistance = sgTotemPops.add(new IntSetting.Builder()
        .name("player-radius")
        .description("The radius in which to log totem pops.")
        .defaultValue(30)
        .sliderRange(1, 50)
        .range(1, 100)
        .visible(() -> totemPops.get() && totemsDistanceCheck.get())
        .build()
    );

    private final Setting<Boolean> totemsIgnoreOwn = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignores your own totem pops.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends totem pops.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreOthers = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-others")
        .description("Ignores other players totem pops.")
        .defaultValue(false)
        .build()
    );

    // Visual Range

    private final Setting<Boolean> visualRange = sgVisualRange.add(new BoolSetting.Builder()
        .name("visual-range")
        .description("Notifies you when an entity enters your render distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Event> event = sgVisualRange.add(new EnumSetting.Builder<Event>()
        .name("event")
        .description("When to log the entities.")
        .defaultValue(Event.Both)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to notify about.")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFriends = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFakes = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-fake-players")
        .description("Ignores fake players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> visualMakeSound = sgVisualRange.add(new BoolSetting.Builder()
        .name("sound")
        .description("Emits a sound effect on enter / leave")
        .defaultValue(true)
        .build()
    );

    // Pearl

    private final Setting<Boolean> pearl = sgPearl.add(new BoolSetting.Builder()
        .name("pearl")
        .description("Notifies you when a player is teleported using an ender pearl.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pearlIgnoreOwn = sgPearl.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignores your own pearls.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pearlIgnoreFriends = sgPearl.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends pearls.")
        .defaultValue(false)
        .build()
    );

    // Joins/Leaves

    private final Setting<JoinLeaveModes> joinsLeavesMode = sgJoinsLeaves.add(new EnumSetting.Builder<JoinLeaveModes>()
        .name("player-joins-leaves")
        .description("How to handle player join/leave notifications.")
        .defaultValue(JoinLeaveModes.None)
        .build()
    );

    private final Setting<Integer> notificationDelay = sgJoinsLeaves.add(new IntSetting.Builder()
        .name("notification-delay")
        .description("How long to wait in ticks before posting the next join/leave notification in your chat.")
        .range(0, 1000)
        .sliderRange(0, 100)
        .defaultValue(0)
        .build()
    );

    private final Setting<Boolean> simpleNotifications = sgJoinsLeaves.add(new BoolSetting.Builder()
        .name("simple-notifications")
        .description("Display join/leave notifications without a prefix, to reduce chat clutter.")
        .defaultValue(true)
        .build()
    );

    private int timer;
    private boolean loginPacket = true;
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Map<Integer, Vec3> pearlStartPosMap = new HashMap<>();
    private final ArrayListDeque<MutableComponent> messageQueue = new ArrayListDeque<>();

    private final Random random = new Random();

    public Notifier() {
        super(Categories.Misc, "notifier", "Notifies you of different events.");
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!event.entity.getUUID().equals(mc.player.getUUID()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Despawn) {
            if (event.entity instanceof Player) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((Player) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, ChatFormatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.level.playSound(mc.player, mc.player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableComponent text = Component.literal(event.entity.getType().getDescription().getString()).withStyle(ChatFormatting.WHITE);
                text.append(Component.literal(" has spawned at ").withStyle(ChatFormatting.GRAY));
                text.append(formatCoords(event.entity.position()));
                text.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
                info(text);
            }
        }

        if (pearl.get() && event.entity instanceof ThrownEnderpearl pearlEntity) {
            pearlStartPosMap.put(pearlEntity.getId(), new Vec3(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()));
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (!event.entity.getUUID().equals(mc.player.getUUID()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Spawn) {
            if (event.entity instanceof Player) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((Player) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, ChatFormatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.level.playSound(mc.player, mc.player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableComponent text = Component.literal(event.entity.getType().getDescription().getString()).withStyle(ChatFormatting.WHITE);
                text.append(Component.literal(" has despawned at ").withStyle(ChatFormatting.GRAY));
                text.append(formatCoords(event.entity.position()));
                text.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
                info(text);
            }
        }

        if (pearl.get()) {
            Entity e = event.entity;
            int i = e.getId();
            if (pearlStartPosMap.containsKey(i)) {
                ThrownEnderpearl pearl = (ThrownEnderpearl) e;
                if (pearl.getOwner() != null && pearl.getOwner() instanceof Player p) {
                    double d = pearlStartPosMap.get(i).distanceTo(e.position());
                    if ((!Friends.get().isFriend(p) || !pearlIgnoreFriends.get()) && (!p.equals(mc.player) || !pearlIgnoreOwn.get())) {
                        info("(highlight)%s's(default) pearl landed at %d, %d, %d (highlight)(%.1fm away, travelled %.1fm)(default).", pearl.getOwner().getName().getString(), pearl.blockPosition().getX(), pearl.blockPosition().getY(), pearl.blockPosition().getZ(), pearl.distanceTo(mc.player), d);
                    }
                }
                pearlStartPosMap.remove(i);
            }
        }
    }

    // Totem Pops && Joins/Leaves

    @Override
    public void onActivate() {
        totemPopMap.clear();
        chatIdMap.clear();
        pearlStartPosMap.clear();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        messageQueue.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        timer = 0;
        totemPopMap.clear();
        chatIdMap.clear();
        messageQueue.clear();
        pearlStartPosMap.clear();
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        loginPacket = true;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        switch (event.packet) {
            case
                ClientboundPlayerInfoUpdatePacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Joins) -> {
                if (loginPacket) {
                    loginPacket = false;
                    return;
                }

                if (packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                    createJoinNotifications(packet);
                }
            }
            case
                ClientboundPlayerInfoRemovePacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Leaves) ->
                createLeaveNotification(packet);

            case
                ClientboundEntityEventPacket packet when totemPops.get() && packet.getEventId() == EntityEvent.PROTECTED_FROM_DEATH && packet.getEntity(mc.level) instanceof Player entity -> {
                if ((entity.equals(mc.player) && totemsIgnoreOwn.get())
                    || (Friends.get().isFriend(entity) && totemsIgnoreOthers.get())
                    || (!Friends.get().isFriend(entity) && totemsIgnoreFriends.get())
                ) return;

                synchronized (totemPopMap) {
                    int pops = totemPopMap.getOrDefault(entity.getUUID(), 0);
                    totemPopMap.put(entity.getUUID(), ++pops);

                    double distance = PlayerUtils.distanceTo(entity);
                    if (totemsDistanceCheck.get() && distance > totemsDistance.get()) return;

                    ChatUtils.sendMsg(getChatId(entity), ChatFormatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (joinsLeavesMode.get() != JoinLeaveModes.None) {
            timer++;
            while (timer >= notificationDelay.get() && !messageQueue.isEmpty()) {
                timer = 0;
                if (simpleNotifications.get()) {
                    mc.player.sendSystemMessage(messageQueue.removeFirst());
                } else {
                    ChatUtils.sendMsg(messageQueue.removeFirst());
                }
            }
        }

        if (!totemPops.get()) return;
        synchronized (totemPopMap) {
            for (Player player : mc.level.players()) {
                if (!totemPopMap.containsKey(player.getUUID())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUUID());

                    ChatUtils.sendMsg(getChatId(player), ChatFormatting.GRAY, "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                    chatIdMap.removeInt(player.getUUID());
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIfAbsent(entity.getUUID(), value -> random.nextInt());
    }

    private void createJoinNotifications(ClientboundPlayerInfoUpdatePacket packet) {
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
            if (entry.profile() == null) continue;

            if (simpleNotifications.get()) {
                messageQueue.addLast(Component.literal(
                    ChatFormatting.GRAY + "["
                        + ChatFormatting.GREEN + "+"
                        + ChatFormatting.GRAY + "] "
                        + entry.profile().name()
                ));
            } else {
                messageQueue.addLast(Component.literal(
                    ChatFormatting.WHITE
                        + entry.profile().name()
                        + ChatFormatting.GRAY + " joined."
                ));
            }
        }
    }

    private void createLeaveNotification(ClientboundPlayerInfoRemovePacket packet) {
        if (mc.getConnection() == null) return;

        for (UUID id : packet.profileIds()) {
            PlayerInfo toRemove = mc.getConnection().getPlayerInfo(id);
            if (toRemove == null) continue;

            if (simpleNotifications.get()) {
                messageQueue.addLast(Component.literal(
                    ChatFormatting.GRAY + "["
                        + ChatFormatting.RED + "-"
                        + ChatFormatting.GRAY + "] "
                        + toRemove.getProfile().name()
                ));
            } else {
                messageQueue.addLast(Component.literal(
                    ChatFormatting.WHITE
                        + toRemove.getProfile().name()
                        + ChatFormatting.GRAY + " left."
                ));
            }
        }
    }

    public enum Event {
        Spawn,
        Despawn,
        Both
    }

    public enum JoinLeaveModes {
        None, Joins, Leaves, Both
    }
}
