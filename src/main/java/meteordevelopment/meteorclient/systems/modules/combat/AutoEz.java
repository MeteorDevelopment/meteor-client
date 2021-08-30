/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

// Original from Kurumi Addon

package meteordevelopment.meteorclient.systems.modules.combat;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Random;

public class AutoEz extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Random random = new Random();
    public ArrayList<String> kills = new ArrayList<>();
    ArrayList<Pair<UUID, Long>> players = new ArrayList<>();
    ArrayList<String> msgplayers = new ArrayList<>();

    private final Setting<Boolean> toggleDominations = sgGeneral.add(new BoolSetting.Builder()
        .name("Dominations")
        .description("Different messages after 4 kills")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("Ignore Friends")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleClearOnActivate = sgGeneral.add(new BoolSetting.Builder()
        .name("Clear Kills On Activate")
        .description("Clear kill history when you turn on the module")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("Messages")
        .description("Messages to be sent when you kill somebody")
        .defaultValue(Arrays.asList(
            "Mongrel!",
            "This is gonna be a real piece of piss, you bloody fruit shop owners!",
            "G'day!",
            "Standin' around like a bloody idiot!",
            "Gotcha, ya mental defective!",
            "LOL! GIT GUD! ${PLAYER}!",
            "Cope."
        ))
        .build()
    );

    private final Setting<List<String>> dominations = sgGeneral.add(new StringListSetting.Builder()
        .name("Dominations")
        .description("Messages to be sent when you kill somebody 4 ore more times")
        .defaultValue(Arrays.asList(
            "You shouldn't have even gotten outta bed!",
            "This is getting too easy, mate!",
            "I'm running outta places to put holes in ya!",
            "How's about ya call it a day?",
            "Fightin men might not be your thing, lad.",
            "How many times have you died? I'm actually getting impressed. (${KILLCOUNT})",
            "Bloody hell, you're awful!",
            "Now I gotta make a necklace outta your teeth, bushman's rules.",
            "Dominated, ya ploddin' potatohead!",
            "Cope Harder!"
        ))
        .build()
    );

    public AutoEz() {
        super(Categories.Combat, "auto-ez", "When you kill somebody, you gotta mack em!");
    }

    @Override
    public void onActivate() {
        players.clear();
        msgplayers.clear();
        if (toggleClearOnActivate.get()) kills.clear();
    }

    private boolean checkFriend(PlayerEntity p) {
        return (ignoreFriends.get() && Friends.get().isFriend(p));
    }

    @EventHandler
    private void AttackEntity(AttackEntityEvent e) {
        if (e.entity instanceof EndCrystalEntity) {
            List<AbstractClientPlayerEntity> worldplayers = mc.world.getPlayers();
            for (int x = 0; x < worldplayers.size(); x++) {
                PlayerEntity p = worldplayers.get(x);
                if (!p.isSpectator() && !p.isCreative() && !p.isInvulnerable() && !mc.player.equals(p) && !checkFriend(p) && p.distanceTo(e.entity) < 12) {

                    Pair<UUID, Long> pair = new Pair<>(p.getUuid(), System.currentTimeMillis());
                    int index = -1;
                    for (int w = 0; w < players.size(); w++) {
                        if (players.get(w).getLeft().equals(p.getUuid())) {
                            index = w;
                            break;
                        }
                    }
                    if (index == -1) {
                        players.add(pair);
                    } else {
                        players.set(index, pair);
                    }

                }
            }
        }

        if (e.entity instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) e.entity;
            if (!p.isSpectator() && !p.isCreative() && !p.isInvulnerable() && !mc.player.equals(p) && !checkFriend(p)) {

                Pair<UUID, Long> pair = new Pair<>(p.getUuid(), System.currentTimeMillis());
                int index = -1;
                for (int w = 0; w < players.size(); w++) {
                    if (players.get(w).getLeft().equals(p.getUuid())) {
                        index = w;
                        break;
                    }
                }
                if (index == -1) {
                    players.add(pair);
                } else {
                    players.set(index, pair);
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (players.size() == 0) return;

        ArrayList<Pair<UUID, Long>> newPlayers = players;

        for (int x = 0; x < players.size(); x++) {
            Pair<UUID, Long> w = players.get(x);
            long time = w.getRight();

            PlayerEntity p = mc.world.getPlayerByUuid(w.getLeft());

            if (System.currentTimeMillis() - time > 2000 || p == null) {
                newPlayers.remove(x);
                continue;
            }

            if (p.isDead()) {
                if (!msgplayers.contains(p.getName().asString()))
                    msgplayers.add(p.getName().asString());
                newPlayers.remove(x);
                MeteorExecutor.execute(() -> send());
            }
        }

        players = newPlayers;
    }

    private void send() {
        int size = msgplayers.size();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        if (size != msgplayers.size()) {
            MeteorExecutor.execute(() -> send());
            return;
        }

        if (msgplayers.size() == 0) return;

        for ( int i = 0; i < msgplayers.size(); i += 1 ) {
            String playername = msgplayers.get(i);
            kills.add(playername);
            int counter = 0;
            for ( int j = 0; j < kills.size(); j += 1 ) {
                if ( kills.get(j) == playername ) counter += 1;
            }

            String msg;

            if ( toggleDominations.get() && counter >= 4 ) msg = dominations.get().get(random.nextInt(dominations.get().size()));
            else msg = messages.get().get(random.nextInt(messages.get().size()));
            msg = msg.replace("${PLAYER}", playername);
            msg = msg.replace("${KILLCOUNT}", String.valueOf(counter));
            mc.player.sendChatMessage(msg);

        }

        msgplayers.clear();
    }

}
