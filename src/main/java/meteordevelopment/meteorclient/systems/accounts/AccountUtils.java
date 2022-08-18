/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

public class AccountUtils {
    public static void setBaseUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(service, url);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void setJoinUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("joinUrl");
            field.setAccessible(true);
            field.set(service, new URL(url));
        } catch (IllegalAccessException | NoSuchFieldException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void setCheckUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("checkUrl");
            field.setAccessible(true);
            field.set(service, new URL(url));
        } catch (IllegalAccessException | NoSuchFieldException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
