/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.utils.entity.FriendType;
import minegame159.meteorclient.utils.render.color.SettingColor;

public class TopBarFriends extends TopBarWindowScreen {
    public TopBarFriends() {
        super(TopBarType.Friends);

        refreshWidgetsOnInit = true;
    }

    @Override
    protected void initWidgets() {
        Settings s = new Settings();

        SettingGroup sgEnemy = s.createGroup("Enemies");
        SettingGroup sgNeutral = s.createGroup("Neutral");
        SettingGroup sgTrusted = s.createGroup("Trusted");

        // Enemies

        sgEnemy.add(new BoolSetting.Builder()
                .name("show-in-tracers")
                .description("Whether to show enemies in tracers.")
                .defaultValue(true)
                .onChanged(aBoolean -> Friends.get().showEnemies = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showEnemies))
                .build()
        );

        sgEnemy.add(new ColorSetting.Builder()
                .name("color")
                .description("The color used to show enemies in ESP and Tracers.")
                .defaultValue(new SettingColor(204, 0, 0))
                .onChanged(Friends.get().enemyColor::set)
                .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().enemyColor))
                .build()
        );

        // Neutral

        sgNeutral.add(new BoolSetting.Builder()
                .name("show-in-tracers")
                .description("Whether to show neutrals in tracers.")
                .defaultValue(true)
                .onChanged(aBoolean -> Friends.get().showNeutral = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showNeutral))
                .build()
        );

        sgNeutral.add(new ColorSetting.Builder()
                .name("color")
                .description("The color used to show neutrals in ESP and Tracers.")
                .defaultValue(new SettingColor(60, 240,240))
                .onChanged(Friends.get().neutralColor::set)
                .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().neutralColor))
                .build()
        );

        sgNeutral.add(new BoolSetting.Builder()
                .name("attack")
                .description("Whether to attack neutrals.")
                .defaultValue(false)
                .onChanged(aBoolean -> Friends.get().attackNeutral = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().attackNeutral))
                .build()
        );

        // Trusted

        sgTrusted.add(new BoolSetting.Builder()
                .name("show-in-tracers")
                .description("Whether to show trusted in tracers.")
                .defaultValue(true)
                .onChanged(aBoolean -> Friends.get().showTrusted = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Friends.get().showTrusted))
                .build()
        );

        sgTrusted.add(new ColorSetting.Builder()
                .name("color")
                .description("The color used to show trusted in ESP and Tracers.")
                .defaultValue(new SettingColor(57, 247, 47))
                .onChanged(Friends.get().trustedColor::set)
                .onModuleActivated(colorSetting -> colorSetting.set(Friends.get().trustedColor))
                .build()
        );

        add(s.createTable()).fillX().expandX();
        row();

        // Friends
        WSection section = add(new WSection("Friends", true)).fillX().expandX().getWidget();

        for (Friend friend : Friends.get()) {
            section.add(new WLabel(friend.name));

            WDropbox<FriendType> typeSetting = section.add(new WDropbox<>(friend.type)).getWidget();
            typeSetting.action = () -> friend.type = typeSetting.getValue();

            WMinus remove = section.add(new WMinus()).getWidget();
            remove.action = () -> {
                Friends.get().remove(friend);

                clear();
                initWidgets();
            };

            section.row();
        }

        WTable t = section.add(new WTable()).fillX().expandX().getWidget();
        WTextBox username = t.add(new WTextBox("", 400)).fillX().expandX().getWidget();
        username.setFocused(true);

        WPlus add = t.add(new WPlus()).getWidget();
        add.action = () -> {
            String name = username.getText().trim();
            if (!name.isEmpty()) Friends.get().add(new Friend(name));
        };
    }
}
