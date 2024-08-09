/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BungeeCordSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> spoofedIp = sgGeneral.add(new StringSetting.Builder()
            .name("spoofed-ip")
            .description("The IP to spoof. Recommended to change it to something not suspicious")
            .defaultValue("127.0.0.1")
            .build()
    );

    public final Setting<String> spoofedUuid = sgGeneral.add(new StringSetting.Builder()
            .name("spoofed-uuid")
            .description("The UUID to spoof. If empty then it uses UUID from your account or offline uuid if it is cracked account.")
            .defaultValue("")
            .filter((text, c) -> {
                if (text.length() > 32) return false;
                // UUID must be without dashes
                if (c == '-') return false;
                return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            })
            .build()
    );

    public BungeeCordSpoof() {
        super(Categories.Misc, "bungeeCord-spoof",
            "This module allows you to connect to backend servers that use BungeeCord (with 'bungeecord: true' enabled in spigot.yml). " +
                "Additionally, it allows you to spoof your IP and UUID.");
        this.runInMainMenu = true;
    }
}
