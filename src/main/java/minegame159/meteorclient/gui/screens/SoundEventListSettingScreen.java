package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SoundEventListSettingScreen extends WindowScreen {
    private final Setting<List<SoundEvent>> setting;
    private final WTextBox filter;

    public SoundEventListSettingScreen(Setting<List<SoundEvent>> setting) {
        super("Select sounds", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 0);
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX();
        row();

        // All sounds
        WTable table1 = add(new WTable()).top().getWidget();
        Consumer<SoundEvent> soundForEach = sound -> {
            if (setting.get().contains(sound)) return;

            table1.add(new WLabel(getName(sound)));

            WPlus plus = table1.add(new WPlus()).getWidget();
            plus.action = plus1 -> {
                if (!setting.get().contains(sound)) {
                    setting.get().add(sound);
                    reload();
                }
            };

            table1.row();
        };

        // Sort all sounds
        if (filter.text.isEmpty()) {
            Registry.SOUND_EVENT.forEach(soundForEach);
        } else {
            List<Pair<SoundEvent, Integer>> sounds = new ArrayList<>();
            Registry.SOUND_EVENT.forEach(sound -> {
                int words = Utils.search(getName(sound), filter.text);
                if (words > 0) sounds.add(new Pair<>(sound, words));
            });
            sounds.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<SoundEvent, Integer> pair : sounds) soundForEach.accept(pair.getLeft());
        }

        if (table1.getCells().size() > 0) add(new WVerticalSeparator()).expandY();

        // Selected sounds
        WTable table2 = add(new WTable()).top().getWidget();
        for (SoundEvent sound : setting.get()) {
            table2.add(new WLabel(getName(sound)));

            WMinus minus = table2.add(new WMinus()).getWidget();
            minus.action = minus1 -> {
                if (setting.get().remove(sound)) {
                    reload();
                }
            };

            table2.row();
        }
    }

    private String getName(SoundEvent sound) {
        return StringUtils.capitalize(sound.getId().getPath());
    }

    private void reload() {
        double verticalScroll = window.verticalScroll;

        setting.changed();
        clear();
        initWidgets();

        window.getRoot().layout();
        window.moveWidgets(0, verticalScroll);
    }
}
