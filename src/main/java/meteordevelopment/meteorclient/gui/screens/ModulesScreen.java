/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    private WCategoryController controller;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().get(0));
    }

    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).widget();

        // Help
        WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
        help.add(theme.label("Left click - Toggle module"));
        help.add(theme.label("Right click - Open module settings"));
    }

    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }

    // Category

    protected WWindow createCategory(WContainer c, Category category) {
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
            w.add(theme.module(module)).expandX();
        }

        return w;
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            Set<Module> modules = Modules.get().searchTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                for (Module module : modules) {
                    section.add(theme.module(module)).expandX();
                }
            }

            // Settings
            modules = Modules.get().searchSettingTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                for (Module module : modules) {
                    section.add(theme.module(module)).expandX();
                }
            }
        }
    }

    protected WWindow createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.COMPASS.getDefaultStack())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.maxHeight -= 20;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l).expandX();
        createSearchW(l, text.get());

        return w;
    }

    // Favorites

    protected Cell<WWindow> createFavorites(WContainer c) {
        boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
        if (!hasFavorites) return null;

        WWindow w = theme.window("Favorites");
        w.id = "favorites";
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.NETHER_STAR.getDefaultStack())).pad(2);
        }

        Cell<WWindow> cell = c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        createFavoritesW(w);
        return cell;
    }

    protected boolean createFavoritesW(WWindow w) {
        boolean hasFavorites = false;

        for (Module module : Modules.get().getAll()) {
            if (module.favorite) {
                w.add(theme.module(module)).expandX();
                hasFavorites = true;
            }
        }

        return hasFavorites;
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {}

    // Stuff

    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;

        @Override
        public void init() {
            for (Category category : Modules.loopCategories()) {
                windows.add(createCategory(this, category));
            }

            windows.add(createSearch(this));

            refresh();
        }

        protected void refresh() {
            if (favorites == null) {
                favorites = createFavorites(this);
                if (favorites != null) windows.add(favorites.widget());
            }
            else {
                favorites.widget().clear();

                if (!createFavoritesW(favorites.widget())) {
                    remove(favorites);
                    windows.remove(favorites.widget());
                    favorites = null;
                }
            }
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
