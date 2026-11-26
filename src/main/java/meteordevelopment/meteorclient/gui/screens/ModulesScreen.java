/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.builtin.HudTab;
import meteordevelopment.meteorclient.gui.tabs.builtin.ProfilesTab;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    private WTextBox searchTextBox;
    private WModernLayout layout;

    private final Category favoritesCategory = new Category("__meteor_favorites_internal__");
    private Category selectedCategory;
    private Module selectedModule;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        layout = add(new WModernLayout()).expandX().expandWidgetY().widget();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (locked) return false;

        boolean ctrl = SystemUtils.IS_OS_MAC
            ? Input.modsMatch(input.modifiers(), MOD_SUPER)
            : Input.modsMatch(input.modifiers(), MOD_CONTROL);

        if (ctrl && input.key() == KEY_F) {
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }
            return true;
        }

        if (input.key() == KEY_ESCAPE && layout != null && layout.search != null
            && !layout.search.get().isEmpty()) {
            layout.search.set("");
            layout.search.setFocused(true);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void tick() {
        super.tick();
        if (selectedModule != null && layout != null && layout.detailsContent != null) {
            selectedModule.settings.tick(layout.detailsContent, theme);
        }
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
    public void reload() {
        clear();
        initWidgets();
        invalidate();
    }

    private void selectModule(Module module) {
        if (selectedModule == module) return;

        selectedModule = module;
        if (layout != null) {
            layout.restartDrawerAnimation();
            layout.rebuildDetails();
        }
    }

    private Color accentColor() {
        return theme instanceof MeteorGuiTheme meteorTheme ? meteorTheme.accentColor.get() : theme.textColor();
    }

    private double panelCornerRadius() {
        return theme instanceof MeteorGuiTheme meteorTheme
            ? theme.scale(meteorTheme.cornerRadius.get())
            : theme.scale(12);
    }

    private double smallCornerRadius() {
        return theme instanceof MeteorGuiTheme meteorTheme
            ? theme.scale(meteorTheme.smallCornerRadius.get())
            : theme.scale(7);
    }

    private double glassOpacity() {
        return MeteorGuiTheme.GLASS_OPACITY;
    }

    private int glassAlpha(int alpha) {
        return Math.max(0, Math.min(255, (int) Math.round(alpha * glassOpacity())));
    }

    private double easeOutQuart(double value) {
        double t = Math.max(0, Math.min(1, value));
        return 1 - Math.pow(1 - t, 4);
    }

    private List<Module> getCategoryModules(Category category) {
        List<Module> result = new ArrayList<>();
        if (category == favoritesCategory) {
            for (Module module : Modules.get().getAll()) {
                if (module.favorite && !Config.get().hiddenModules.get().contains(module)) result.add(module);
            }
        } else {
            for (Module module : Modules.get().getGroup(category)) {
                if (!Config.get().hiddenModules.get().contains(module)) result.add(module);
            }
        }
        result.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title));
        return result;
    }

    private enum SearchMatchType {
        MODULE,
        SETTING
    }

    private static class SearchMatch {
        private final Module module;
        private final Setting<?> setting;
        private final SearchMatchType type;
        private final String title;
        private final String subtitle;
        private final int score;

        private SearchMatch(Module module, Setting<?> setting, SearchMatchType type,
                            String title, String subtitle, int score) {
            this.module = module;
            this.setting = setting;
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.score = score;
        }
    }

    private class WModernLayout extends WContainer {
        private final List<Category> categories = new ArrayList<>();

        private WHorizontalList searchBar;
        private WVerticalList sidebar;
        private WVerticalList center;
        private WView moduleView;
        private WVerticalList details;
        private WView detailsView;
        private WVerticalList detailsContent;
        private WTextBox search;

        private Cell<?> searchBarCell;
        private Cell<?> sidebarCell;
        private Cell<?> centerCell;
        private Cell<?> detailsCell;
        private double drawerProgress;

        @Override
        public void init() {
            for (Category category : Modules.loopCategories()) {
                if (!getCategoryModules(category).isEmpty()) categories.add(category);
            }

            selectedCategory = categories.isEmpty() ? favoritesCategory : categories.getFirst();
            selectedModule = getCategoryModules(selectedCategory).stream().findFirst().orElse(null);

            searchBar = theme.horizontalList();
            searchBar.spacing = 10;
            searchBarCell = add(searchBar).expandX().expandWidgetY();

            sidebar = theme.verticalList();
            sidebar.spacing = 5;
            sidebarCell = add(sidebar).expandX().expandWidgetY();

            center = theme.verticalList();
            center.spacing = 8;
            centerCell = add(center).expandX().expandWidgetY();

            details = theme.verticalList();
            details.spacing = 6;
            detailsCell = add(details).expandX().expandWidgetY();

            buildSearchBar();
            buildSidebar();
            buildCenter();
            rebuildModules();
            rebuildDetails();
        }

        private void buildSearchBar() {
            searchBar.clear();
            searchBar.add(theme.label("METEOR CLIENT", true)).centerY().padLeft(12);

            search = theme.textBox("", "Search modules, settings or aliases...");
            search.minWidth = 220;
            search.action = this::rebuildModules;
            searchTextBox = search;
            searchBar.add(search).expandX().padHorizontal(8);

            searchBar.add(theme.label("CTRL + F")).centerY();

            WPressable clearSearch = theme.button("×");
            clearSearch.action = () -> {
                search.set("");
                search.setFocused(true);
            };
            searchBar.add(clearSearch).padRight(10);
        }

        private void buildSidebar() {
            sidebar.clear();

            sidebar.add(theme.horizontalSeparator("FEATURES")).expandX();

            for (Category category : categories) {
                sidebar.add(new WCategoryButton(category)).expandX();
            }

            sidebar.add(new WCategoryButton(favoritesCategory)).expandX();

            sidebar.add(theme.horizontalSeparator("OTHER")).expandX();

            Tab profilesTab = Tabs.get(ProfilesTab.class);
            if (profilesTab != null) sidebar.add(new WTabButton("Profiles", profilesTab, new ItemStack(Items.BOOK))).expandX();

            Tab hudTab = Tabs.get(HudTab.class);
            if (hudTab != null) sidebar.add(new WTabButton("HUD", hudTab, new ItemStack(Items.ITEM_FRAME))).expandX();
        }

        private void buildCenter() {
            moduleView = theme.view();
            moduleView.hasScrollBar = true;
            moduleView.scrollOnlyWhenMouseOver = true;
            moduleView.maxHeight = Math.max(theme.scale(150),
                getWindowHeight() - bodyTop() - layoutPad() - theme.scale(42));
            moduleView.spacing = 10;
            center.add(moduleView).expandX();
        }

        private void rebuildModules() {
            if (moduleView == null) return;
            moduleView.clear();

            String query = search == null ? "" : search.get().trim();
            if (query.isEmpty()) {
                List<Module> visible = selectedCategory == null
                    ? List.of()
                    : getCategoryModules(selectedCategory);

                String categoryTitle = selectedCategory == null
                    ? "Modules"
                    : selectedCategory == favoritesCategory ? "Favorites" : selectedCategory.name;
                moduleView.add(theme.horizontalSeparator(categoryTitle)).expandX();
                addModuleGrid(visible);
                if (visible.isEmpty()) {
                    moduleView.add(theme.label("No modules in this category.", 320)).pad(12);
                    selectModule(null);
                } else if (!visible.contains(selectedModule)) {
                    selectModule(visible.getFirst());
                }
                invalidate();
                return;
            }

            List<SearchMatch> moduleMatches = new ArrayList<>();
            List<SearchMatch> settingMatches = new ArrayList<>();
            collectSearchMatches(query, moduleMatches, settingMatches);

            int limit = Math.max(1, Config.get().moduleSearchCount.get());
            moduleMatches.sort(Comparator.comparingInt((SearchMatch match) -> match.score)
                .thenComparing(match -> match.title, String.CASE_INSENSITIVE_ORDER));
            settingMatches.sort(Comparator.comparingInt((SearchMatch match) -> match.score)
                .thenComparing(match -> match.title, String.CASE_INSENSITIVE_ORDER));

            if (moduleMatches.size() > limit) moduleMatches = new ArrayList<>(moduleMatches.subList(0, limit));
            if (settingMatches.size() > limit) settingMatches = new ArrayList<>(settingMatches.subList(0, limit));

            if (!moduleMatches.isEmpty()) {
                moduleView.add(theme.horizontalSeparator("MODULES  ·  " + moduleMatches.size())).expandX();
                for (SearchMatch match : moduleMatches) {
                    moduleView.add(new WSearchResult(match)).expandX();
                }
            }

            if (!settingMatches.isEmpty()) {
                moduleView.add(theme.horizontalSeparator("SETTINGS  ·  " + settingMatches.size())).expandX();
                for (SearchMatch match : settingMatches) {
                    moduleView.add(new WSearchResult(match)).expandX();
                }
            }

            if (moduleMatches.isEmpty() && settingMatches.isEmpty()) {
                moduleView.add(theme.label("No modules or settings matched \"" + query + "\".", 320)).pad(12);
                moduleView.add(theme.label("Try a module name, alias or setting name.", 320)).padHorizontal(12);
                selectModule(null);
            } else {
                Module firstMatch = !moduleMatches.isEmpty()
                    ? moduleMatches.getFirst().module
                    : settingMatches.getFirst().module;
                if (firstMatch != selectedModule) selectModule(firstMatch);
            }

            invalidate();
        }

        private void addModuleGrid(List<Module> visible) {
            for (int i = 0; i < visible.size(); i += 2) {
                WHorizontalList row = theme.horizontalList();
                row.spacing = 10;

                Module first = visible.get(i);
                WWidget firstWidget = theme.module(first, first.title);
                row.add(new WModuleCard(first, firstWidget)).expandX();

                if (i + 1 < visible.size()) {
                    Module second = visible.get(i + 1);
                    WWidget secondWidget = theme.module(second, second.title);
                    row.add(new WModuleCard(second, secondWidget)).expandX();
                } else {
                    row.add(new WModuleGridSpacer()).expandX();
                }

                moduleView.add(row).expandX();
            }
        }

        private void collectSearchMatches(String query, List<SearchMatch> moduleMatches,
                                          List<SearchMatch> settingMatches) {
            for (Module module : Modules.get().getAll()) {
                if (Config.get().hiddenModules.get().contains(module)) continue;

                int bestModuleScore = Integer.MAX_VALUE;
                String displayTitle = module.title;
                String moduleSubtitle = module.category.name + "  ·  " + addonName(module);

                if (Utils.searchTextDefault(module.title, query, false)) {
                    bestModuleScore = Utils.searchLevenshteinDefault(module.title, query, false);
                }

                if (Config.get().moduleAliases.get()) {
                    for (String alias : module.aliases) {
                        if (!Utils.searchTextDefault(alias, query, false)) continue;

                        int score = Utils.searchLevenshteinDefault(alias, query, false);
                        if (score < bestModuleScore) {
                            bestModuleScore = score;
                            displayTitle = Utils.nameToTitle(alias);
                            moduleSubtitle = "Alias for " + module.title + "  ·  " + module.category.name;
                        }
                    }
                }

                if (bestModuleScore != Integer.MAX_VALUE) {
                    moduleMatches.add(new SearchMatch(
                        module, null, SearchMatchType.MODULE, displayTitle, moduleSubtitle, bestModuleScore
                    ));
                }

                for (SettingGroup group : module.settings) {
                    for (Setting<?> setting : group) {
                        if (setting.title == null || setting.title.isBlank()) continue;

                        int settingScore = Integer.MAX_VALUE;
                        if (Utils.searchTextDefault(setting.title, query, false)) {
                            settingScore = Utils.searchLevenshteinDefault(setting.title, query, false);
                        }
                        if (setting.description != null && !setting.description.isBlank()
                            && Utils.searchTextDefault(setting.description, query, false)) {
                            settingScore = Math.min(settingScore,
                                Utils.searchLevenshteinDefault(setting.description, query, false));
                        }

                        if (settingScore != Integer.MAX_VALUE) {
                            String subtitle = module.category.name + "  ·  " + module.title + "  ·  " + group.name;
                            settingMatches.add(new SearchMatch(
                                module, setting, SearchMatchType.SETTING, setting.title, subtitle, settingScore
                            ));
                        }
                    }
                }
            }
        }

        private String addonName(Module module) {
            return module.addon == null || module.addon == MeteorClient.ADDON
                ? "Meteor"
                : module.addon.name;
        }

        private class WSearchResult extends WPressable {
            private final SearchMatch match;
            private double hoverProgress;

            WSearchResult(SearchMatch match) {
                this.match = match;
            }

            @Override
            protected void onCalculateSize() {
                width = theme.scale(360);
                height = theme.scale(46);
            }

            @Override
            protected void onPressed(int button) {
                if (button == MOUSE_BUTTON_LEFT) {
                    if (match.type == SearchMatchType.MODULE) match.module.toggle();
                    selectModule(match.module);
                } else if (button == MOUSE_BUTTON_RIGHT) {
                    mc.gui.setScreen(theme.moduleScreen(match.module));
                }
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                hoverProgress = approach(hoverProgress, mouseOver ? 1 : 0, delta, 4.8);
                double hoverEase = easeOutQuart(hoverProgress);
                boolean selected = match.module == selectedModule;
                int backgroundAlpha = (int) (18 + hoverEase * 24 + (selected ? 12 : 0));

                if (backgroundAlpha > 0) {
                    drawRoundedRect(renderer, x, y, width, height, smallCornerRadius(),
                        new SettingColor(31, 27, 42, glassAlpha(38 + (int) (hoverEase * 18) + (selected ? 10 : 0))));
                }

                SettingColor typeColor = match.type == SearchMatchType.MODULE
                    ? new SettingColor(166, 227, 161)
                    : new SettingColor(137, 180, 250);
                String typeLabel = match.type == SearchMatchType.MODULE ? "MODULE" : "SETTING";
                double badgeWidth = theme.textWidth(typeLabel) + theme.scale(12);
                double badgeHeight = theme.scale(19);
                double badgeX = x + theme.scale(8);
                double badgeY = y + (height - badgeHeight) / 2;

                drawRoundedRect(renderer, badgeX, badgeY, badgeWidth, badgeHeight, theme.scale(5),
                    new SettingColor(typeColor.r, typeColor.g, typeColor.b, 34));
                renderer.text(typeLabel, badgeX + theme.scale(6),
                    badgeY + (badgeHeight - theme.textHeight()) / 2, typeColor, true);

                double textX = badgeX + badgeWidth + theme.scale(10);
                double textWidth = Math.max(theme.scale(24), width - (textX - x) - theme.scale(10));
                renderer.text(fitText(match.title, textWidth), textX, y + theme.scale(6),
                    selected ? accentColor() : theme.textColor(), false);
                renderer.text(fitText(match.subtitle, textWidth), textX, y + theme.scale(25),
                    theme.textSecondaryColor(), false);

                if (match.type == SearchMatchType.MODULE && match.module.isActive()) {
                    renderer.quad(x + width - theme.scale(8), y + height / 2 - theme.scale(3),
                        theme.scale(6), theme.scale(6), GuiRenderer.CIRCLE,
                        new SettingColor(166, 227, 161, 220));
                }
            }
        }

        private String fitText(String value, double maxWidth) {
            if (value == null || value.isBlank() || theme.textWidth(value) <= maxWidth) {
                return value == null ? "" : value;
            }

            String ellipsis = "...";
            int length = value.length();
            while (length > 0 && theme.textWidth(value.substring(0, length) + ellipsis) > maxWidth) length--;
            return length == 0 ? "" : value.substring(0, length) + ellipsis;
        }

        private void rebuildDetails() {
            if (details == null) return;
            details.clear();
            detailsView = null;
            detailsContent = null;
            details.add(theme.label("MODULE SETTINGS", true)).padTop(10).padLeft(10);

            if (selectedModule == null) {
                details.add(theme.label("Select a module to view its settings.", 220)).pad(10);
                return;
            }

            details.add(theme.label(selectedModule.title, true)).padTop(4).padLeft(10);
            double descriptionWidth = Math.max(theme.scale(120),
                Math.min(theme.scale(280), getWindowWidth() * 0.25 - theme.scale(28)));
            details.add(theme.label(selectedModule.description, descriptionWidth)).padHorizontal(10);

            if (selectedModule.addon != null && selectedModule.addon != MeteorClient.ADDON) {
                WHorizontalList addonRow = theme.horizontalList();
                addonRow.spacing = 4;
                addonRow.add(theme.label("From:").color(theme.textSecondaryColor()));
                addonRow.add(theme.label(selectedModule.addon.name).color(selectedModule.addon.color));
                details.add(addonRow).padHorizontal(10);
            }

            WHorizontalList activeRow = theme.horizontalList();
            activeRow.spacing = 8;
            activeRow.add(theme.label("Enabled"));
            activeRow.add(theme.label("")).expandX();
            activeRow.add(new WModuleToggle(selectedModule));
            details.add(activeRow).expandX().pad(10);

            WHorizontalList favoriteRow = theme.horizontalList();
            favoriteRow.spacing = 8;
            favoriteRow.add(theme.label("Favorite"));
            favoriteRow.add(theme.label("")).expandX();
            WFavorite favorite = theme.favorite(selectedModule.favorite);
            favorite.action = () -> {
                selectedModule.favorite = favorite.checked;
                if (favoritesCategory == selectedCategory) rebuildModules();
            };
            favoriteRow.add(favorite);
            details.add(favoriteRow).expandX().padHorizontal(10);

            details.add(theme.horizontalSeparator("BIND")).expandX();
            WHorizontalList bindRow = theme.horizontalList();
            bindRow.spacing = 8;
            bindRow.add(theme.label("Keybind"));
            WKeybind keybind = bindRow.add(theme.keybind(selectedModule.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(selectedModule);
            details.add(bindRow).expandX().padHorizontal(10);

            details.add(theme.horizontalSeparator("SETTINGS")).expandX();

            detailsView = theme.view();
            detailsView.maxHeight = Math.max(theme.scale(120),
                getWindowHeight() - bodyTop() - layoutPad() - theme.scale(190));
            detailsView.hasScrollBar = true;
            detailsView.scrollOnlyWhenMouseOver = true;
            detailsContent = theme.verticalList();
            detailsContent.add(theme.settings(selectedModule.settings)).expandX();
            detailsView.add(detailsContent).expandX();

            WWidget moduleWidget = selectedModule.getWidget(theme);
            if (moduleWidget != null) {
                detailsView.add(theme.horizontalSeparator("MODULE")).expandX();
                Cell<WWidget> moduleWidgetCell = detailsView.add(moduleWidget);
                if (moduleWidget instanceof WContainer) moduleWidgetCell.expandX();
            }

            details.add(detailsView).expandX();

            details.add(theme.button("Open full settings")).expandX().widget().action =
                () -> mc.gui.setScreen(theme.moduleScreen(selectedModule));
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = layoutPad();
            double bodyY = bodyTop();
            double bodyHeight = bodyHeight();

            searchBarCell.x = pad;
            searchBarCell.y = pad;
            searchBarCell.width = width - pad * 2;
            searchBarCell.height = theme.scale(40);
            searchBarCell.alignWidget();

            sidebarCell.x = pad;
            sidebarCell.y = bodyY;
            sidebarCell.width = sidebarWidth();
            sidebarCell.height = bodyHeight;
            sidebarCell.alignWidget();

            centerCell.x = pad + sidebarWidth() + layoutGap();
            centerCell.y = bodyY;
            centerCell.width = centerWidth();
            centerCell.height = bodyHeight;
            centerCell.alignWidget();

            detailsCell.x = detailsBaseX() + drawerOffset();
            detailsCell.y = bodyY;
            detailsCell.width = detailsWidth();
            detailsCell.height = bodyHeight;
            detailsCell.alignWidget();
        }

        @Override
        public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            drawerProgress = approach(drawerProgress, selectedModule == null ? 0 : 1, delta, 4.2);

            if (detailsCell != null && details != null) {
                double targetX = detailsBaseX() + drawerOffset();
                detailsCell.move(targetX - detailsCell.x, 0);
                details.mouseMoved(mouseX, mouseY, mouseX, mouseY);
            }

            return super.render(renderer, mouseX, mouseY, delta);
        }

        @Override
        protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (widget == searchBar) {
                drawRoundedRect(renderer, searchBarCell.x, searchBarCell.y, searchBarCell.width, searchBarCell.height,
                    smallCornerRadius(), new SettingColor(18, 15, 24, glassAlpha(34)));
                drawRoundedRect(renderer, searchBarCell.x, searchBarCell.y, searchBarCell.width, searchBarCell.height,
                    smallCornerRadius(), new SettingColor(88, 69, 111, glassAlpha(14)));
                super.renderWidget(widget, renderer, mouseX, mouseY, delta);
                return;
            }

            if (widget == details) {
                if (drawerProgress <= 0.001) return;

                double eased = drawerEase();
                double panelX = details.x;
                double panelY = bodyTop();
                double panelWidth = detailsWidth();
                double panelHeight = bodyHeight();

                drawRoundedRect(renderer, panelX, panelY, panelWidth, panelHeight, panelCornerRadius(),
                    new SettingColor(88, 68, 112, glassAlpha((int) (26 * eased))));
                drawRoundedRect(renderer, panelX + theme.scale(1), panelY + theme.scale(1),
                    panelWidth - theme.scale(2), panelHeight - theme.scale(2),
                    Math.max(0, panelCornerRadius() - theme.scale(1)),
                    new SettingColor(13, 11, 19, glassAlpha((int) (30 * eased))));

                super.renderWidget(widget, renderer, mouseX, mouseY, delta);
                return;
            }

            super.renderWidget(widget, renderer, mouseX, mouseY, delta);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            Color outer = new SettingColor(8, 7, 12, 0);
            Color panel = new SettingColor(13, 11, 19, glassAlpha(30));
            Color border = new SettingColor(84, 66, 108, glassAlpha(23));
            double pad = layoutPad();
            double bodyY = bodyTop();
            double bodyHeight = bodyHeight();
            double centerX = pad + sidebarWidth() + layoutGap();
            double mainWidth = sidebarWidth() + layoutGap() + centerWidth();

            renderer.quad(0, 0, width, this.height, outer);

            drawRoundedRect(renderer, pad, bodyY, mainWidth, bodyHeight, panelCornerRadius(), border);
            drawRoundedRect(renderer, pad + theme.scale(1), bodyY + theme.scale(1),
                mainWidth - theme.scale(2), bodyHeight - theme.scale(2),
                Math.max(0, panelCornerRadius() - theme.scale(1)), panel);

            renderer.quad(pad + sidebarWidth(), bodyY + theme.scale(12), theme.scale(1),
                bodyHeight - theme.scale(24), new SettingColor(76, 62, 97, glassAlpha(18)));
            renderer.quad(centerX + theme.scale(12), bodyY + theme.scale(10),
                centerWidth() - theme.scale(24), theme.scale(2), new SettingColor(
                    accentColor().r, accentColor().g, accentColor().b, glassAlpha(78)));
        }

        private double layoutPad() {
            return theme.scale(12);
        }

        private double layoutGap() {
            return theme.scale(10);
        }

        private double bodyTop() {
            return layoutPad() + theme.scale(40) + layoutGap();
        }

        private double bodyHeight() {
            return Math.max(theme.scale(120), height - bodyTop() - layoutPad());
        }

        private double sidebarWidth() {
            double availableWidth = width - layoutPad() * 2 - layoutGap() * 2;
            return Math.min(theme.scale(220), availableWidth * 0.19);
        }

        private double detailsWidth() {
            double availableWidth = width - layoutPad() * 2 - layoutGap() * 2;
            return Math.min(theme.scale(320), availableWidth * 0.25);
        }

        private double centerWidth() {
            double availableWidth = width - layoutPad() * 2 - layoutGap() * 2;
            return Math.max(theme.scale(160), availableWidth - sidebarWidth() - detailsWidth());
        }

        private double detailsBaseX() {
            return layoutPad() + sidebarWidth() + layoutGap() + centerWidth() + layoutGap();
        }

        private double drawerEase() {
            return easeOutQuart(drawerProgress);
        }

        private double drawerOffset() {
            return (width - detailsBaseX()) * (1 - drawerEase());
        }

        private void restartDrawerAnimation() {
            drawerProgress = 0;
        }

        private class WCategoryButton extends WPressable {
            private final Category category;
            private double hoverProgress;
            private double selectedProgress;

            WCategoryButton(Category category) {
                this.category = category;
                selectedProgress = category == selectedCategory ? 1 : 0;
            }

            @Override
            protected void onCalculateSize() {
                width = theme.scale(164);
                height = theme.scale(38);
            }

            @Override
            protected void onPressed(int button) {
                if (button != MOUSE_BUTTON_LEFT) return;
                selectedCategory = category;
                search.set("");
                rebuildModules();
                Module first = getCategoryModules(category).stream().findFirst().orElse(null);
                selectModule(first);
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                hoverProgress = approach(hoverProgress, mouseOver ? 1 : 0, delta, 4.8);
                selectedProgress = approach(selectedProgress, category == selectedCategory ? 1 : 0, delta, 4.0);
                double hoverEase = easeOutQuart(hoverProgress);
                double selectedEase = easeOutQuart(selectedProgress);

                int backgroundAlpha = (int) (selectedEase * 78 + hoverEase * 22);
                if (backgroundAlpha > 0) {
                    drawRoundedRect(renderer, x, y, width, height, smallCornerRadius(),
                        new SettingColor(34, 28, 45, glassAlpha((int) (55 * selectedEase + 24 * hoverEase))));
                }

                if (selectedEase > 0.01) {
                    renderer.quad(x + theme.scale(2), y + theme.scale(8), theme.scale(2),
                        height - theme.scale(16),
                        new SettingColor(accentColor().r, accentColor().g, accentColor().b, glassAlpha((int) (92 * selectedEase))));
                }

                ItemStack icon = categoryIcon(category);
                if (icon != null && !icon.isEmpty()) {
                    renderer.post(() -> renderer.item(icon, (int) (x + theme.scale(9)), (int) (y + theme.scale(5)), 1.5f, false));
                }

                renderer.text(category == favoritesCategory ? "Favorites" : category.name, x + theme.scale(38), y + theme.scale(12),
                    selectedEase > 0.2 ? accentColor() : theme.textColor(), false);
            }
        }

        private ItemStack categoryIcon(Category category) {
            if (category == null) return ItemStack.EMPTY;
            return category == favoritesCategory ? new ItemStack(Items.NETHER_STAR) : category.icon.get();
        }

        private class WModuleCard extends WContainer {
            private final Module module;
            private final WWidget content;
            private double selectionProgress;
            private boolean leftPressed;

            WModuleCard(Module module, WWidget content) {
                this.module = module;
                this.content = content;
            }

            @Override
            public void init() {
                // WContainer.add assigns the current theme to its child.
                // Defer adding the theme-created module widget until this wrapper
                // has itself been attached to its parent and received that theme.
                add(content).expandX();
            }

            @Override
            protected void onCalculateSize() {
                super.onCalculateSize();
                height = Math.max(height, theme.scale(54));
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
                boolean wasOver = mouseOver;
                boolean handled = super.mouseClicked(click, doubled);
                if (click.button() == MOUSE_BUTTON_LEFT && wasOver) leftPressed = true;
                return handled;
            }

            @Override
            public boolean mouseReleased(MouseButtonEvent click) {
                boolean handled = super.mouseReleased(click);
                if (click.button() == MOUSE_BUTTON_LEFT) {
                    if (leftPressed && mouseOver) selectModule(module);
                    leftPressed = false;
                }
                return handled;
            }

            @Override
            public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                boolean hidden = super.render(renderer, mouseX, mouseY, delta);
                if (hidden) return true;

                selectionProgress = approach(selectionProgress, module == selectedModule ? 1 : 0, delta, 4.0);
                double selectionEase = easeOutQuart(selectionProgress);
                if (selectionEase > 0.01) {
                    Color accent = accentColor();
                    SettingColor outline = new SettingColor(accent.r, accent.g, accent.b,
                        glassAlpha((int) (96 * selectionEase)));
                    double line = theme.scale(1.4);
                    double radius = smallCornerRadius();
                    double markerWidth = theme.scale(3) * selectionEase;

                    renderer.quad(x + radius, y, width - radius * 2, line, outline);
                    renderer.quad(x + radius, y + height - line, width - radius * 2, line, outline);
                    renderer.quad(x, y + radius, line, height - radius * 2, outline);
                    renderer.quad(x + width - line, y + radius, line, height - radius * 2, outline);
                    renderer.quad(x + theme.scale(1), y + radius, markerWidth, height - radius * 2, outline);
                }

                return false;
            }
        }

        private class WModuleGridSpacer extends WWidget {
            @Override
            protected void onCalculateSize() {
                width = theme.scale(240);
                height = theme.scale(54);
            }
        }

        private class WTabButton extends WPressable {
            private final String label;
            private final Tab tab;
            private final ItemStack icon;
            private double hoverProgress;

            WTabButton(String label, Tab tab, ItemStack icon) {
                this.label = label;
                this.tab = tab;
                this.icon = icon;
            }

            @Override
            protected void onCalculateSize() {
                width = theme.scale(164);
                height = theme.scale(38);
            }

            @Override
            protected void onPressed(int button) {
                if (button == MOUSE_BUTTON_LEFT) tab.openScreen(theme);
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                hoverProgress = approach(hoverProgress, mouseOver ? 1 : 0, delta, 4.8);
                int backgroundAlpha = glassAlpha((int) (38 * easeOutQuart(hoverProgress)));
                if (backgroundAlpha > 0) {
                    drawRoundedRect(renderer, x, y, width, height, smallCornerRadius(),
                        new SettingColor(36, 29, 47, backgroundAlpha));
                }

                if (icon != null && !icon.isEmpty()) {
                    renderer.post(() -> renderer.item(icon, (int) (x + theme.scale(9)),
                        (int) (y + theme.scale(5)), 1.5f, false));
                }

                renderer.text(label, x + theme.scale(38), y + theme.scale(12), theme.textColor(), false);
            }
        }

        private class WModuleToggle extends WPressable {
            private final Module module;
            private double activeProgress;

            WModuleToggle(Module module) {
                this.module = module;
                activeProgress = module.isActive() ? 1 : 0;
            }

            @Override
            protected void onCalculateSize() {
                width = theme.scale(38);
                height = theme.scale(22);
            }

            @Override
            protected void onPressed(int button) {
                if (button == MOUSE_BUTTON_LEFT) module.toggle();
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                activeProgress = approach(activeProgress, module.isActive() ? 1 : 0, delta, 4.2);
                double activeEase = easeOutQuart(activeProgress);

                double radius = height / 2;
                Color accent = accentColor();
                Color track = new SettingColor(
                    (int) (87 + (accent.r - 87) * activeEase),
                    (int) (77 + (accent.g - 77) * activeEase),
                    (int) (100 + (accent.b - 100) * activeEase),
                    (int) (80 + 90 * activeEase)
                );
                drawRoundedRect(renderer, x, y, width, height, radius, track);

                double knob = height - theme.scale(4);
                double knobStart = x + theme.scale(2);
                double knobEnd = x + width - knob - theme.scale(2);
                double knobX = knobStart + (knobEnd - knobStart) * activeEase;
                renderer.quad(knobX, y + theme.scale(2), knob, knob, GuiRenderer.CIRCLE,
                    new SettingColor(250, 247, 255, (int) (210 + 45 * activeEase)));
            }
        }


        private double approach(double value, double target, double delta, double speed) {
            return value + (target - value) * Math.min(1, Math.max(0, delta * speed));
        }

        private void drawRoundedRect(GuiRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
            double diameter = radius * 2;
            if (width <= diameter || height <= diameter) {
                renderer.quad(x, y, width, height, color);
                return;
            }

            renderer.quad(x + radius, y, width - diameter, height, color);
            renderer.quad(x, y + radius, width, height - diameter, color);
            renderer.quad(x, y, diameter, diameter, GuiRenderer.CIRCLE, color);
            renderer.quad(x + width - diameter, y, diameter, diameter, GuiRenderer.CIRCLE, color);
            renderer.quad(x, y + height - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
            renderer.quad(x + width - diameter, y + height - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
        }

    }
}
