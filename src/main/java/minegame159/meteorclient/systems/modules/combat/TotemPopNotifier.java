/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.Random;
import java.util.UUID;

public class TotemPopNotifier extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> own = sgGeneral.add(new BoolSetting.Builder()
            .name("own")
            .description("Notifies you of your own totem pops.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Notifies you of your friends totem pops.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder()
            .name("others")
            .description("Notifies you of other players totem pops.")
            .defaultValue(true)
            .build()
    );

    private final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIds = new Object2IntOpenHashMap<>();

    private final Random random = new Random();

    public TotemPopNotifier() {
        super(Categories.Combat, "totem-pop-notifier", "Sends a chat message when a player either pops a totem or dies.");
    }

    @Override
    public void onActivate() {
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (entity == null
                || (entity.equals(mc.player) && !own.get())
                || (Friends.get().attack(((PlayerEntity) entity)) && !others.get())
                || (!Friends.get().attack(((PlayerEntity) entity)) && !friends.get())
        ) return;

        synchronized (totemPops) {
            int pops = totemPops.getOrDefault(entity.getUuid(), 0);
            totemPops.put(entity.getUuid(), ++pops);

            ChatUtils.info(getChatId(entity), "(highlight)%s (default)popped (highlight)%d (default)%s.", ((PlayerEntity) entity).getGameProfile().getName(), pops, pops == 1 ? "totem" : "totems");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (totemPops) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPops.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPops.removeInt(player.getUuid());

                    ChatUtils.info(getChatId(player), "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getGameProfile().getName(), pops, pops == 1 ? "totem" : "totems");
                    chatIds.removeInt(player.getUuid());
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIds.computeIntIfAbsent(entity.getUuid(), value -> random.nextInt());
    }
}