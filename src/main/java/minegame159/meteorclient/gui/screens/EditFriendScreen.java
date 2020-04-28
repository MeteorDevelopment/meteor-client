package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.accountsfriends.Friend;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditFriendScreen extends WindowScreen {
    private Friend friend;

    public final Map<String, List<Setting<?>>> settingGroups = new LinkedHashMap<>(1);
    public final List<Setting<?>> settings = new ArrayList<>(1);

    public EditFriendScreen(Friend friend) {
        super(friend.name, true);
        this.friend = friend;

        addSetting(new BoolSetting.Builder()
                .name("trusted")
                .description("Do you trust this person?")
                .defaultValue(friend.trusted)
                .onChanged(aBoolean -> friend.trusted = aBoolean)
                .build()
        );

        addSetting(new ColorSetting.Builder()
                .name("color")
                .description("Color.")
                .defaultValue(friend.color)
                .onChanged(color -> friend.color = color)
                .build()
        );

        addSetting(new BoolSetting.Builder()
                .name("attack")
                .description("Should modules attack this person?")
                .defaultValue(friend.attack)
                .onChanged(aBoolean -> friend.attack = aBoolean)
                .build()
        );

        addSetting(new BoolSetting.Builder()
                .name("show-in-tracers")
                .description("Show in tracers.")
                .defaultValue(friend.showInTracers)
                .onChanged(aBoolean -> friend.showInTracers = aBoolean)
                .build()
        );

        createSettingsWindow();
        row();
        add(new WButton("Back")).fillX().expandX().getWidget().action = button1 -> onClose();
    }

    private  <T> Setting<T> addSetting(Setting<T> setting) {
        settings.add(setting);
        List<Setting<?>> group = settingGroups.computeIfAbsent(setting.group == null ? "Other" : setting.group, s -> new ArrayList<>(1));
        group.add(setting);
        return setting;
    }

    private void createSettingsWindow() {
        // Settings
        if (settingGroups.size() > 0) {
            WTable table = add(new WTable()).fillX().expandX().getWidget();
            for (String group : settingGroups.keySet()) {
                if (settingGroups.size() > 1) {
                    table.add(new WHorizontalSeparator(group)).fillX().expandX();
                    table.row();
                }

                for (Setting<?> setting : settingGroups.get(group)) {
                    if (setting.isVisible()) {
                        ModuleScreen.generateSettingToGrid(table, setting);
                    }
                }
            }
        }
    }
}
