/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorAccount;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;

public class AccountTab extends Tab {
    public AccountTab() {
        super("Account");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return Config.get().token.isBlank() ? new LoginScreen(theme, this) : new AccountScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof AccountScreen || screen instanceof LoginScreen;
    }

    private static class AccountScreen extends WindowTabScreen {
        public AccountScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WButton logout = add(theme.button("Log Out")).expandX().widget();
            logout.action = () -> {
                Config.get().token = "";
                Tabs.get(AccountTab.class).openScreen(theme);
            };
        }
    }

    private static class LoginScreen extends WindowTabScreen {
        private static final String URL = "https://meteorclient.com/api/account/login?name=%s&password=%s";
        private static final Color RED = new Color(255, 0, 0);
        private static String error;

        public LoginScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            add(theme.label("Username or E-Mail")).expandX();
            WTextBox username = add(theme.textBox("")).minWidth(400).expandX().widget();

            add(theme.label("Password")).expandX();
            WTextBox password = add(theme.textBox("")).minWidth(400).padBottom(10).expandX().widget();

            if (error != null) add(theme.label(error).color(RED)).expandX();

            WButton login = add(theme.button("Log In")).expandX().widget();
            login.action = () -> {
                String usrText = username.get(), pswText = password.get();
                if (usrText.isBlank() || pswText.isBlank()) return;

                String url = String.format(URL, usrText, pswText);

                LoginResponse res = Http.get(url).sendJson(LoginResponse.class, false);
                if (res == null) return;
                if (res.error != null) {
                    error = res.error;
                    reload();
                    return;
                }
                else error = null;

                Config.get().token = res.token;
                MeteorAccount.login();
                Tabs.get(AccountTab.class).openScreen(theme);
            };
        }

        private static class LoginResponse {
            public String token, error;
        }
    }
}
