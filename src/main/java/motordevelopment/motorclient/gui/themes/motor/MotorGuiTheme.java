/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor;

import motordevelopment.motorclient.gui.DefaultSettingsWidgetFactory;
import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WidgetScreen;
import motordevelopment.motorclient.gui.renderer.packer.GuiTexture;
import motordevelopment.motorclient.gui.themes.motor.widgets.*;
import motordevelopment.motorclient.gui.themes.motor.widgets.input.WMotorDropdown;
import motordevelopment.motorclient.gui.themes.motor.widgets.input.WMotorSlider;
import motordevelopment.motorclient.gui.themes.motor.widgets.input.WMotorTextBox;
import motordevelopment.motorclient.gui.themes.motor.widgets.pressable.*;
import motordevelopment.motorclient.gui.utils.AlignmentX;
import motordevelopment.motorclient.gui.utils.CharFilter;
import motordevelopment.motorclient.gui.widgets.*;
import motordevelopment.motorclient.gui.widgets.containers.WSection;
import motordevelopment.motorclient.gui.widgets.containers.WView;
import motordevelopment.motorclient.gui.widgets.containers.WWindow;
import motordevelopment.motorclient.gui.widgets.input.WDropdown;
import motordevelopment.motorclient.gui.widgets.input.WSlider;
import motordevelopment.motorclient.gui.widgets.input.WTextBox;
import motordevelopment.motorclient.gui.widgets.pressable.*;
import motordevelopment.motorclient.renderer.text.TextRenderer;
import motordevelopment.motorclient.settings.*;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.render.color.Color;
import motordevelopment.motorclient.utils.render.color.SettingColor;

import static motordevelopment.motorclient.MotorClient.mc;
import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;

public class MotorGuiTheme extends GuiTheme {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgTextColors = settings.createGroup("Text");
    private final SettingGroup sgBackgroundColors = settings.createGroup("Background");
    private final SettingGroup sgOutline = settings.createGroup("Outline");
    private final SettingGroup sgSeparator = settings.createGroup("Separator");
    private final SettingGroup sgScrollbar = settings.createGroup("Scrollbar");
    private final SettingGroup sgSlider = settings.createGroup("Slider");
    private final SettingGroup sgStarscript = settings.createGroup("Starscript");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the GUI.")
        .defaultValue(1)
        .min(0.75)
        .sliderRange(0.75, 4)
        .onSliderRelease()
        .onChanged(aDouble -> {
            if (mc.currentScreen instanceof WidgetScreen) ((WidgetScreen) mc.currentScreen).invalidate();
        })
        .build()
    );

    public final Setting<AlignmentX> moduleAlignment = sgGeneral.add(new EnumSetting.Builder<AlignmentX>()
        .name("module-alignment")
        .description("How module titles are aligned.")
        .defaultValue(AlignmentX.Center)
        .build()
    );

    public final Setting<Boolean> categoryIcons = sgGeneral.add(new BoolSetting.Builder()
        .name("category-icons")
        .description("Adds item icons to module categories.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> hideHUD = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-HUD")
        .description("Hide HUD when in GUI.")
        .defaultValue(false)
        .onChanged(v -> {
            if (mc.currentScreen instanceof WidgetScreen) mc.options.hudHidden = v;
        })
        .build()
    );

    // Colors

    public final Setting<SettingColor> accentColor = color("accent", "Main color of the GUI.", new SettingColor(145, 61, 226));
    public final Setting<SettingColor> checkboxColor = color("checkbox", "Color of checkbox.", new SettingColor(145, 61, 226));
    public final Setting<SettingColor> plusColor = color("plus", "Color of plus button.", new SettingColor(50, 255, 50));
    public final Setting<SettingColor> minusColor = color("minus", "Color of minus button.", new SettingColor(255, 50, 50));
    public final Setting<SettingColor> favoriteColor = color("favorite", "Color of checked favorite button.", new SettingColor(250, 215, 0));

    // Text

    public final Setting<SettingColor> textColor = color(sgTextColors, "text", "Color of text.", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> textSecondaryColor = color(sgTextColors, "text-secondary-text", "Color of secondary text.", new SettingColor(150, 150, 150));
    public final Setting<SettingColor> textHighlightColor = color(sgTextColors, "text-highlight", "Color of text highlighting.", new SettingColor(45, 125, 245, 100));
    public final Setting<SettingColor> titleTextColor = color(sgTextColors, "title-text", "Color of title text.", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> loggedInColor = color(sgTextColors, "logged-in-text", "Color of logged in account name.", new SettingColor(45, 225, 45));
    public final Setting<SettingColor> placeholderColor = color(sgTextColors, "placeholder", "Color of placeholder text.", new SettingColor(255, 255, 255, 20));

    // Background

    public final ThreeStateColorSetting backgroundColor = new ThreeStateColorSetting(
            sgBackgroundColors,
            "background",
            new SettingColor(20, 20, 20, 200),
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200)
    );

    public final Setting<SettingColor> moduleBackground = color(sgBackgroundColors, "module-background", "Color of module background when active.", new SettingColor(50, 50, 50));

    // Outline

    public final ThreeStateColorSetting outlineColor = new ThreeStateColorSetting(
            sgOutline,
            "outline",
            new SettingColor(0, 0, 0),
            new SettingColor(10, 10, 10),
            new SettingColor(20, 20, 20)
    );

    // Separator

    public final Setting<SettingColor> separatorText = color(sgSeparator, "separator-text", "Color of separator text", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> separatorCenter = color(sgSeparator, "separator-center", "Center color of separators.", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> separatorEdges = color(sgSeparator, "separator-edges", "Color of separator edges.", new SettingColor(225, 225, 225, 150));

    // Scrollbar

    public final ThreeStateColorSetting scrollbarColor = new ThreeStateColorSetting(
            sgScrollbar,
            "Scrollbar",
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200),
            new SettingColor(50, 50, 50, 200)
    );

    // Slider

    public final ThreeStateColorSetting sliderHandle = new ThreeStateColorSetting(
            sgSlider,
            "slider-handle",
            new SettingColor(130, 0, 255),
            new SettingColor(140, 30, 255),
            new SettingColor(150, 60, 255)
    );

    public final Setting<SettingColor> sliderLeft = color(sgSlider, "slider-left", "Color of slider left part.", new SettingColor(100,35,170));
    public final Setting<SettingColor> sliderRight = color(sgSlider, "slider-right", "Color of slider right part.", new SettingColor(50, 50, 50));

    // Starscript

    private final Setting<SettingColor> starscriptText = color(sgStarscript, "starscript-text", "Color of text in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptBraces = color(sgStarscript, "starscript-braces", "Color of braces in Starscript code.", new SettingColor(150, 150, 150));
    private final Setting<SettingColor> starscriptParenthesis = color(sgStarscript, "starscript-parenthesis", "Color of parenthesis in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptDots = color(sgStarscript, "starscript-dots", "Color of dots in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptCommas = color(sgStarscript, "starscript-commas", "Color of commas in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptOperators = color(sgStarscript, "starscript-operators", "Color of operators in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptStrings = color(sgStarscript, "starscript-strings", "Color of strings in Starscript code.", new SettingColor(106, 135, 89));
    private final Setting<SettingColor> starscriptNumbers = color(sgStarscript, "starscript-numbers", "Color of numbers in Starscript code.", new SettingColor(104, 141, 187));
    private final Setting<SettingColor> starscriptKeywords = color(sgStarscript, "starscript-keywords", "Color of keywords in Starscript code.", new SettingColor(204, 120, 50));
    private final Setting<SettingColor> starscriptAccessedObjects = color(sgStarscript, "starscript-accessed-objects", "Color of accessed objects (before a dot) in Starscript code.", new SettingColor(152, 118, 170));

    public MotorGuiTheme() {
        super("motor");

        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor color) {
        return group.add(new ColorSetting.Builder()
                .name(name + "-color")
                .description(description)
                .defaultValue(color)
                .build());
    }
    private Setting<SettingColor> color(String name, String description, SettingColor color) {
        return color(sgColors, name, description, color);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WMotorWindow(icon, title));
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        if (maxWidth == 0) return w(new WMotorLabel(text, title));
        return w(new WMotorMultiLabel(text, title, maxWidth));
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return w(new WMotorHorizontalSeparator(text));
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return w(new WMotorVerticalSeparator());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WMotorButton(text, texture));
    }

    @Override
    public WMinus minus() {
        return w(new WMotorMinus());
    }

    @Override
    public WPlus plus() {
        return w(new WMotorPlus());
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WMotorCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WMotorSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WMotorTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WMotorDropdown<>(values, value));
    }

    @Override
    public WTriangle triangle() {
        return w(new WMotorTriangle());
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WMotorTooltip(text));
    }

    @Override
    public WView view() {
        return w(new WMotorView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WMotorSection(title, expanded, headerWidget));
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return w(new WMotorAccount(screen, account));
    }

    @Override
    public WWidget module(Module module) {
        return w(new WMotorModule(module));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WMotorQuad(color));
    }

    @Override
    public WTopBar topBar() {
        return w(new WMotorTopBar());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WMotorFavorite(checked));
    }

    // Colors

    @Override
    public Color textColor() {
        return textColor.get();
    }

    @Override
    public Color textSecondaryColor() {
        return textSecondaryColor.get();
    }

    //     Starscript

    @Override
    public Color starscriptTextColor() {
        return starscriptText.get();
    }

    @Override
    public Color starscriptBraceColor() {
        return starscriptBraces.get();
    }

    @Override
    public Color starscriptParenthesisColor() {
        return starscriptParenthesis.get();
    }

    @Override
    public Color starscriptDotColor() {
        return starscriptDots.get();
    }

    @Override
    public Color starscriptCommaColor() {
        return starscriptCommas.get();
    }

    @Override
    public Color starscriptOperatorColor() {
        return starscriptOperators.get();
    }

    @Override
    public Color starscriptStringColor() {
        return starscriptStrings.get();
    }

    @Override
    public Color starscriptNumberColor() {
        return starscriptNumbers.get();
    }

    @Override
    public Color starscriptKeywordColor() {
        return starscriptKeywords.get();
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return starscriptAccessedObjects.get();
    }

    // Other

    @Override
    public TextRenderer textRenderer() {
        return TextRenderer.get();
    }

    @Override
    public double scale(double value) {
        double scaled = value * scale.get();

        if (IS_SYSTEM_MAC) {
            scaled /= (double) mc.getWindow().getWidth() / mc.getWindow().getFramebufferWidth();
        }

        return scaled;
    }

    @Override
    public boolean categoryIcons() {
        return categoryIcons.get();
    }

    @Override
    public boolean hideHUD() {
        return hideHUD.get();
    }

    public class ThreeStateColorSetting {
        private final Setting<SettingColor> normal, hovered, pressed;

        public ThreeStateColorSetting(SettingGroup group, String name, SettingColor c1, SettingColor c2, SettingColor c3) {
            normal = color(group, name, "Color of " + name + ".", c1);
            hovered = color(group, "hovered-" + name, "Color of " + name + " when hovered.", c2);
            pressed = color(group, "pressed-" + name, "Color of " + name + " when pressed.", c3);
        }

        public SettingColor get() {
            return normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered, boolean bypassDisableHoverColor) {
            if (pressed) return this.pressed.get();
            return (hovered && (bypassDisableHoverColor || !disableHoverColor)) ? this.hovered.get() : this.normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered) {
            return get(pressed, hovered, false);
        }
    }
}
