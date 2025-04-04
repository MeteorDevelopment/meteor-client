/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.widgets.containers.WVerticalList;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.systems.proxies.Proxies;
import motordevelopment.motorclient.systems.proxies.Proxy;
import motordevelopment.motorclient.systems.proxies.ProxyType;
import motordevelopment.motorclient.utils.Utils;
import motordevelopment.motorclient.utils.render.color.Color;

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
                int pog = 0, bruh = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);

                    if (matcher.matches()) {
                        String address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group(3));

                        Proxy proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name(matcher.group(1) != null ? matcher.group(1) : address + ":" + port)
                            .type(matcher.group(4) != null ? ProxyType.parse(matcher.group(4)) : ProxyType.Socks4)
                            .build();

                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name.get()).color(Color.GREEN));
                            pog++;
                        }
                        else {
                            list.add(theme.label("Proxy already exists: " + proxy.name.get()).color(Color.ORANGE));
                            bruh++;
                        }
                    }
                    else {
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
