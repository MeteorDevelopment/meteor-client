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
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.util.math.Vec3d;

import java.util.*;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;

public class Notifier extends Module {
    private final SettingGroup sgTotemPops = settings.createGroup("totem-pops");
    private final SettingGroup sgVisualRange = settings.createGroup("visual-range");
    private final SettingGroup sgPearl = settings.createGroup("pearl");
    private final SettingGroup sgJoinsLeaves = settings.createGroup("joins/leaves");

    // Totem Pops

    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder()
        .name("totem-pops")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> totemsDistanceCheck = sgTotemPops.add(new BoolSetting.Builder()
        .name("distance-check")
        .defaultValue(false)
        .visible(totemPops::get)
        .build()
    );

    private final Setting<Integer> totemsDistance = sgTotemPops.add(new IntSetting.Builder()
        .name("player-radius")
        .defaultValue(30)
        .sliderRange(1, 50)
        .range(1, 100)
        .visible(() -> totemPops.get() && totemsDistanceCheck.get())
        .build()
    );

    private final Setting<Boolean> totemsIgnoreOwn = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-own")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreOthers = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-others")
        .defaultValue(false)
        .build()
    );

    // Visual Range

    private final Setting<Boolean> visualRange = sgVisualRange.add(new BoolSetting.Builder()
        .name("visual-range")
        .defaultValue(false)
        .build()
    );

    private final Setting<Event> event = sgVisualRange.add(new EnumSetting.Builder<Event>()
        .name("event")
        .defaultValue(Event.Both)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFriends = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFakes = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-fake-players")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> visualMakeSound = sgVisualRange.add(new BoolSetting.Builder()
        .name("sound")
        .defaultValue(true)
        .build()
    );

    // Pearl

    private final Setting<Boolean> pearl = sgPearl.add(new BoolSetting.Builder()
        .name("pearl")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pearlIgnoreOwn = sgPearl.add(new BoolSetting.Builder()
        .name("ignore-own")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pearlIgnoreFriends = sgPearl.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(false)
        .build()
    );

    // Joins/Leaves

    private final Setting<JoinLeaveModes> joinsLeavesMode = sgJoinsLeaves.add(new EnumSetting.Builder<JoinLeaveModes>()
        .name("player-joins-leaves")
        .defaultValue(JoinLeaveModes.None)
        .build()
    );

    private final Setting<Integer> notificationDelay = sgJoinsLeaves.add(new IntSetting.Builder()
        .name("notification-delay")
        .range(0, 1000)
        .sliderRange(0, 100)
        .defaultValue(0)
        .build()
    );

    private final Setting<Boolean> simpleNotifications = sgJoinsLeaves.add(new BoolSetting.Builder()
        .name("simple-notifications")
        .defaultValue(true)
        .build()
    );

    private int timer;
    private boolean loginPacket = true;
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Map<Integer, Vec3d> pearlStartPosMap = new HashMap<>();
    private final ArrayListDeque<Text> messageQueue = new ArrayListDeque<>();

    private final Random random = new Random();

    public Notifier() {
        super(Categories.Misc, "notifier");
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Despawn) {
            if (event.entity instanceof PlayerEntity) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsgRaw(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
                text.append(Text.literal(" has spawned at ").formatted(Formatting.GRAY));
                text.append(formatCoords(event.entity.getEntityPos()));
                text.append(Text.literal(".").formatted(Formatting.GRAY));
                info(text);
            }
        }

        if (pearl.get() && event.entity instanceof EnderPearlEntity pearlEntity) {
            pearlStartPosMap.put(pearlEntity.getId(), new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()));
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Spawn) {
            if (event.entity instanceof PlayerEntity) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsgRaw(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
                text.append(Text.literal(" has despawned at ").formatted(Formatting.GRAY));
                text.append(formatCoords(event.entity.getEntityPos()));
                text.append(Text.literal(".").formatted(Formatting.GRAY));
                info(text);
            }
        }

        if (pearl.get()) {
            Entity e = event.entity;
            int i = e.getId();
            if (pearlStartPosMap.containsKey(i)) {
                EnderPearlEntity pearl = (EnderPearlEntity) e;
                if (pearl.getOwner() != null && pearl.getOwner() instanceof PlayerEntity p) {
                    double d = pearlStartPosMap.get(i).distanceTo(e.getEntityPos());
                    if ((!Friends.get().isFriend(p) || !pearlIgnoreFriends.get()) && (!p.equals(mc.player) || !pearlIgnoreOwn.get())) {
                        info("(highlight)%s's(default) pearl landed at %d, %d, %d (highlight)(%.1fm away, travelled %.1fm)(default).", pearl.getOwner().getName().getString(), pearl.getBlockPos().getX(), pearl.getBlockPos().getY(), pearl.getBlockPos().getZ(), pearl.distanceTo(mc.player), d);
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
            case PlayerListS2CPacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Joins) -> {
                if (loginPacket) {
                    loginPacket = false;
                    return;
                }

                if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                    createJoinNotifications(packet);
                }
            }
            case PlayerRemoveS2CPacket packet when joinsLeavesMode.get().equals(JoinLeaveModes.Both) || joinsLeavesMode.get().equals(JoinLeaveModes.Leaves) ->
                createLeaveNotification(packet);

            case EntityStatusS2CPacket packet when totemPops.get() && packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING && packet.getEntity(mc.world) instanceof PlayerEntity entity -> {
                if ((entity.equals(mc.player) && totemsIgnoreOwn.get())
                    || (Friends.get().isFriend(entity) && totemsIgnoreOthers.get())
                    || (!Friends.get().isFriend(entity) && totemsIgnoreFriends.get())
                ) return;

                synchronized (totemPopMap) {
                    int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
                    totemPopMap.put(entity.getUuid(), ++pops);

                    double distance = PlayerUtils.distanceTo(entity);
                    if (totemsDistanceCheck.get() && distance > totemsDistance.get()) return;

                    ChatUtils.sendMsgRaw(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                }
            }
            default -> {}
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (joinsLeavesMode.get() != JoinLeaveModes.None) {
            timer++;
            while (timer >= notificationDelay.get() && !messageQueue.isEmpty()) {
                timer = 0;
                if (simpleNotifications.get()) {
                    mc.player.sendMessage(messageQueue.removeFirst(), false);
                } else {
                    ChatUtils.sendMsg(messageQueue.removeFirst());
                }
            }
        }

        if (!totemPops.get()) return;
        synchronized (totemPopMap) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPopMap.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUuid());

                    ChatUtils.sendMsgRaw(getChatId(player), Formatting.GRAY, "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                    chatIdMap.removeInt(player.getUuid());
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

    private void createJoinNotifications(PlayerListS2CPacket packet) {
        for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
            if (entry.profile() == null) continue;

            if (simpleNotifications.get()) {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.GREEN + "+"
                        + Formatting.GRAY + "] "
                        + entry.profile().name()
                ));
            } else {
                messageQueue.addLast(Text.literal(
                    Formatting.WHITE
                        + entry.profile().name()
                        + Formatting.GRAY + " joined."
                ));
            }
        }
    }

    private void createLeaveNotification(PlayerRemoveS2CPacket packet) {
        if (mc.getNetworkHandler() == null) return;

        for (UUID id : packet.profileIds()) {
            PlayerListEntry toRemove = mc.getNetworkHandler().getPlayerListEntry(id);
            if (toRemove == null) continue;

            if (simpleNotifications.get()) {
                messageQueue.addLast(Text.literal(
                    Formatting.GRAY + "["
                        + Formatting.RED + "-"
                        + Formatting.GRAY + "] "
                        + toRemove.getProfile().name()
                ));
            } else {
                messageQueue.addLast(Text.literal(
                    Formatting.WHITE
                        + toRemove.getProfile().name()
                        + Formatting.GRAY + " left."
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
