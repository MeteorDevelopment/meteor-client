/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.SharedConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static minegame159.meteorclient.utils.Utils.mc;

public class Placeholders {
    private static final Pattern pattern = Pattern.compile("(\\{version}|\\{mc_version}|\\{player}|\\{username}|\\{server})");

    public static String apply(String string) {
        Matcher matcher = pattern.matcher(string);
        StringBuffer sb = new StringBuffer(string.length());

        while (matcher.find()) {
            matcher.appendReplacement(sb, getReplacement(matcher.group(1)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String getReplacement(String placeholder) {
        switch (placeholder) {
            case "{version}":    return Config.get().version != null ? (Config.get().devBuild.isEmpty() ? Config.get().version.getOriginalString() : Config.get().version.getOriginalString() + " " + Config.get().devBuild) : "";
            case "{mc_version}": return SharedConstants.getGameVersion().getName();
            case "{player}":
            case "{username}":   return mc.getSession().getUsername();
            case "{server}":     return Utils.getWorldName();
            default:             return "";
        }
    }
}
