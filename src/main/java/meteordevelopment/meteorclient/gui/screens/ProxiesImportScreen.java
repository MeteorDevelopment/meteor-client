/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.systems.proxies.ProxyType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;

public class ProxiesImportScreen extends WindowScreen {

    private final File file;
    public ProxiesImportScreen(GuiTheme theme, File file) {
        super(theme, "Import Proxies");
        this.file = file;
        onClosed(() -> {
            if (parent instanceof ProxiesScreen screen) screen.reload();
        });
    }

    @Override
    public void initWidgets() {
        if (file.exists() && file.isFile()) {
            add(theme.label("Importing proxies from %s...".formatted(file.getName())).color(Color.GREEN));
            WVerticalList list = add(theme.section("Log", false)).widget().add(theme.verticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int success = 0;
                int failure = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);

                    if (matcher.matches()) {
                        String address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group(3));

                        Proxy proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(matcher.group(1) != null ? matcher.group(1) : "%s:%d".formatted(address, port))
                            .type(matcher.group(4) != null ? ProxyType.parse(matcher.group(4)) : ProxyType.SOCKS_4)
                            //FIXME: this doesn't differentiate socks5 vs socks4 https://www.wikiwand.com/en/SOCKS
                            //Can be tested with these: https://github.com/TheSpeedX/PROXY-List
                            .build();

                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: %s".formatted(proxy.name.get())).color(Color.GREEN));
                            success++;
                        }
                        else {
                            list.add(theme.label("Proxy already exists: %s".formatted(proxy.name.get())).color(Color.ORANGE));
                            failure++;
                        }
                    }
                    else {
                        list.add(theme.label("Invalid proxy: %s".formatted(line)).color(Color.RED));
                        failure++;
                    }
                }
                add(theme
                    .label("Successfully imported %d/%d proxies.".formatted(success, failure + success))
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) success / (success + failure)))
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else add(theme.label("Invalid File!"));

        add(theme.horizontalSeparator()).expandX();
        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::close;
    }
}
