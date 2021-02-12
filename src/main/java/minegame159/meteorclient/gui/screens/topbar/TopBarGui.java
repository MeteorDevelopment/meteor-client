/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
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
                .name("gUI-scale")
                .description("Scale of the GUI.")
                .defaultValue(1)
                .min(1)
                .max(3)
                .noSlider()
                .onChanged(aDouble -> {
                    GuiConfig.get().guiScale = aDouble;
                    if (MinecraftClient.getInstance().currentScreen instanceof WidgetScreen) {
                        ((WidgetScreen) MinecraftClient.getInstance().currentScreen).root.invalidate();
                    }
                })
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.get().guiScale))
                .build()
        );

        sg.add(new DoubleSetting.Builder()
                .name("scroll-sensitivity")
                .description("Sensitivity of scrolling in the GUI.")
                .defaultValue(1)
                .min(0.5)
                .max(4)
                .onChanged(aDouble -> GuiConfig.get().scrollSensitivity = aDouble)
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.get().scrollSensitivity))
                .build()
        );

        sg.add(new EnumSetting.Builder<AlignmentX>()
                .name("module-name-alignment")
                .description("Alignment of module name text in click gui.")
                .defaultValue(AlignmentX.Center)
                .onChanged(anEnum -> GuiConfig.get().moduleNameAlignment = anEnum)
                .onModuleActivated(alignmentXSetting -> alignmentXSetting.set(GuiConfig.get().moduleNameAlignment))
                .build()
        );

        sg.add(new DoubleSetting.Builder()
                .name("module-name-alignment-padding")
                .description("The padding of the module names in the Click GUI.")
                .defaultValue(7)
                .min(0)
                .max(20)
                .onChanged(aDouble -> GuiConfig.get().moduleNameAlignmentPadding = aDouble)
                .onModuleActivated(doubleSetting -> doubleSetting.set(GuiConfig.get().moduleNameAlignmentPadding))
                .build()
        );


        SettingGroup sgColors = s.createGroup("Colors");

        sgColors.add(new ColorSetting.Builder()
                .name("text")
                .description("Text color.")
                .defaultValue(new SettingColor(255, 255, 255))
                .onChanged(color -> GuiConfig.get().text.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().text))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("window-header-text")
                .description("Window header text color.")
                .defaultValue(new SettingColor(255, 255, 255))
                .onChanged(color -> GuiConfig.get().windowHeaderText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().windowHeaderText))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("logged-in-text")
                .description("Logged in text color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.get().loggedInText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().loggedInText))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("account-type-text")
                .description("Account type text color.")
                .defaultValue(new SettingColor(150, 150, 150))
                .onChanged(color -> GuiConfig.get().accountTypeText.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().accountTypeText))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("background")
                .description("Background color")
                .defaultValue(new SettingColor(20, 20, 20, 200))
                .onChanged(color -> GuiConfig.get().background.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().background))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("background-hovered")
                .description("Background hovered color.")
                .defaultValue(new SettingColor(30, 30, 30, 200))
                .onChanged(color -> GuiConfig.get().backgroundHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().backgroundHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("background-pressed")
                .description("Background pressed color.")
                .defaultValue(new SettingColor(40, 40, 40, 200))
                .onChanged(color -> GuiConfig.get().backgroundPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().backgroundPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar")
                .description("Scrollbar color")
                .defaultValue(new SettingColor(80, 80, 80, 200))
                .onChanged(color -> GuiConfig.get().scrollbar.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().scrollbar))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar-hovered")
                .description("Scrollbar hovered color")
                .defaultValue(new SettingColor(90, 90, 90, 200))
                .onChanged(color -> GuiConfig.get().scrollbarHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().scrollbarHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("scrollbar-pressed")
                .description("Scrollbar pressed color")
                .defaultValue(new SettingColor(100, 100, 100, 200))
                .onChanged(color -> GuiConfig.get().scrollbarPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().scrollbarPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("outline")
                .description("Outline color.")
                .defaultValue(new SettingColor(0, 0, 0, 225))
                .onChanged(color -> GuiConfig.get().outline.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().outline))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("outline-hovered")
                .description("Outline hovered color.")
                .defaultValue(new SettingColor(10, 10, 10, 225))
                .onChanged(color -> GuiConfig.get().outlineHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().outlineHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("outline-pressed")
                .description("Outline pressed color.")
                .defaultValue(new SettingColor(20, 20, 20, 225))
                .onChanged(color -> GuiConfig.get().outlinePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().outlinePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("checkbox")
                .description("Checkbox color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.get().checkbox.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().checkbox))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("checkbox-pressed")
                .description("Checkbox pressed color.")
                .defaultValue(new SettingColor(70, 225, 70))
                .onChanged(color -> GuiConfig.get().checkboxPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().checkboxPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("separator")
                .description("Separator color.")
                .defaultValue(new SettingColor(200, 200, 200, 225))
                .onChanged(color -> GuiConfig.get().separator.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().separator))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("plus")
                .description("Plus color.")
                .defaultValue(new SettingColor(45, 225, 45))
                .onChanged(color -> GuiConfig.get().plus.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().plus))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("plus-hovered")
                .description("Plus hovered color.")
                .defaultValue(new SettingColor(60, 225, 60))
                .onChanged(color -> GuiConfig.get().plusHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().plusHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("plus-pressed")
                .description("Plus pressed color.")
                .defaultValue(new SettingColor(75, 255, 75))
                .onChanged(color -> GuiConfig.get().plusPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().plusPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("minus")
                .description("Minus color.")
                .defaultValue(new SettingColor(225, 45, 45))
                .onChanged(color -> GuiConfig.get().minus.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().minus))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("minus-hovered")
                .description("Minus hovered color.")
                .defaultValue(new SettingColor(225, 60, 60))
                .onChanged(color -> GuiConfig.get().minusHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().minusHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("minus-pressed")
                .description("Minus pressed color.")
                .defaultValue(new SettingColor(225, 75, 75))
                .onChanged(color -> GuiConfig.get().minusPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().minusPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("accent")
                .description("Accent color.")
                .defaultValue(new SettingColor(135, 0, 255))
                .onChanged(color -> GuiConfig.get().accent.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().accent))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("module-background")
                .description("Module background color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.get().moduleBackground.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().moduleBackground))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("reset")
                .description("Reset color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.get().reset.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().reset))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("reset-hovered")
                .description("Reset hovered color.")
                .defaultValue(new SettingColor(60, 60, 60))
                .onChanged(color -> GuiConfig.get().resetHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().resetHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("reset-pressed")
                .description("Reset pressed color.")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.get().resetPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().resetPressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("slider-left")
                .description("Slider left color.")
                .defaultValue(new SettingColor(0, 150, 80))
                .onChanged(color -> GuiConfig.get().sliderLeft.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().sliderLeft))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-right")
                .description("Slider right color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.get().sliderRight.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().sliderRight))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle")
                .description("Slider handle color.")
                .defaultValue(new SettingColor(0, 255, 180))
                .onChanged(color -> GuiConfig.get().sliderHandle.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().sliderHandle))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle-hovered")
                .description("Slider handle hovered color.")
                .defaultValue(new SettingColor(0, 240, 165))
                .onChanged(color -> GuiConfig.get().sliderHandleHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().sliderHandleHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("slider-handle-pressed")
                .description("Slider handle pressed color.")
                .defaultValue(new SettingColor(0, 225, 150))
                .onChanged(color -> GuiConfig.get().sliderHandlePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().sliderHandlePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle")
                .description("SettingColor edit handle")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.get().colorEditHandle.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().colorEditHandle))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle-hovered")
                .description("SettingColor edit handle hovered color.")
                .defaultValue(new SettingColor(80, 80, 80))
                .onChanged(color -> GuiConfig.get().colorEditHandleHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().colorEditHandleHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("color-edit-handle-pressed")
                .description("SettingColor edit handle pressed.")
                .defaultValue(new SettingColor(90, 90, 90))
                .onChanged(color -> GuiConfig.get().colorEditHandlePressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().colorEditHandlePressed))
                .build()
        );

        sgColors.add(new ColorSetting.Builder()
                .name("edit")
                .description("Edit color.")
                .defaultValue(new SettingColor(50, 50, 50))
                .onChanged(color -> GuiConfig.get().edit.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().edit))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("edit-hovered")
                .description("Edit hovered color.")
                .defaultValue(new SettingColor(60, 60, 60))
                .onChanged(color -> GuiConfig.get().editHovered.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().editHovered))
                .build()
        );
        sgColors.add(new ColorSetting.Builder()
                .name("edit-pressed")
                .description("Edit pressed color.")
                .defaultValue(new SettingColor(70, 70, 70))
                .onChanged(color -> GuiConfig.get().editPressed.set(color))
                .onModuleActivated(colorSetting -> colorSetting.set(GuiConfig.get().editPressed))
                .build()
        );

        SettingGroup sgListSettingScreen = s.createGroup("List Setting Screen");

        sgListSettingScreen.add(new BoolSetting.Builder()
                .name("expand-list-setting-screen")
                .description("Automatically expand all lists from List Setting Screen when count of matches no more than a certain value.")  // TODO: grammar
                .defaultValue(true)
                .onChanged(bool -> GuiConfig.get().expandListSettingScreen = bool)
                .onModuleActivated(boolSetting -> boolSetting.set(GuiConfig.get().expandListSettingScreen))
                .build()
        );

        sgListSettingScreen.add(new BoolSetting.Builder()
                .name("collapse-list-setting-screen")
                .description("Automatically collapse all lists from List Setting Screen when count of matches more than a certain value.")  // TODO: grammar
                .defaultValue(true)
                .onChanged(bool -> GuiConfig.get().collapseListSettingScreen = bool)
                .onModuleActivated(setting -> setting.set(GuiConfig.get().collapseListSettingScreen))
                .build()
        );

        sgListSettingScreen.add(new IntSetting.Builder()
                .name("count-list-setting-screen")
                .description("The count of matches after which the list will be expanded/collapsed.")  // TODO: grammar
                .defaultValue(20)
                .onChanged(i -> GuiConfig.get().countListSettingScreen = i)
                .onModuleActivated(setting -> setting.set(GuiConfig.get().countListSettingScreen))
                .build()
        );


        add(s.createTable()).fillX().expandX();
    }
}
