/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
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
        this.onClosed(() -> {
            if (parent instanceof ProxiesScreen screen) {
                screen.reload();
            }
        });
    }

    @Override
    public void initWidgets() {
        if (file.exists() && file.isFile()) {
            add(theme.label("Importing proxies from " + file.getName() + "...").color(Color.GREEN));
            WVerticalList list = add(theme.section("Log", false)).widget().add(theme.verticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int success = 0, fail = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher;
                    Proxy proxy = null;

                    matcher = Proxies.PROXY_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group(3));

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(matcher.group(1) != null ? matcher.group(1) : address + ":" + port)
                            .type(matcher.group(4) != null ? ProxyType.parse(matcher.group(4)) : ProxyType.Socks4)
                            .build();
                    }

                    matcher = Proxies.PROXY_PATTERN_WEBSHARE.matcher(line);
                    if (proxy == null && matcher.matches()) {
                        String address = matcher.group(1).replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group(2));

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(address + ":" + port)
                            .username(matcher.group(3) != null ? matcher.group(3) : "")
                            .password(matcher.group(4) != null ? matcher.group(4) : "")
                            .type(ProxyType.Socks5)
                            .build();
                    }

                    matcher = Proxies.PROXY_PATTERN_URI.matcher(line);
                    if (proxy == null && matcher.matches()) {
                        String address = matcher.group("addr").replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group("port"));

                        ProxyType type = ProxyType.parse(matcher.group(1));
                        if (type == null) {
                            if (matcher.group(1) != null && matcher.group(1).equals("socks")) type = ProxyType.Socks5;
                            // if it has a password it's a socks5 proxy
                            else if (matcher.group("pass") != null) type = ProxyType.Socks5;
                            else type = ProxyType.Socks4;
                        }

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(address + ":" + port)
                            .username(matcher.group("user") != null ? matcher.group("user") : "")
                            .password(matcher.group("pass") != null ? matcher.group("pass") : "")
                            .type(type)
                            .build();
                    }

                    if (proxy == null) {
                        list.add(theme.label("Unrecognised proxy format: " + line).color(Color.RED));
                        fail++;
                    } else {
                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name.get()).color(Color.GREEN));
                            success++;
                        }
                        else {
                            list.add(theme.label("Proxy already exists: " + proxy.name.get()).color(Color.ORANGE));
                            fail++;
                        }
                    }
                }
                add(theme
                    .label("Successfully imported " + success + "/" + (fail + success) + " proxies.")
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) success / (success + fail)))
                );
            } catch (IOException e) {
                MeteorClient.LOG.error("An error occurred while importing the proxy file", e);
            }
        } else {
            add(theme.label("Invalid File!"));
        }
        add(theme.horizontalSeparator()).expandX();
        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::close;
    }
}
