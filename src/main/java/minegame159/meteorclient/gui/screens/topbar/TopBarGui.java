/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;

public class TopBarGui extends TopBarWindowScreen {
    public TopBarGui() {
        super(TopBarType.Gui);
    }

    @Override
    protected void initWidgets() {
        Settings s = new Settings();
        SettingGroup sg = s.getDefaultGroup();

        sg.add(new DoubleSetting.Builder()
                .name("gui-scale")
                .description("Scale of the GUI.")
                .defaultValue(1)
                .min(1)
                .max(3)
                .noSlider()
                .onChanged(aDouble -> {
                    GuiConfig.INSTANCE.guiScale = aDouble;
                    if (MinecraftClient.getInstance().currentScreen instanceof WidgetScreen) {
                        ((WidgetScreen) MinecraftClient.getInstance().currentScreen).root.invalidate();
                    }
                })
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.INSTANCE.guiScale))
                .build()
        );

        sg.add(new DoubleSetting.Builder()
                .name("scroll-sensitivity")
                .description("Sensitivity of scrolling in the GUI.")
                .defaultValue(1)
                .min(0.5)
                .max(4)
                .onChanged(aDouble -> GuiConfig.INSTANCE.scrollSensitivity = aDouble)
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.INSTANCE.scrollSensitivity))
                .build()
        );

        sg.add(new EnumSetting.Builder<AlignmentX>()
                .name("module-name-alignment")
                .description("Alignment of module name text in click gui.")
                .defaultValue(AlignmentX.Center)
                .onChanged(anEnum -> GuiConfig.INSTANCE.moduleNameAlignment = anEnum)
                .onModuleActivated(alignmentXSetting -> alignmentXSetting.set(GuiConfig.INSTANCE.moduleNameAlignment))
                .build()
        );

        sg.add(new DoubleSetting.Builder()
                .name("module-name-alignment-padding")
                .description("The padding of the module names in the Click GUI.")
                .defaultValue(7)
                .min(0)
                .max(20)
                .onChanged(aDouble -> GuiConfig.INSTANCE.moduleNameAlignmentPadding = aDouble)
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.INSTANCE.moduleNameAlignmentPadding))
                .build()
        );


        SettingGroup sgColors = s.createGroup("Colors");

        sgColors.add(new ColorSetting.Builder()
                .name("text")
                .description("Text color.")
                .defaultValue(new SettingColor(255, 255, 255))
                .onChanged(color -> GuiConfig.INSTANCE.text.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.text))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("window-header-text")
                .description("Window header text color.")
                .defaultValue(new SettingColor(255, 255, 255))
                .onChanged(color -> GuiConfig.INSTANCE.windowHeaderText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.windowHeaderText))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("logged-in-text")
                .description("Logged in text color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.INSTANCE.loggedInText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.loggedInText))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("account-type-text")
                .description("Account type text color.")
                .defaultValue(new SettingColor(150, 150, 150))
                .onChanged(color -> GuiConfig.INSTANCE.accountTypeText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.accountTypeText))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("background")
                .description("Background color")
                .defaultValue(new SettingColor(20, 20, 20, 200))
                .onChanged(color -> GuiConfig.INSTANCE.background.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.background))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("background-hovered")
                .description("Background hovered color.")
                .defaultValue(new SettingColor(30, 30, 30, 200))
                .onChanged(color -> GuiConfig.INSTANCE.backgroundHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.backgroundHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("background-pressed")
                .description("Background pressed color.")
                .defaultValue(new SettingColor(40, 40, 40, 200))
                .onChanged(color -> GuiConfig.INSTANCE.backgroundPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.backgroundPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar")
                .description("Scrollbar color")
                .defaultValue(new SettingColor(80, 80, 80, 200))
                .onChanged(color -> GuiConfig.INSTANCE.scrollbar.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.scrollbar))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar-hovered")
                .description("Scrollbar hovered color")
                .defaultValue(new SettingColor(90, 90, 90, 200))
                .onChanged(color -> GuiConfig.INSTANCE.scrollbarHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.scrollbarHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar-pressed")
                .description("Scrollbar pressed color")
                .defaultValue(new SettingColor(100, 100, 100, 200))
                .onChanged(color -> GuiConfig.INSTANCE.scrollbarPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.scrollbarPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("outline")
                .description("Outline color.")
                .defaultValue(new SettingColor(0, 0, 0, 225))
                .onChanged(color -> GuiConfig.INSTANCE.outline.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.outline))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("outline-hovered")
                .description("Outline hovered color.")
                .defaultValue(new SettingColor(10, 10, 10, 225))
                .onChanged(color -> GuiConfig.INSTANCE.outlineHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.outlineHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("outline-pressed")
                .description("Outline pressed color.")
                .defaultValue(new SettingColor(20, 20, 20, 225))
                .onChanged(color -> GuiConfig.INSTANCE.outlinePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.outlinePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("checkbox")
                .description("Checkbox color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.INSTANCE.checkbox.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.checkbox))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("checkbox-pressed")
                .description("Checkbox pressed color.")
                .defaultValue(new SettingColor(70, 225, 70))
                .onChanged(color -> GuiConfig.INSTANCE.checkboxPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.checkboxPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("separator")
                .description("Separator color.")
                .defaultValue(new SettingColor(200, 200, 200, 225))
                .onChanged(color -> GuiConfig.INSTANCE.separator.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.separator))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("plus")
                .description("Plus color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.INSTANCE.plus.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.plus))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("plus-hovered")
                .description("Plus hovered color.")
                .defaultValue(new SettingColor(60, 225, 60))
                .onChanged(color -> GuiConfig.INSTANCE.plusHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.plusHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("plus-pressed")
                .description("Plus pressed color.")
                .defaultValue(new SettingColor(75, 255, 75))
                .onChanged(color -> GuiConfig.INSTANCE.plusPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.plusPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("minus")
                .description("Minus color.")
                .defaultValue(new SettingColor(225, 45, 45))
                .onChanged(color -> GuiConfig.INSTANCE.minus.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.minus))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("minus-hovered")
                .description("Minus hovered color.")
                .defaultValue(new SettingColor(225, 60, 60))
                .onChanged(color -> GuiConfig.INSTANCE.minusHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.minusHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("minus-pressed")
                .description("Minus pressed color.")
                .defaultValue(new SettingColor(225, 75, 75))
                .onChanged(color -> GuiConfig.INSTANCE.minusPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.minusPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("accent")
                .description("Accent color.")
                .defaultValue(new SettingColor(135, 0, 255))
                .onChanged(color -> GuiConfig.INSTANCE.accent.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.accent))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("module-background")
                .description("Module background color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.INSTANCE.moduleBackground.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.moduleBackground))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("reset")
                .description("Reset color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.INSTANCE.reset.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.reset))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("reset-hovered")
                .description("Reset hovered color.")
                .defaultValue(new SettingColor(60, 60, 60))
                .onChanged(color -> GuiConfig.INSTANCE.resetHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.resetHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("reset-pressed")
                .description("Reset pressed color.")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.INSTANCE.resetPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.resetPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("slider-left")
                .description("Slider left color.")
                .defaultValue(new SettingColor(0, 150, 80))
                .onChanged(color -> GuiConfig.INSTANCE.sliderLeft.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.sliderLeft))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-right")
                .description("Slider right color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.INSTANCE.sliderRight.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.sliderRight))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle")
                .description("Slider handle color.")
                .defaultValue(new SettingColor(0, 255, 180))
                .onChanged(color -> GuiConfig.INSTANCE.sliderHandle.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.sliderHandle))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle-hovered")
                .description("Slider handle hovered color.")
                .defaultValue(new SettingColor(0, 240, 165))
                .onChanged(color -> GuiConfig.INSTANCE.sliderHandleHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.sliderHandleHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle-pressed")
                .description("Slider handle pressed color.")
                .defaultValue(new SettingColor(0, 225, 150))
                .onChanged(color -> GuiConfig.INSTANCE.sliderHandlePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.sliderHandlePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle")
                .description("SettingColor edit handle")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.INSTANCE.colorEditHandle.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.colorEditHandle))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle-hovered")
                .description("SettingColor edit handle hovered color.")
                .defaultValue(new SettingColor(80, 80, 80))
                .onChanged(color -> GuiConfig.INSTANCE.colorEditHandleHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.colorEditHandleHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle-pressed")
                .description("SettingColor edit handle pressed.")
                .defaultValue(new SettingColor(90, 90, 90))
                .onChanged(color -> GuiConfig.INSTANCE.colorEditHandlePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.colorEditHandlePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("edit")
                .description("Edit color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.INSTANCE.edit.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.edit))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("edit-hovered")
                .description("Edit hovered color.")
                .defaultValue(new SettingColor(60, 60, 60))
                .onChanged(color -> GuiConfig.INSTANCE.editHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.editHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("edit-pressed")
                .description("Edit pressed color.")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.INSTANCE.editPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.INSTANCE.editPressed))
                .build()
        );

        SettingGroup sgListSettingScreen = s.createGroup("List Setting Screen");

        sgListSettingScreen.add(new BoolSetting.Builder()
                .name("expand-list-setting-screen")
                .description("Automatically expand all lists from List Setting Screen when count of matches no more than a certain value.")  // TODO: grammar
                .defaultValue(true)
                .onChanged(bool -> GuiConfig.INSTANCE.expandListSettingScreen = bool)
                .onModuleActivated(boolSetting -> boolSetting.set(GuiConfig.INSTANCE.expandListSettingScreen))
                .build()
        );

        sgListSettingScreen.add(new BoolSetting.Builder()
                .name("collapse-list-setting-screen")
                .description("Automatically collapse all lists from List Setting Screen when count of matches more than a certain value.")  // TODO: grammar
                .defaultValue(true)
                .onChanged(bool -> GuiConfig.INSTANCE.collapseListSettingScreen = bool)
                .onModuleActivated(setting -> setting.set(GuiConfig.INSTANCE.collapseListSettingScreen))
                .build()
        );

        sgListSettingScreen.add(new IntSetting.Builder()
                .name("count-list-setting-screen")
                .description("The count of matches after which the list will be expanded/collapsed.")  // TODO: grammar
                .defaultValue(20)
                .onChanged(i -> GuiConfig.INSTANCE.countListSettingScreen = i)
                .onModuleActivated(setting -> setting.set(GuiConfig.INSTANCE.countListSettingScreen))
                .build()
        );


        add(s.createTable()).fillX().expandX();
    }
}
