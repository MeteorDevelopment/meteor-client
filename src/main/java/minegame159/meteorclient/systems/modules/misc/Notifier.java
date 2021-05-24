/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.entity.EntityRemovedEvent;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.UUID;

public class Notifier extends Module {

    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");

    // Totem Pops

    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder()
            .name("totem-pops")
            .description("Notifies you when a player pops a totem.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> totemsIgnoreOwn = sgTotemPops.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Notifies you of your own totem pops.")
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

    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();

    private final Random random = new Random();

    public Notifier() {
        super(Categories.Misc, "notifier", "Notifies you of different events.");
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        Entity entity = event.entity;

        if (visualRange.get() && entity instanceof PlayerEntity) {
            if (!entity.equals(mc.player) && (!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) entity))) && (!visualRangeIgnoreFakes.get() || !(entity instanceof FakePlayerEntity))) {
                ChatUtils.sendMsg(event.entity.getEntityId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getEntityName());
            }
        }

    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        Entity entity = event.entity;

        if (visualRange.get() && entity instanceof PlayerEntity) {
            if (!entity.equals(mc.player) && (!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) entity))) && (!visualRangeIgnoreFakes.get() || !(entity instanceof FakePlayerEntity))) {
                ChatUtils.sendMsg(event.entity.getEntityId() + 100, Formatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getEntityName());
            }
        }
    }

    // Totem Pops

    @Override
    public void onActivate() {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!totemPops.get()) return;
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (entity == null
                || (entity.equals(mc.player) && totemsIgnoreOwn.get())
                || (Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreOthers.get())
                || (!Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreFriends.get())
        ) return;

        synchronized (totemPopMap) {
            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++pops);

            ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", ((PlayerEntity) entity).getEntityName(), pops, pops == 1 ? "totem" : "totems");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!totemPops.get()) return;
        synchronized (totemPopMap) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPopMap.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUuid());

                    ChatUtils.sendMsg(getChatId(player), Formatting.GRAY, "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getEntityName(), pops, pops == 1 ? "totem" : "totems");
                    chatIdMap.removeInt(player.getUuid());
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIntIfAbsent(entity.getUuid(), value -> random.nextInt());
    }
}
