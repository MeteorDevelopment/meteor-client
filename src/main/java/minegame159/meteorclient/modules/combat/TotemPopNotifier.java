/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemPopNotifier extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> announce = sgGeneral.add(new BoolSetting.Builder()
            .name("announce-in-chat")
            .description("Sends a chat message for everyone to see instead of a client-side alert.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Doesn't announce your own totem pops.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFriend = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friend")
            .description("Doesn't announce your friend's totem pops.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> popMessage = sgGeneral.add(new StringSetting.Builder()
            .name("pop-message")
            .description("Chat alert to send when a player pops.")
            .defaultValue("EZ pops. {player} just popped {pops} {totems}. Meteor on Crack!")
            .build()
    );

    private final Setting<String> deathMessage = sgGeneral.add(new StringSetting.Builder()
            .name("death-message")
            .description("Chat alert to send on a player's death.")
            .defaultValue("EZZZ. {player} just died after popping {pops} {totems}. Meteor on Crack!")
            .build()
    );

    private final Map<UUID, Integer> totemPops = new HashMap<>();

    public TotemPopNotifier() {
        super(Category.Combat, "totem-pop-notifier", "Sends a chat message when a player either pops a totem or dies.");
    }

    @Override
    public void onActivate() {
        totemPops.clear();
    }

    @EventHandler
    private final Listener<GameJoinedEvent> onGameJoin = new Listener<>(event -> {
        totemPops.clear();
    });

    @EventHandler
    private final Listener<ReceivePacketEvent> onReceivePacket = new Listener<>(event -> {
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null || entity.equals(mc.player) || !FriendManager.INSTANCE.attack((PlayerEntity) entity)) return;

        synchronized (totemPops) {
            int pops = totemPops.getOrDefault(entity.getUuid(), 0);
            pops++;
            totemPops.put(entity.getUuid(), pops);
            String send = popMessage.get().replace("{player}",entity.getName().getString()).replace("{pops}", String.valueOf(pops)).replace("{totems}", (pops == 1 ? "totem" : "totems"));
            if (announce.get()) mc.player.sendChatMessage(send);
            else Chat.info("(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getName().getString(), pops, pops == 1 ? "totem" : "totems");
        }
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        synchronized (totemPops) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPops.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPops.remove(player.getUuid());
                    String send = deathMessage.get().replace("{player}", player.getName().getString()).replace("{pops}", String.valueOf(pops)).replace("{totems}", (pops == 1 ? "totem" : "totems"));
                    if (announce.get()) mc.player.sendChatMessage(send);
                    else Chat.info("(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                }
            }
        }
    });
}