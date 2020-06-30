package minegame159.meteorclient.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import minegame159.meteorclient.gui.widgets.WIntTextBox;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.effect.StatusEffect;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatusEffectSettingScreen extends WindowScreen {
    private final Setting<Object2IntMap<StatusEffect>> setting;
    private final WTextBox filter;

    public StatusEffectSettingScreen(Setting<Object2IntMap<StatusEffect>> setting) {
        super("Select Potions", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
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

        List<StatusEffect> statusEffects = new ArrayList<>(setting.get().keySet());
        statusEffects.sort(Comparator.comparing(statusEffect -> statusEffect.getName().asString()));

        WTable table = add(new WTable()).expandX().fillX().getWidget();

        for (StatusEffect statusEffect : statusEffects) {
            String name = statusEffect.getName().asString();
            if (!StringUtils.containsIgnoreCase(name, filter.text)) continue;

            table.add(new WLabel(name));
            table.add(new WIntTextBox(setting.get().getInt(statusEffect), 50)).fillX().right().getWidget().action = textBox -> {
                setting.get().put(statusEffect, textBox.value);
                setting.changed();
            };

            table.row();
        }
    }
}
