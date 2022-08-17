/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.sql.Time;
import java.util.*;

public class AutoEz  extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> taunts = sgGeneral.add(new StringListSetting.Builder()
        .name("taunts")
        .description("Messages to be sent when you kill somebody")
        .defaultValue(List.of(
            "Mongrel!",
            "G'day!",
            "Gotcha, ya mental defective!",
            "Bloody hell, you're awful!",
            "LOL! GIT GUD!",
            "Cope.",
            "%PLAYER"
        ))
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        // .description("Ignore Friends!")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enableDominations = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-dominations")
        .description("Toggle to enable/disable dominations")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> dominations = sgGeneral.add(new StringListSetting.Builder()
        .name("dominations")
        .description("Messages to be sent when you kill somebody more than 3 times")
        .defaultValue(List.of(
            "This is getting too easy, mate!",
            "How's about ya call it a day?",
            "Fightin men might not be your thing, lad.",
            "How many times have you died? I'm actually getting impressed.",
            "Now I gotta make a necklace outta your teeth, bushman's rules.",
            "Cope Harder!",
            "%KILLCOUNT"
        ))
        // .visible(enable_dominations::get)
        .visible(() -> enableDominations.get())
        .build()
    );

    private final Setting<Boolean> clearStats = sgGeneral.add(new BoolSetting.Builder()
        .name("clear-stats-on-activate")
        .description("Clear Kill History on Activate")
        .defaultValue(true)
        .build()
    );

    private final Random random = new Random();
    private final HashMap<UUID, Long> players = new HashMap<UUID, Long>();
    private final HashMap<UUID, Integer> kills = new HashMap<UUID, Integer>();

    public AutoEz() {
        super(Categories.Misc, "auto-ez", "When you kill somebody, you gotta mack em!");
    }

    private final boolean friendCheck (PlayerEntity p) { return (ignoreFriends.get() && Friends.get().isFriend(p)); }

    private void send(UUID uuid, String username) {
        Integer killCnt = kills.get(uuid);
        killCnt = (killCnt == null ? 0 : killCnt);
        killCnt += 1;
        kills.put(uuid, killCnt);
        String taunt;
        if (killCnt > 3 && enableDominations.get()) {
            taunt = dominations.get().get(random.nextInt(dominations.get().size()));
        } else {
            taunt = taunts.get().get(random.nextInt(taunts.get().size()));
        }
        taunt = taunt.replaceAll("%PLAYER".toString(), username);
        taunt = taunt.replaceAll("%KILLCOUNT".toString(), String.valueOf(killCnt));
        ChatUtils.sendPlayerMsg(taunt);
    }

    @Override
    public void onActivate() {
        if(clearStats.get()) {
            kills.clear();
        }
    }

    @EventHandler
    private void AttackEntity(AttackEntityEvent e) {
        if (e.entity instanceof EndCrystalEntity) {
            List<AbstractClientPlayerEntity> worldplayers = mc.world.getPlayers();
            for (PlayerEntity player : worldplayers) {
                if (
                    !player.isSpectator() &&
                    !player.isCreative() &&
                    !player.isInvulnerable() &&
                    !mc.player.equals(player) &&
                    !friendCheck(player) &&
                    player.distanceTo(e.entity) < 12)
                {
                    players.put(player.getUuid(), System.currentTimeMillis());
                }
            }
        }

        if (e.entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) e.entity;
            if (
                !player.isSpectator() &&
                !player.isCreative() &&
                !player.isInvulnerable() &&
                !mc.player.equals(player) &&
                !friendCheck(player))
            {
                players.put(player.getUuid(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (players.size() == 0) return;

        for (Map.Entry<UUID, Long> entry : players.entrySet()) {
            PlayerEntity player = mc.world.getPlayerByUuid(entry.getKey());
            if (System.currentTimeMillis() - entry.getValue() > 2000 || player == null) {
                players.remove(entry.getKey());
                continue;
            }

            if (player.isDead()) {
                send(player.getUuid(), player.getGameProfile().getName());
                players.remove(entry.getKey());
            }
        }
    }
}
