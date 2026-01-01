/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.*;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorDropdown;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorSlider;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.*;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.util.MacWindowUtil;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ClassicGuiTheme extends MeteorTheme {

    // Colors

    {
        accentColor = color("accent", "Main color of the GUI.", new SettingColor(125, 80, 200));
        checkboxColor = color("checkbox", "Color of checkbox.", new SettingColor(125, 80, 200));
        plusColor = color("plus", "Color of plus button.", new SettingColor(80, 200, 80));
        minusColor = color("minus", "Color of minus button.", new SettingColor(200, 80, 80));
        favoriteColor = color("favorite", "Color of checked favorite button.", new SettingColor(220, 200, 80));
    }

    // Text

    {
        textColor = color(sgTextColors, "text", "Color of text.", new SettingColor(240, 240, 240));
        textSecondaryColor = color(sgTextColors, "text-secondary-text", "Color of secondary text.", new SettingColor(160, 160, 160));
        textHighlightColor = color(sgTextColors, "text-highlight", "Color of text highlighting.", new SettingColor(60, 130, 230, 80));
        titleTextColor = color(sgTextColors, "title-text", "Color of title text.", new SettingColor(240, 240, 240));
        loggedInColor = color(sgTextColors, "logged-in-text", "Color of logged in account name.", new SettingColor(80, 200, 80));
        placeholderColor = color(sgTextColors, "placeholder", "Color of placeholder text.", new SettingColor(240, 240, 240, 20));
    }

    // Background

    {
        backgroundColor = new ThreeStateColorSetting(
                sgBackgroundColors,
                "background",
                new SettingColor(25, 25, 25, 180),
                new SettingColor(35, 35, 35, 180),
                new SettingColor(45, 45, 45, 180)
        );

        moduleBackground = color(sgBackgroundColors, "module-background", "Color of module background when active.", new SettingColor(35, 35, 35));
    }

    // Outline

    {
        outlineColor = new ThreeStateColorSetting(
                sgOutline,
                "outline",
                new SettingColor(15, 15, 15),
                new SettingColor(25, 25, 25),
                new SettingColor(35, 35, 35)
        );
    }

    // Separator

    {
        separatorText = color(sgSeparator, "separator-text", "Color of separator text", new SettingColor(255, 255, 255));
        separatorCenter = color(sgSeparator, "separator-center", "Center color of separators.", new SettingColor(255, 255, 255));
        separatorEdges = color(sgSeparator, "separator-edges", "Color of separator edges.", new SettingColor(225, 225, 225, 150));
    }

    // Scrollbar

    {
        scrollbarColor = new ThreeStateColorSetting(
                sgScrollbar,
                "Scrollbar",
                new SettingColor(40, 40, 40, 160),
                new SettingColor(50, 50, 50, 160),
                new SettingColor(60, 60, 60, 160)
        );
    }

    // Slider

    {
        sliderHandle = new ThreeStateColorSetting(
                sgSlider,
                "slider-handle",
                new SettingColor(110, 60, 200),
                new SettingColor(120, 80, 210),
                new SettingColor(130, 100, 220)
        );

        sliderLeft = color(sgSlider, "slider-left", "Color of slider left part.", new SettingColor(80, 50, 140));
        sliderRight = color(sgSlider, "slider-right", "Color of slider right part.", new SettingColor(60, 60, 60));
    }

    // Starscript

    {
        starscriptText = color(sgStarscript, "starscript-text", "Color of text in Starscript code.", new SettingColor(169, 183, 198));
        starscriptBraces = color(sgStarscript, "starscript-braces", "Color of braces in Starscript code.", new SettingColor(150, 150, 150));
        starscriptParenthesis = color(sgStarscript, "starscript-parenthesis", "Color of parenthesis in Starscript code.", new SettingColor(169, 183, 198));
        starscriptDots = color(sgStarscript, "starscript-dots", "Color of dots in starscript code.", new SettingColor(169, 183, 198));
        starscriptCommas = color(sgStarscript, "starscript-commas", "Color of commas in starscript code.", new SettingColor(169, 183, 198));
        starscriptOperators = color(sgStarscript, "starscript-operators", "Color of operators in Starscript code.", new SettingColor(169, 183, 198));
        starscriptStrings = color(sgStarscript, "starscript-strings", "Color of strings in Starscript code.", new SettingColor(106, 135, 89));
        starscriptNumbers = color(sgStarscript, "starscript-numbers", "Color of numbers in Starscript code.", new SettingColor(104, 141, 187));
        starscriptKeywords = color(sgStarscript, "starscript-keywords", "Color of keywords in Starscript code.", new SettingColor(204, 120, 50));
        starscriptAccessedObjects = color(sgStarscript, "starscript-accessed-objects", "Color of accessed objects (before a dot) in Starscript code.", new SettingColor(152, 118, 170));
    }

    public ClassicGuiTheme() {
        super("Classic");
    }

}
