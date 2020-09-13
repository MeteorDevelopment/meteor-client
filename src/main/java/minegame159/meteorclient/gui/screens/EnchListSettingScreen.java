package minegame159.meteorclient.gui.screens;

//Created by squidoodly 25/07/2020

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EnchListSettingScreen extends WindowScreen{
    private final Setting<List<Enchantment>> setting;
    private final WTextBox filter;

    public EnchListSettingScreen(Setting<List<Enchantment>> setting){
        super("Select Enchantments", true);

        this.setting = setting;

        filter = new WTextBox("", 0);
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets(){
        add(filter).fillX().expandX();
        row();

        //All enchantments
        WTable table1 = add(new WTable()).top().getWidget();
        Consumer<Enchantment> enchForEach = enchantment -> {
            if(setting.get().contains(enchantment)) return;

            table1.add(new WLabel(enchantment.getName(1).getString()));

            WPlus plus = table1.add(new WPlus()).getWidget();
            plus.action = plus1 -> {
                if(!(setting.get().contains(enchantment))){
                    setting.get().add(enchantment);
                    reload();
                }
            };

            table1.row();
        };

        //Sort all enchantments
        if (filter.text.isEmpty()) {
            Registry.ENCHANTMENT.forEach(enchForEach);
        } else {
            List<Pair<Enchantment, Integer>> enchs = new ArrayList<>();
            Registry.ENCHANTMENT.forEach(ench -> {
                int words = Utils.search(ench.getName(1).getString(), filter.text);
                if(words > 0) enchs.add(new Pair<>(ench, words));
            });
            enchs.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<Enchantment, Integer> pair : enchs) enchForEach.accept(pair.getLeft());
        }

        if (table1.getCells().size() > 0) add(new WVerticalSeparator()).expandY();

        //Selected enchantments
        WTable table2 = add(new WTable()).top().getWidget();
        for (Enchantment ench : setting.get()) {
            table2.add(new WLabel(ench.getName(1).getString()));

            WMinus minus = table2.add(new WMinus()).getWidget();
            minus.action = minus1 -> {
                if(setting.get().remove(ench)) {
                    reload();
                }
            };

            table2.row();
        }
    }

    private void reload(){
        double verticalScroll = window.verticalScroll;

        setting.changed();
        clear();
        initWidgets();

        window.getRoot().layout();
        window.moveWidgets(0, verticalScroll);
    }
}
