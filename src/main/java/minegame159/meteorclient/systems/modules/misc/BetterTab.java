/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.friends.Friend;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.MeteorPlayers;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class BetterTab extends Module {

    private final SettingGroup sgDefault = settings.getDefaultGroup();

    public final Setting<Integer> tabSize = sgDefault.add(new IntSetting.Builder()
            .name("tablist-size")
            .description("Bypasses the 80 player limit on the tablist.")
            .defaultValue(100)
            .min(1)
            .sliderMax(1000)
            .sliderMin(1)
            .build()
    );

    private final Setting<Boolean> self = sgDefault.add(new BoolSetting.Builder()
            .name("highlight-self")
            .description("Highlights yourself in the tablist.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> selfColor = sgDefault.add(new ColorSetting.Builder()
            .name("self-color")
            .description("The color to highlight your name with.")
            .defaultValue(new SettingColor(250, 130, 30))
            .build()
    );

    private final Setting<Boolean> friends = sgDefault.add(new BoolSetting.Builder()
            .name("highlight-friends")
            .description("Highlights friends in the tablist.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> meteor = sgDefault.add(new BoolSetting.Builder()
            .name("meteor-users")
            .description("Shows if the player is using Meteor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> meteorColor = sgDefault.add(new ColorSetting.Builder()
            .name("meteor-color")
            .description("The color to highlight meteor users with.")
            .defaultValue(new SettingColor(135, 0, 255))
            .build()
    );


    public BetterTab() {
        super(Categories.Misc, "better-tab", "Various improvements to the tab list.");
    }

    public Text getPlayerName(PlayerListEntry playerListEntry) {
        Text name;
        Color color = null;

        name = playerListEntry.getDisplayName();
        if (name == null) name = new LiteralText(playerListEntry.getProfile().getName());

        if (playerListEntry.getProfile().getId().toString().equals(mc.player.getGameProfile().getId().toString()) && self.get()) {
            color = selfColor.get();
        }
        else if (friends.get() && Friends.get().get(playerListEntry.getProfile().getName()) != null) {
            Friend friend = Friends.get().get(playerListEntry.getProfile().getName());
            if (friend != null) color = Friends.get().getFriendColor(friend);
        }
        else if (meteor.get() && MeteorPlayers.get(playerListEntry.getProfile().getId())) {
            color = meteorColor.get();
        }

        if (color != null) {
            String nameString = name.getString();

            for (Formatting format : Formatting.values()) {
                if (format.isColor()) nameString = nameString.replace(format.toString(), "");
            }

            name = new LiteralText(nameString).setStyle(name.getStyle().withColor(new TextColor(color.getPacked())));
        }

        return name;
    }

}
