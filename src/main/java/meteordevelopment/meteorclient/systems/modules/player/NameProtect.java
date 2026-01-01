/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class NameProtect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> nameProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("name-protect")
        .description("Hides your name client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("Name to be replaced with.")
        .defaultValue("seasnail")
        .visible(nameProtect::get)
        .build()
    );

    private final Setting<Boolean> nickAll = sgGeneral.add(new BoolSetting.Builder()
        .name("nick-all")
        .description("Gives all players random names and skins.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> skinProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("skin-protect")
        .description("Make players become Steves.")
        .defaultValue(true)
        .build()
    );

    private String username = "If you see this, something is wrong.";
    private final Map<String, String> fakeNames = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public NameProtect() {
        super(Categories.Player, "name-protect", "Hide player names and skins.");
    }

    @Override
    public void onActivate() {
        username = mc.getSession().getUsername();
        fakeNames.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (nickAll.get() && mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().getName();
                if (!fakeNames.containsKey(name)) {
                    fakeNames.put(name, "Player" + (random.nextInt(9000) + 1000));
                }
            }
        }
    }

    public String replaceName(String string) {
        if (string == null || !isActive()) return string;

        String result = string;
        if (nameProtect.get()) {
            result = result.replace(username, name.get());
        }
        
        if (nickAll.get()) {
            // This is computationally expensive if we iterate.
            // But we don't have a list of all players in the string.
            // We can only replace known players.
            // For now, let's just rely on getName() being used by Nametags/TabList.
            // Chat replacement for *others* is hard without parsing.
            // But we can iterate the map if it's small.
            if (fakeNames.size() < 1000) {
                 for (Map.Entry<String, String> entry : fakeNames.entrySet()) {
                     result = result.replace(entry.getKey(), entry.getValue());
                 }
            }
        }
        
        return result;
    }

    public String getName(String original) {
        if (!isActive()) return original;

        if (original.equals(username) && nameProtect.get()) {
            return name.get();
        }

        if (nickAll.get()) {
            return fakeNames.computeIfAbsent(original, k -> "Player" + (random.nextInt(9000) + 1000));
        }

        return original;
    }

    public boolean skinProtect() {
        return isActive() && skinProtect.get();
    }
    
    public boolean shouldProtectSkin(String playerName) {
        if (!isActive()) return false;
        if (nickAll.get()) return true;
        if (playerName.equals(username)) return skinProtect.get();
        return false;
    }
}
