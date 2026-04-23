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
                        String address = matcher.group("address").replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group("port"));

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(matcher.group("name") != null ? matcher.group("name") : address + ":" + port)
                            .type(matcher.group("type") != null ? ProxyType.parse(matcher.group("type")) : ProxyType.SOCKS4)
                            .secure(matcher.group("secure") != null)
                            .build();
                    }

                    matcher = Proxies.PROXY_PATTERN_WEBSHARE.matcher(line);
                    if (proxy == null && matcher.matches()) {
                        String address = matcher.group("address").replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group("port"));

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(address + ":" + port)
                            .username(matcher.group("username") != null ? matcher.group("username") : "")
                            .password(matcher.group("password") != null ? matcher.group("password") : "")
                            .type(ProxyType.HTTP)
                            .secure(true)
                            .build();
                    }

                    matcher = Proxies.PROXY_PATTERN_URI.matcher(line);
                    if (proxy == null && matcher.matches()) {
                        String address = matcher.group("address").replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group("port"));

                        ProxyType type = ProxyType.parse(matcher.group("type"));
                        boolean secure = matcher.group("secure") != null;
                        if (type == null) {
                            type = ProxyType.HTTP;
                            secure = true;
                        }

                        proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(address + ":" + port)
                            .username(matcher.group("username") != null ? matcher.group("username") : "")
                            .password(matcher.group("password") != null ? matcher.group("password") : "")
                            .type(type)
                            .secure(secure)
                            .build();
                    }

                    if (proxy == null) {
                        list.add(theme.label("Unrecognised proxy format: " + line).color(Color.RED));
                        fail++;
                    } else {
                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name.get()).color(Color.GREEN));
                            success++;
                        } else {
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
        WButton refresh = add(theme.button("Check proxies")).expandX().widget();
        refresh.action = () -> {
            Proxies.get().checkProxies(false);
            onClose();
        };

        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::onClose;
    }
}
