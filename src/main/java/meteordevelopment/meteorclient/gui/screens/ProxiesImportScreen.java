/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
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
        this.onClosed(() -> {
            if (parent instanceof ProxiesScreen screen) {
                screen.dirty = true;
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
                int pog = 0, bruh = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        Proxy proxy = new Proxy();
                        proxy.address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        // should be safe to parse because of regex matching
                        proxy.port = Integer.parseInt(matcher.group(3));
                        proxy.name = matcher.group(1) != null ? matcher.group(1) : proxy.address + ":" + proxy.port;
                        proxy.type = matcher.group(4) != null ? ProxyType.parse(matcher.group(4)) : ProxyType.Socks4;
                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name).color(Color.GREEN));
                            pog++;
                        } else {
                            list.add(theme.label("Proxy already exists: " + proxy.name).color(Color.ORANGE));
                            bruh++;
                        }
                    } else {
                        list.add(theme.label("Invalid proxy: " + line).color(Color.RED));
                        bruh++;
                    }
                }
                add(theme
                    .label("Successfully imported " + pog + "/" + (bruh + pog) + " proxies.")
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) pog / (pog + bruh)))
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            add(theme.label("Invalid File!"));
        }
        add(theme.horizontalSeparator()).expandX();
        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::close;
    }
}
