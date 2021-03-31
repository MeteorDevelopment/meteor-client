/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.Tabs;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.containers.*;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.gui.widgets.pressable.WMinus;
import minegame159.meteorclient.gui.widgets.pressable.WPlus;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.utils.files.ProfileUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;

import java.util.List;

import static minegame159.meteorclient.utils.Utils.getWindowHeight;
import static minegame159.meteorclient.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().get(0));

        add(createCategoryContainer());

        // Help
        WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
        help.add(theme.label("Left click - Toggle module"));
        help.add(theme.label("Right click - Open module settings"));
    }

    protected WCategoryController createCategoryContainer() {
        return new WCategoryController();
    }

    // Category

    protected void createCategory(WContainer c, Category category) {
        WWindow w = theme.window(category.name);
        w.id = category.name;
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(category.icon)).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        for (Module module : Modules.get().getGroup(category)) {
            w.add(theme.module(module)).expandX().widget().tooltip = module.description;
        }
    }

    // Profiles

    protected void createProfilesW(WWindow w) {
        // Profiles
        WTable t = w.add(theme.table()).expandX().widget();

        if (ProfileUtils.getProfiles().size() > 0) {
            for (String profile : ProfileUtils.getProfiles()) {
                t.add(theme.label(profile)).expandX().widget();

                WButton save = t.add(theme.button("Save")).widget();
                save.action = () -> ProfileUtils.save(profile);

                WButton load = t.add(theme.button("Load")).widget();
                load.action = () -> ProfileUtils.load(profile);

                WMinus delete = t.add(theme.minus()).widget();
                delete.action = () -> {
                    ProfileUtils.delete(profile);

                    w.clear();
                    createProfilesW(w);
                };

                t.row();
            }

            w.add(theme.horizontalSeparator()).expandX();
        }

        // New Profile
        WHorizontalList l = w.add(theme.horizontalList()).expandX().widget();

        WTextBox name = l.add(theme.textBox("")).minWidth(140).expandX().widget();
        WPlus add = l.add(theme.plus()).widget();

        add.action = () -> {
            if (ProfileUtils.save(name.get())) {
                w.clear();
                createProfilesW(w);
            }
        };
    }

    protected void createProfiles(WContainer c) {
        WWindow w = theme.window("Profiles");
        w.id = "profiles";

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.BOOKSHELF.getDefaultStack())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;

        createProfilesW(w);
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            List<Pair<Module, Integer>> modules = Modules.get().searchTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                for (Pair<Module, Integer> pair : modules) {
                    section.add(theme.module(pair.getLeft())).expandX();
                }
            }

            // Settings
            modules = Modules.get().searchSettingTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                for (Pair<Module, Integer> pair : modules) {
                    section.add(theme.module(pair.getLeft())).expandX();
                }
            }
        }
    }

    protected void createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.COMPASS.getDefaultStack())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.maxHeight -= 18;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).widget();
        text.setFocused(true);
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l);
        createSearchW(l, text.get());
    }

    // Stuff

    protected class WCategoryController extends WContainer {
        @Override
        public void init() {
            for (Category category : Modules.loopCategories()) {
                createCategory(this, category);
            }

            createProfiles(this);
            createSearch(this);
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(4);
            double h = theme.scale(40);

            double x = this.x + pad;
            double y = this.y;

            for (Cell<?> cell : cells) {
                double windowWidth = getWindowWidth();
                double windowHeight = getWindowHeight();

                if (x + cell.width > windowWidth) {
                    x = x + pad;
                    y += h;
                }

                if (x > windowWidth) {
                    x = windowWidth / 2.0 - cell.width / 2.0;
                    if (x < 0) x = 0;
                }
                if (y > windowHeight) {
                    y = windowHeight / 2.0 - cell.height / 2.0;
                    if (y < 0) y = 0;
                }

                cell.x = x;
                cell.y = y;

                cell.width = cell.widget().width;
                cell.height = cell.widget().height;

                cell.alignWidget();

                x += cell.width + pad;
            }
        }
    }
}
