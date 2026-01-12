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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        .visible(totemPops::get)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends totem pops.")
        .defaultValue(false)
        .visible(totemPops::get)
        .build()
    );

    private final Setting<Boolean> totemsIgnoreOthers = sgTotemPops.add(new BoolSetting.Builder()
        .name("ignore-others")
        .description("Ignores other players totem pops.")
        .defaultValue(false)
        .visible(totemPops::get)
        .build()
    );

    private final Setting<FilterMode> totemsFilterMode = sgTotemPops.add(new EnumSetting.Builder<FilterMode>()
        .name("filter-mode")
        .description("Mode of the regex filter.")
        .defaultValue(FilterMode.None)
        .visible(totemPops::get)
        .build()
    );

    private final Setting<String> totemsFilterRegex = sgTotemPops.add(new StringSetting.Builder()
        .name("filter-regex")
        .description("Regex pattern for filtering player names.")
        .defaultValue("^.+$")
        .visible(() -> totemPops.get() && totemsFilterMode.get() != FilterMode.None)
        .onChanged(s -> totemsFilterPattern = compileRegex(s))
        .build()
    );
    private Pattern totemsFilterPattern = Pattern.compile("^.+$");

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
        .visible(visualRange::get)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to notify about.")
        .defaultValue(EntityType.PLAYER)
        .visible(visualRange::get)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFriends = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends.")
        .defaultValue(true)
        .visible(visualRange::get)
        .build()
    );

    private final Setting<Boolean> visualRangeIgnoreFakes = sgVisualRange.add(new BoolSetting.Builder()
        .name("ignore-fake-players")
        .description("Ignores fake players.")
        .defaultValue(true)
        .visible(visualRange::get)
        .build()
    );

    private final Setting<Boolean> visualMakeSound = sgVisualRange.add(new BoolSetting.Builder()
        .name("sound")
        .description("Emits a sound effect on enter / leave")
        .defaultValue(true)
        .visible(visualRange::get)
        .build()
    );

    private final Setting<FilterMode> visualRangeFilterMode = sgVisualRange.add(new EnumSetting.Builder<FilterMode>()
        .name("filter-mode")
        .description("Mode of the regex filter.")
        .defaultValue(FilterMode.None)
        .visible(visualRange::get)
        .build()
    );

    private final Setting<String> visualRangeFilterRegex = sgVisualRange.add(new StringSetting.Builder()
        .name("filter-regex")
        .description("Regex pattern for filtering entity names.")
        .defaultValue("^.+$")
        .visible(() -> visualRange.get() && visualRangeFilterMode.get() != FilterMode.None)
        .onChanged(s -> visualRangeFilterPattern = compileRegex(s))
        .build()
    );
    private Pattern visualRangeFilterPattern = Pattern.compile("^.+$");

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
        .visible(pearl::get)
        .build()
    );

    private final Setting<Boolean> pearlIgnoreFriends = sgPearl.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends pearls.")
        .defaultValue(false)
        .visible(pearl::get)
        .build()
    );

    private final Setting<FilterMode> pearlFilterMode = sgPearl.add(new EnumSetting.Builder<FilterMode>()
        .name("filter-mode")
        .description("Mode of the regex filter.")
        .defaultValue(FilterMode.None)
        .visible(pearl::get)
        .build()
    );

    private final Setting<String> pearlFilterRegex = sgPearl.add(new StringSetting.Builder()
        .name("filter-regex")
        .description("Regex pattern for filtering player names.")
        .defaultValue("^.+$")
        .visible(() -> pearl.get() && pearlFilterMode.get() != FilterMode.None)
        .onChanged(s -> pearlFilterPattern = compileRegex(s))
        .build()
    );
    private Pattern pearlFilterPattern = Pattern.compile("^.+$");

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
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None)
        .build()
    );

    private final Setting<Boolean> simpleNotifications = sgJoinsLeaves.add(new BoolSetting.Builder()
        .name("simple-notifications")
        .description("Display join/leave notifications without a prefix, to reduce chat clutter.")
        .defaultValue(true)
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None)
        .build()
    );

    private final Setting<Boolean> joinsLeavesIgnoreFriends = sgJoinsLeaves.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends joining/leaving.")
        .defaultValue(false)
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None)
        .build()
    );

    private final Setting<Boolean> joinsLeavesIgnoreOthers = sgJoinsLeaves.add(new BoolSetting.Builder()
        .name("ignore-others")
        .description("Ignores non-friends joining/leaving.")
        .defaultValue(false)
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None)
        .build()
    );

    private final Setting<FilterMode> joinsLeavesFilterMode = sgJoinsLeaves.add(new EnumSetting.Builder<FilterMode>()
        .name("filter-mode")
        .description("Mode of the regex filter.")
        .defaultValue(FilterMode.None)
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None)
        .build()
    );

    private final Setting<String> joinsLeavesFilterRegex = sgJoinsLeaves.add(new StringSetting.Builder()
        .name("filter-regex")
        .description("Regex pattern for filtering player names.")
        .defaultValue("^.+$")
        .visible(() -> joinsLeavesMode.get() != JoinLeaveModes.None && joinsLeavesFilterMode.get() != FilterMode.None)
        .onChanged(s -> joinsLeavesFilterPattern = compileRegex(s))
        .build()
    );
    private Pattern joinsLeavesFilterPattern = Pattern.compile("^.+$");

    private int timer;
    private boolean loginPacket = true;
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Map<Integer, Vec3d> pearlStartPosMap = new HashMap<>();
    private final ArrayListDeque<Text> messageQueue = new ArrayListDeque<>();

    private final Random random = new Random();

    public Notifier() {
        super(Categories.Misc, "notifier", "Notifies you of different events.");
    }

    private static Pattern compileRegex(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    private boolean shouldNotify(String name, FilterMode filterMode, Pattern filterPattern, boolean isOwn, boolean ignoreOwn, boolean isFriend, boolean ignoreFriends, boolean ignoreOthers) {
        if (filterMode != FilterMode.None && filterPattern != null) {
            boolean matches = filterPattern.matcher(name).matches();
            if (filterMode == FilterMode.AlwaysNotify && matches) return true;
            if (filterMode == FilterMode.AlwaysIgnore && matches) return false;
        }

        if (isOwn && ignoreOwn) return false;
        if (isFriend && ignoreFriends) return false;
        if (!isFriend && !isOwn && ignoreOthers) return false;

        return true;
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Despawn) {
            if (event.entity instanceof PlayerEntity player) {
                if (visualRangeIgnoreFakes.get() && (event.entity instanceof FakePlayerEntity || EntityUtils.getGameMode(player) == null)) return;

                String name = player.getName().getString();
                boolean isFriend = Friends.get().isFriend(player);

                if (shouldNotify(name, visualRangeFilterMode.get(), visualRangeFilterPattern,
                    false, false, isFriend, visualRangeIgnoreFriends.get(), false)) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered your visual range!", name);

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                String entityName = event.entity.getType().getName().getString();

                if (shouldNotify(entityName, visualRangeFilterMode.get(), visualRangeFilterPattern,
                    false, false, false, false, false)) {
                    MutableText text = Text.literal(entityName).formatted(Formatting.WHITE);
                    text.append(Text.literal(" has spawned at ").formatted(Formatting.GRAY));
                    text.append(formatCoords(event.entity.getEntityPos()));
                    text.append(Text.literal(".").formatted(Formatting.GRAY));
                    info(text);
                }
            }
        }

        if (pearl.get() && event.entity instanceof EnderPearlEntity pearlEntity) {
            pearlStartPosMap.put(pearlEntity.getId(), new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()));
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Spawn) {
            if (event.entity instanceof PlayerEntity player) {
                if (visualRangeIgnoreFakes.get() && (event.entity instanceof FakePlayerEntity || EntityUtils.getGameMode(player) == null)) return;

                String name = player.getName().getString();
                boolean isFriend = Friends.get().isFriend(player);

                if (shouldNotify(name, visualRangeFilterMode.get(), visualRangeFilterPattern,
                    false, false, isFriend, visualRangeIgnoreFriends.get(), false)) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has left your visual range!", name);

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                String entityName = event.entity.getType().getName().getString();

                if (shouldNotify(entityName, visualRangeFilterMode.get(), visualRangeFilterPattern,
                    false, false, false, false, false)) {
                    MutableText text = Text.literal(entityName).formatted(Formatting.WHITE);
                    text.append(Text.literal(" has despawned at ").formatted(Formatting.GRAY));
                    text.append(formatCoords(event.entity.getEntityPos()));
                    text.append(Text.literal(".").formatted(Formatting.GRAY));
                    info(text);
                }
            }
        }

        if (pearl.get()) {
            Entity e = event.entity;
            int i = e.getId();
            if (pearlStartPosMap.containsKey(i)) {
                EnderPearlEntity pearlEntity = (EnderPearlEntity) e;
                if (pearlEntity.getOwner() != null && pearlEntity.getOwner() instanceof PlayerEntity p) {
                    String ownerName = p.getName().getString();
                    boolean isFriend = Friends.get().isFriend(p);
                    boolean isOwn = p.equals(mc.player);

                    if (shouldNotify(ownerName, pearlFilterMode.get(), pearlFilterPattern, isOwn, pearlIgnoreOwn.get(), isFriend, pearlIgnoreFriends.get(), false)) {
                        double d = pearlStartPosMap.get(i).distanceTo(e.getEntityPos());
                        info("(highlight)%s's(default) pearl landed at %d, %d, %d (highlight)(%.1fm away, travelled %.1fm)(default).", ownerName, pearlEntity.getBlockPos().getX(), pearlEntity.getBlockPos().getY(), pearlEntity.getBlockPos().getZ(), pearlEntity.distanceTo(mc.player), d);
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
                String name = entity.getName().getString();
                boolean isFriend = Friends.get().isFriend(entity);
                boolean isOwn = entity.equals(mc.player);

                if (!shouldNotify(name, totemsFilterMode.get(), totemsFilterPattern,
                    isOwn, totemsIgnoreOwn.get(), isFriend, totemsIgnoreFriends.get(), totemsIgnoreOthers.get())) {
                    return;
                }

                synchronized (totemPopMap) {
                    int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
                    totemPopMap.put(entity.getUuid(), ++pops);

                    double distance = PlayerUtils.distanceTo(entity);
                    if (totemsDistanceCheck.get() && distance > totemsDistance.get()) return;

                    ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", name, pops, pops == 1 ? "totem" : "totems");
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

                    ChatUtils.sendMsg(getChatId(player), Formatting.GRAY, "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getName().getString(), pops, pops == 1 ? "totem" : "totems");
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

            String name = entry.profile().name();
            boolean isFriend = Friends.get().get(name) != null;

            if (!shouldNotify(name, joinsLeavesFilterMode.get(), joinsLeavesFilterPattern,
                false, false, isFriend, joinsLeavesIgnoreFriends.get(), joinsLeavesIgnoreOthers.get())) {
                continue;
            }

            if (simpleNotifications.get()) {
                messageQueue.addLast(Text.literal("[").formatted(Formatting.GRAY)
                    .append(Text.literal("+").formatted(Formatting.GREEN))
                    .append(Text.literal("] ").formatted(Formatting.GRAY))
                    .append(Text.literal(name).formatted(Formatting.WHITE)));
            } else {
                messageQueue.addLast(Text.literal(name).formatted(Formatting.WHITE)
                    .append(Text.literal(" joined.").formatted(Formatting.GRAY)));
            }
        }
    }

    private void createLeaveNotification(PlayerRemoveS2CPacket packet) {
        if (mc.getNetworkHandler() == null) return;

        for (UUID id : packet.profileIds()) {
            PlayerListEntry toRemove = mc.getNetworkHandler().getPlayerListEntry(id);
            if (toRemove == null) continue;

            String name = toRemove.getProfile().name();
            boolean isFriend = Friends.get().get(name) != null;

            if (!shouldNotify(name, joinsLeavesFilterMode.get(), joinsLeavesFilterPattern,
                false, false, isFriend, joinsLeavesIgnoreFriends.get(), joinsLeavesIgnoreOthers.get())) {
                continue;
            }

            if (simpleNotifications.get()) {
                messageQueue.addLast(Text.literal("[").formatted(Formatting.GRAY)
                    .append(Text.literal("-").formatted(Formatting.RED))
                    .append(Text.literal("] ").formatted(Formatting.GRAY))
                    .append(Text.literal(name).formatted(Formatting.WHITE)));
            } else {
                messageQueue.addLast(Text.literal(name).formatted(Formatting.WHITE)
                    .append(Text.literal(" left.").formatted(Formatting.GRAY)));
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

    public enum FilterMode {
        None,
        AlwaysIgnore,
        AlwaysNotify
    }
}
