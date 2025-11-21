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
import meteordevelopment.meteorclient.utils.network.DiscordWebhook;
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
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgJoinsLeaves = settings.createGroup("Joins/Leaves");
    private final SettingGroup sgDiscord = settings.createGroup("Discord Webhook");

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

    // Discord Webhook

    private final Setting<Boolean> discordWebhookEnabled = sgDiscord.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables Discord webhook notifications.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> webhookUrl = sgDiscord.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Discord webhook URL to send notifications to.")
        .defaultValue("")
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordPlayerDetection = sgDiscord.add(new BoolSetting.Builder()
        .name("player-detection")
        .description("Send Discord notification when a player enters render distance.")
        .defaultValue(true)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordTotemPops = sgDiscord.add(new BoolSetting.Builder()
        .name("totem-pops")
        .description("Send Discord notification for totem pops.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordPearls = sgDiscord.add(new BoolSetting.Builder()
        .name("pearl-alerts")
        .description("Send Discord notification for pearl throws.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordPlayerDeath = sgDiscord.add(new BoolSetting.Builder()
        .name("player-death")
        .description("Send Discord notification when you die.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordOtherPlayerDeath = sgDiscord.add(new BoolSetting.Builder()
        .name("other-player-death")
        .description("Send Discord notification when other players die in render distance.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> discordEntityDeath = sgDiscord.add(new BoolSetting.Builder()
        .name("entity-death")
        .description("Send Discord notification when entities die in render distance.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Set<EntityType<?>>> discordEntityTypes = sgDiscord.add(new EntityTypeListSetting.Builder()
        .name("entity-types")
        .description("Which entity types to track for death notifications.")
        .defaultValue(EntityType.ENDER_DRAGON, EntityType.WITHER)
        .visible(() -> discordWebhookEnabled.get() && discordEntityDeath.get())
        .build()
    );

    private final Setting<Boolean> discordPlayerMovement = sgDiscord.add(new BoolSetting.Builder()
        .name("player-movement")
        .description("Send Discord notification when a player moves a certain distance.")
        .defaultValue(false)
        .visible(discordWebhookEnabled::get)
        .build()
    );

    private final Setting<Integer> movementDistance = sgDiscord.add(new IntSetting.Builder()
        .name("movement-distance")
        .description("Distance in blocks before triggering movement notification.")
        .defaultValue(500)
        .min(50)
        .max(10000)
        .sliderRange(50, 2000)
        .visible(() -> discordWebhookEnabled.get() && discordPlayerMovement.get())
        .build()
    );

    private final Setting<Boolean> discordIgnoreOwnMovement = sgDiscord.add(new BoolSetting.Builder()
        .name("ignore-own-movement")
        .description("Ignore your own movement for Discord notifications.")
        .defaultValue(true)
        .visible(() -> discordWebhookEnabled.get() && discordPlayerMovement.get())
        .build()
    );

    private int timer;
    private boolean loginPacket = true;
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Map<Integer, Vec3d> pearlStartPosMap = new HashMap<>();
    private final ArrayListDeque<Text> messageQueue = new ArrayListDeque<>();
    private final Map<UUID, Vec3d> playerPositions = new HashMap<>();
    private final Map<UUID, String> lastDeathMessages = new HashMap<>();

    private final Random random = new Random();

    public Notifier() {
        super(Categories.Misc, "notifier", "Notifies you of different events.");
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Despawn) {
            if (event.entity instanceof PlayerEntity player) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(player)) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);

                    // Send Discord webhook notification
                    if (discordWebhookEnabled.get() && discordPlayerDetection.get() && !webhookUrl.get().isEmpty()) {
                        sendPlayerDetectionWebhook(player, true);
                    }
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
            if (event.entity instanceof PlayerEntity player) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(player)) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);

                    // Send Discord webhook notification
                    if (discordWebhookEnabled.get() && discordPlayerDetection.get() && !webhookUrl.get().isEmpty()) {
                        sendPlayerDetectionWebhook(player, false);
                    }
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

                        // Send Discord webhook notification for pearls
                        if (discordWebhookEnabled.get() && discordPearls.get() && !webhookUrl.get().isEmpty()) {
                            sendPearlWebhook(p, pearl, d);
                        }
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
        playerPositions.clear();
        lastDeathMessages.clear();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        messageQueue.clear();
        playerPositions.clear();
        lastDeathMessages.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        timer = 0;
        totemPopMap.clear();
        chatIdMap.clear();
        messageQueue.clear();
        pearlStartPosMap.clear();
        playerPositions.clear();
        lastDeathMessages.clear();
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

                    ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getName().getString(), pops, pops == 1 ? "totem" : "totems");

                    // Send Discord webhook notification for totem pops
                    if (discordWebhookEnabled.get() && discordTotemPops.get() && !webhookUrl.get().isEmpty()) {
                        sendTotemPopWebhook(entity, pops);
                    }
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

        if (totemPops.get()) {
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

        // Discord webhook death and movement detection
        if (discordWebhookEnabled.get() && !webhookUrl.get().isEmpty()) {
            // Check for player deaths
            if (discordPlayerDeath.get() || discordOtherPlayerDeath.get()) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player.deathTime > 0 || player.getHealth() <= 0) {
                        UUID uuid = player.getUuid();
                        String deathKey = uuid.toString() + "_" + System.currentTimeMillis() / 1000; // Per second check

                        // Only send once per death
                        if (!lastDeathMessages.containsKey(uuid) || !lastDeathMessages.get(uuid).equals(deathKey)) {
                            lastDeathMessages.put(uuid, deathKey);

                            boolean isOwnPlayer = player.equals(mc.player);
                            if ((isOwnPlayer && discordPlayerDeath.get()) || (!isOwnPlayer && discordOtherPlayerDeath.get())) {
                                sendPlayerDeathWebhook(player, isOwnPlayer);
                            }
                        }
                    }
                }
            }

            // Check for entity deaths
            if (discordEntityDeath.get()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof PlayerEntity) continue; // Skip players
                    if (!discordEntityTypes.get().contains(entity.getType())) continue;

                    if (entity.isRemoved() && !entity.isAlive()) {
                        UUID uuid = entity.getUuid();
                        String deathKey = uuid.toString() + "_death";

                        if (!lastDeathMessages.containsKey(uuid)) {
                            lastDeathMessages.put(uuid, deathKey);
                            sendEntityDeathWebhook(entity);
                        }
                    }
                }
            }

            // Track player movement
            if (discordPlayerMovement.get()) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (discordIgnoreOwnMovement.get() && player.equals(mc.player)) continue;
                    if (visualRangeIgnoreFriends.get() && Friends.get().isFriend(player)) continue;
                    if (player instanceof FakePlayerEntity) continue;

                    UUID uuid = player.getUuid();
                    Vec3d currentPos = player.getPos();

                    if (playerPositions.containsKey(uuid)) {
                        Vec3d lastPos = playerPositions.get(uuid);
                        double distance = currentPos.distanceTo(lastPos);

                        if (distance >= movementDistance.get()) {
                            playerPositions.put(uuid, currentPos);
                            sendPlayerMovementWebhook(player, lastPos, currentPos, distance);
                        }
                    } else {
                        playerPositions.put(uuid, currentPos);
                    }
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

    private String getServerIP() {
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        } else if (mc.isInSingleplayer()) {
            return "Singleplayer";
        }
        return "Unknown";
    }

    // Discord Webhook Methods

    private void sendPlayerDetectionWebhook(PlayerEntity player, boolean entered) {
        String title = entered ? "Player Entered Visual Range" : "Player Left Visual Range";
        String description = String.format("**%s** has %s your visual range!",
            player.getName().getString(),
            entered ? "entered" : "left");

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle(title)
            .setDescription(description)
            .setColor(entered ? new java.awt.Color(0, 255, 0) : new java.awt.Color(255, 0, 0))
            .addField("Player", player.getName().getString(), true)
            .addField("Position", String.format("X: %d, Y: %d, Z: %d",
                player.getBlockPos().getX(),
                player.getBlockPos().getY(),
                player.getBlockPos().getZ()), true)
            .addField("Distance", String.format("%.1f blocks", PlayerUtils.distanceTo(player)), true)
            .addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
    }

    private void sendTotemPopWebhook(PlayerEntity player, int pops) {
        String description = String.format("**%s** popped **%d** %s!",
            player.getName().getString(),
            pops,
            pops == 1 ? "totem" : "totems");

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle("Totem Pop Alert")
            .setDescription(description)
            .setColor(new java.awt.Color(255, 215, 0))
            .addField("Player", player.getName().getString(), true)
            .addField("Total Pops", String.valueOf(pops), true)
            .addField("Distance", String.format("%.1f blocks", PlayerUtils.distanceTo(player)), true)
            .addField("Position", String.format("X: %d, Y: %d, Z: %d",
                player.getBlockPos().getX(),
                player.getBlockPos().getY(),
                player.getBlockPos().getZ()), false)
            .addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
    }

    private void sendPearlWebhook(PlayerEntity player, EnderPearlEntity pearl, double distanceTravelled) {
        String description = String.format("**%s** threw a pearl!",
            player.getName().getString());

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle("Pearl Alert")
            .setDescription(description)
            .setColor(new java.awt.Color(0, 128, 128))
            .addField("Player", player.getName().getString(), true)
            .addField("Distance to You", String.format("%.1f blocks", pearl.distanceTo(mc.player)), true)
            .addField("Distance Travelled", String.format("%.1f blocks", distanceTravelled), true)
            .addField("Landing Position", String.format("X: %d, Y: %d, Z: %d",
                pearl.getBlockPos().getX(),
                pearl.getBlockPos().getY(),
                pearl.getBlockPos().getZ()), false)
            .addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
    }

    private void sendPlayerDeathWebhook(PlayerEntity player, boolean isOwnPlayer) {
        String title = isOwnPlayer ? "You Died!" : "Player Died";
        String description = String.format("**%s** has died!",
            player.getName().getString());

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle(title)
            .setDescription(description)
            .setColor(new java.awt.Color(139, 0, 0))
            .addField("Player", player.getName().getString(), true)
            .addField("Health", String.format("%.1f", player.getHealth()), true)
            .addField("Death Position", String.format("X: %d, Y: %d, Z: %d",
                player.getBlockPos().getX(),
                player.getBlockPos().getY(),
                player.getBlockPos().getZ()), false);

        if (!isOwnPlayer) {
            embed.addField("Distance", String.format("%.1f blocks", PlayerUtils.distanceTo(player)), true);
        }

        embed.addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
    }

    private void sendEntityDeathWebhook(Entity entity) {
        String description = String.format("**%s** has died!",
            entity.getType().getName().getString());

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle("Entity Died")
            .setDescription(description)
            .setColor(new java.awt.Color(169, 169, 169))
            .addField("Entity Type", entity.getType().getName().getString(), true)
            .addField("Distance", String.format("%.1f blocks", entity.distanceTo(mc.player)), true)
            .addField("Death Position", String.format("X: %d, Y: %d, Z: %d",
                entity.getBlockPos().getX(),
                entity.getBlockPos().getY(),
                entity.getBlockPos().getZ()), false)
            .addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
    }

    private void sendPlayerMovementWebhook(PlayerEntity player, Vec3d fromPos, Vec3d toPos, double distance) {
        String description = String.format("**%s** has moved **%.1f blocks**!",
            player.getName().getString(),
            distance);

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl.get());
        webhook.setUsername("Meteor Notifier");

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed()
            .setTitle("Player Movement Alert")
            .setDescription(description)
            .setColor(new java.awt.Color(255, 165, 0))
            .addField("Player", player.getName().getString(), true)
            .addField("Distance Moved", String.format("%.1f blocks", distance), true)
            .addField("From Position", String.format("X: %d, Y: %d, Z: %d",
                (int) fromPos.x,
                (int) fromPos.y,
                (int) fromPos.z), true)
            .addField("To Position", String.format("X: %d, Y: %d, Z: %d",
                player.getBlockPos().getX(),
                player.getBlockPos().getY(),
                player.getBlockPos().getZ()), true)
            .addField("Current Distance to You", String.format("%.1f blocks", PlayerUtils.distanceTo(player)), true)
            .addField("Server", getServerIP(), false)
            .setTimestamp(java.time.Instant.now().toString());

        webhook.addEmbed(embed);
        webhook.send();
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
